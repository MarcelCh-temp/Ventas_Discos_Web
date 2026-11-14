package ms_ordenes.ms_ordenes.service;

import ms_ordenes.ms_ordenes.client.CatalogClient;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.Cotizacion;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.Disponibilidad;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.ItemSolicitado;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.LineaCotizada;
import ms_ordenes.ms_ordenes.dto.AgregarItemRequest;
import ms_ordenes.ms_ordenes.dto.CarritoResponse;
import ms_ordenes.ms_ordenes.dto.ItemCarritoResponse;
import ms_ordenes.ms_ordenes.exception.OperacionNoPermitidaException;
import ms_ordenes.ms_ordenes.exception.RecursoNoEncontradoException;
import ms_ordenes.ms_ordenes.exception.ServicioNoDisponibleException;
import ms_ordenes.ms_ordenes.model.Carrito;
import ms_ordenes.ms_ordenes.model.ItemCarrito;
import ms_ordenes.ms_ordenes.repository.CarritoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lote 3 · HU-18 a HU-21 (EP-04). El carrito es del usuario del TOKEN. Antes de agregar o subir una cantidad se
 * consulta a Catalog si el producto está activo y si alcanza el stock (RF-COM-01/03); si Catalog no responde
 * no se acepta el cambio (503). Ver el carrito o quitar un producto funciona aunque Catalog no responda.
 */
@Service
public class CarritoService {

    // Constante MSG_NO_ENCONTRADO: valor compartido que no cambia durante la ejecución.
    static final String MSG_NO_ENCONTRADO = "Producto no encontrado";

    // Dependencia inyectada para acceder a carrito repository sin crearla manualmente.
    private final CarritoRepository carritoRepository;
    // Dependencia inyectada para acceder a catalog client sin crearla manualmente.
    private final CatalogClient catalogClient;

    // Procesa carrito service aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public CarritoService(CarritoRepository carritoRepository, CatalogClient catalogClient) {
        this.carritoRepository = carritoRepository;
        this.catalogClient = catalogClient;
    }

    /** Carrito con los precios y la disponibilidad de Catalog en este momento. */
    // Procesa carrito cotizado aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public record CarritoCotizado(Carrito carrito, Cotizacion cotizacion, Map<Long, Disponibilidad> disponibilidad) {

        LineaCotizada linea(Long productId) {
            return cotizacion.lineas().stream().filter(l -> l.productId().equals(productId)).findFirst().orElse(null);
        }
    }

    // ===================== Consultas =====================

    /** HU-19. Actualiza los precios guardados con los de Catalog; si Catalog no responde, muestra los guardados. */
    @Transactional
    // Procesa obtener aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public CarritoResponse obtener(Long userId) {
        Carrito carrito = carritoRepository.findByUserId(userId).orElse(null);
        if (carrito == null || carrito.estaVacio()) {
            return vacio();
        }
        try {
            CarritoCotizado c = cotizar(carrito);
            actualizarPrecios(c);
            return armar(c);
        } catch (ServicioNoDisponibleException e) {
            return sinCatalogo(carrito);
        }
    }

    // ===================== Comandos =====================

    /** HU-18: producto activo, con stock suficiente para la cantidad resultante. Si ya estaba, suma. */
    @Transactional
    // Procesa agregar aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public CarritoResponse agregar(Long userId, AgregarItemRequest req) {
        Carrito carrito = carritoRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> carritoRepository.save(new Carrito(userId)));
        int nueva = carrito.cantidadResultante(req.productId(), req.cantidadEntera());
        CarritoCotizado c = cotizarCon(carrito, req.productId(), nueva);
        validarComprable(c, req.productId(), nueva);
        carrito.agregar(req.productId(), req.cantidadEntera(), c.linea(req.productId()).precioFinalUnitario());
        actualizarPrecios(c);
        carritoRepository.save(carrito);
        return armar(c);
    }

    /** HU-20 / RN-12: entero >= 1 y sin superar el stock. */
    @Transactional
    // Procesa cambiar cantidad aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public CarritoResponse cambiarCantidad(Long userId, Long productId, int cantidad) {
        Carrito carrito = carritoRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto " + productId + " no está en tu carrito"));
        if (carrito.buscar(productId).isEmpty()) {
            throw new RecursoNoEncontradoException("El producto " + productId + " no está en tu carrito");
        }
        CarritoCotizado c = cotizarCon(carrito, productId, cantidad);
        validarComprable(c, productId, cantidad);
        carrito.cambiarCantidad(productId, cantidad, c.linea(productId).precioFinalUnitario());
        actualizarPrecios(c);
        return armar(c);
    }

    /** HU-21: acción independiente de bajar la cantidad. */
    @Transactional
    // Procesa quitar aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public CarritoResponse quitar(Long userId, Long productId) {
        Carrito carrito = carritoRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto " + productId + " no está en tu carrito"));
        carrito.quitar(productId);
        if (carrito.estaVacio()) {
            return vacio();
        }
        try {
            CarritoCotizado c = cotizar(carrito);
            actualizarPrecios(c);
            return armar(c);
        } catch (ServicioNoDisponibleException e) {
            return sinCatalogo(carrito);
        }
    }

    /** RF-COM-02/04: vaciar (la confirmación la pide el frontend antes de llamar). */
    @Transactional
    // Procesa vaciar aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public CarritoResponse vaciar(Long userId) {
        carritoRepository.findByUserIdForUpdate(userId).ifPresent(Carrito::vaciar);
        return vacio();
    }

    // ===================== Apoyo (también lo usa CheckoutService) =====================

    /** HU-25: carrito del usuario cotizado en este momento; vacío -> 409 (RF-COM-02: no se avanza a la compra). */
    @Transactional(readOnly = true)
    // Procesa cotizar para compra aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public CarritoCotizado cotizarParaCompra(Long userId) {
        Carrito carrito = carritoRepository.findByUserId(userId).orElse(null);
        if (carrito == null || carrito.estaVacio()) {
            throw new OperacionNoPermitidaException("Tu carrito está vacío", Map.of("carrito", "Agrega productos antes de pagar"));
        }
        return cotizar(carrito);
    }

    CarritoCotizado cotizar(Carrito carrito) {
        List<ItemSolicitado> items = carrito.getItems().stream()
                .map(i -> new ItemSolicitado(i.getProductId(), i.getCantidad())).toList();
        return consultarCatalogo(carrito, items);
    }

    /** Cotiza el carrito como quedaría con "cantidad" unidades del producto (antes de cambiarlo). */
    // Procesa cotizar con aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private CarritoCotizado cotizarCon(Carrito carrito, Long productId, int cantidad) {
        List<ItemSolicitado> items = new ArrayList<>();
        boolean incluido = false;
        for (ItemCarrito i : carrito.getItems()) {
            if (i.getProductId().equals(productId)) {
                items.add(new ItemSolicitado(productId, cantidad));
                incluido = true;
            } else {
                items.add(new ItemSolicitado(i.getProductId(), i.getCantidad()));
            }
        }
        if (!incluido) {
            items.add(new ItemSolicitado(productId, cantidad));
        }
        return consultarCatalogo(carrito, items);
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método consultarCatalogo (consultar catalogo).
    private CarritoCotizado consultarCatalogo(Carrito carrito, List<ItemSolicitado> items) {
        Cotizacion cotizacion = catalogClient.cotizar(items);
        List<ItemSolicitado> conocidos = cotizacion.lineas().stream()
                .filter(l -> !MSG_NO_ENCONTRADO.equals(l.motivo()))
                .map(l -> new ItemSolicitado(l.productId(), l.cantidad())).toList();
        Map<Long, Disponibilidad> disponibilidad = conocidos.isEmpty() ? Map.of()
                : catalogClient.disponibilidad(conocidos).stream()
                        .collect(Collectors.toMap(Disponibilidad::productId, Function.identity(), (a, b) -> a));
        return new CarritoCotizado(carrito, cotizacion, disponibilidad);
    }

    /** RF-COM-01/03: activo y con stock suficiente; si no, 404 o 409 con el motivo. */
    // Procesa validar comprable aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private static void validarComprable(CarritoCotizado c, Long productId, int cantidad) {
        LineaCotizada linea = c.linea(productId);
        if (linea == null || MSG_NO_ENCONTRADO.equals(linea.motivo())) {
            throw new RecursoNoEncontradoException(MSG_NO_ENCONTRADO);
        }
        if (!linea.comprable()) {
            throw new OperacionNoPermitidaException(linea.motivo(), Map.of("productId", linea.motivo()));
        }
        Disponibilidad d = c.disponibilidad().get(productId);
        if (d == null || !d.haySuficiente()) {
            String motivo = d != null && "AGOTADO".equals(d.disponibilidad())
                    ? "El producto está agotado"
                    : "No hay stock suficiente para " + cantidad + " unidades";
            throw new OperacionNoPermitidaException(motivo, Map.of("cantidad", motivo));
        }
    }

    // Procesa actualizar precios aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    static void actualizarPrecios(CarritoCotizado c) {
        for (LineaCotizada l : c.cotizacion().lineas()) {
            if (l.comprable() && l.precioFinalUnitario() != null) {
                c.carrito().actualizarPrecio(l.productId(), l.precioFinalUnitario());
            }
        }
        c.carrito().calcularTotal();
    }

    /** Respuesta con los datos de Catalog. El total suma solo lo comprable; el resto se informa en avisos. */
    // Procesa armar aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    static CarritoResponse armar(CarritoCotizado c) {
        Map<Long, LineaCotizada> lineas = c.cotizacion().lineas().stream()
                .collect(Collectors.toMap(LineaCotizada::productId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<ItemCarritoResponse> items = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        int unidades = 0;
        boolean listo = true;
        for (ItemCarrito i : c.carrito().getItems()) {
            LineaCotizada l = lineas.get(i.getProductId());
            Disponibilidad d = c.disponibilidad().get(i.getProductId());
            boolean comprable = l != null && l.comprable();
            boolean alcanza = d != null && d.haySuficiente();
            String motivo = null;
            if (!comprable) {
                motivo = l == null || l.motivo() == null ? MSG_NO_ENCONTRADO : l.motivo();
            } else if (!alcanza) {
                motivo = d != null && "AGOTADO".equals(d.disponibilidad()) ? "El producto está agotado"
                        : "No hay stock suficiente para " + i.getCantidad() + " unidades";
            }
            if (motivo != null) {
                listo = false;
                avisos.add((l != null && l.nombre() != null ? l.nombre() : "Producto " + i.getProductId()) + ": " + motivo);
            }
            unidades += i.getCantidad();
            if (comprable) {
                subtotal = subtotal.add(l.precioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())));
                descuento = descuento.add(l.descuentoUnitario().multiply(BigDecimal.valueOf(i.getCantidad())));
                total = total.add(l.subtotal());
            }
            items.add(new ItemCarritoResponse(i.getProductId(), l == null ? null : l.nombre(), i.getCantidad(),
                    comprable ? l.precioUnitario() : null, comprable ? l.descuentoUnitario() : null,
                    comprable ? l.precioFinalUnitario() : null, comprable ? l.subtotal() : BigDecimal.ZERO,
                    d == null ? null : d.disponibilidad(), d == null ? null : d.haySuficiente(), comprable, motivo));
        }
        return new CarritoResponse(items, items.size(), unidades, subtotal, descuento, total, items.isEmpty(),
                listo && !items.isEmpty(), true, avisos);
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método sinCatalogo (sin catalogo).
    private static CarritoResponse sinCatalogo(Carrito carrito) {
        List<ItemCarritoResponse> items = carrito.getItems().stream()
                .map(i -> new ItemCarritoResponse(i.getProductId(), null, i.getCantidad(), null, null, i.getPrecioUnitario(),
                        i.calcularSubtotal(), null, null, null, null))
                .toList();
        int unidades = carrito.getItems().stream().mapToInt(ItemCarrito::getCantidad).sum();
        return new CarritoResponse(items, items.size(), unidades, carrito.getTotal(), BigDecimal.ZERO, carrito.getTotal(),
                false, false, false, List.of("No fue posible actualizar precios y stock con el catálogo; "
                + "se muestran los últimos precios conocidos. No se puede comprar hasta que se actualicen"));
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método vacio (vacio).
    private static CarritoResponse vacio() {
        return new CarritoResponse(List.of(), 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true, false, true,
                List.of("Tu carrito está vacío"));
    }
}
