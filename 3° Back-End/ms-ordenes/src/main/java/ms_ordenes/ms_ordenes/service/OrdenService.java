package ms_ordenes.ms_ordenes.service;

import ms_ordenes.ms_ordenes.client.AccountsClient;
import ms_ordenes.ms_ordenes.dto.ExisteResponse;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import ms_ordenes.ms_ordenes.client.CatalogClient;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.ItemSolicitado;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.LineaCotizada;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.RespuestaOperacion;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.ResultadoOperacion;
import ms_ordenes.ms_ordenes.client.NotificacionesClient;
import ms_ordenes.ms_ordenes.dto.CambioEstadoRequest;
import ms_ordenes.ms_ordenes.dto.CancelacionInternaRequest;
import ms_ordenes.ms_ordenes.dto.CancelacionInternaResponse;
import ms_ordenes.ms_ordenes.dto.CorreccionEntregaRequest;
import ms_ordenes.ms_ordenes.dto.DespachoRequest;
import ms_ordenes.ms_ordenes.dto.DespachoResponse;
import ms_ordenes.ms_ordenes.dto.HistorialPedidoResponse;
import ms_ordenes.ms_ordenes.dto.PageResponse;
import ms_ordenes.ms_ordenes.dto.PedidoDetalleResponse;
import ms_ordenes.ms_ordenes.dto.PedidoInternoResponse;
import ms_ordenes.ms_ordenes.dto.PedidoResumenResponse;
import ms_ordenes.ms_ordenes.dto.SeguimientoResponse;
import ms_ordenes.ms_ordenes.exception.OperacionNoPermitidaException;
import ms_ordenes.ms_ordenes.exception.RecursoNoEncontradoException;
import ms_ordenes.ms_ordenes.exception.ServicioNoDisponibleException;
import ms_ordenes.ms_ordenes.exception.ValidacionNegocioException;
import ms_ordenes.ms_ordenes.model.Carrito;
import ms_ordenes.ms_ordenes.model.DatosEntrega;
import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.ItemCarrito;
import ms_ordenes.ms_ordenes.model.ItemPedido;
import ms_ordenes.ms_ordenes.model.MetodoPago;
import ms_ordenes.ms_ordenes.model.Orden;
import ms_ordenes.ms_ordenes.repository.CarritoRepository;
import ms_ordenes.ms_ordenes.repository.OrdenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lote 3 · Pedidos (EP-05, EP-06, CC-02, CC-03). Reglas de estado en Orden (Model); aquí la coordinación:
 * pertenencia (userId del token), descuento y reposición de stock en Catalog (idempotentes, DOC-08:
 * ORDERS-PEDIDO-{id}-VENTA / ORDERS-PEDIDO-{id}-CANCELACION), historial y avisos a Notifications después del
 * commit. Los cambios de estado bloquean la fila del pedido (findForUpdate): una cancelación y un despacho
 * simultáneos no se pisan y el stock se repone una sola vez (RN-39, T-07).
 */
@Service
public class OrdenService {

    private static final Logger log = LoggerFactory.getLogger(OrdenService.class);
    // Constante OP_VENTA: valor compartido que no cambia durante la ejecución.
    static final String OP_VENTA = "ORDERS-PEDIDO-%d-VENTA";
    // Constante OP_CANCELACION: valor compartido que no cambia durante la ejecución.
    static final String OP_CANCELACION = "ORDERS-PEDIDO-%d-CANCELACION";
    // Constante TAMANO_MAXIMO: valor compartido que no cambia durante la ejecución.
    static final int TAMANO_MAXIMO = 100;
    private static final List<EstadoOrden> FLUJO = List.of(EstadoOrden.PENDIENTE, EstadoOrden.CONFIRMADO,
            EstadoOrden.PREPARANDO, EstadoOrden.DESPACHADO, EstadoOrden.ENTREGADO);

    // Dependencia inyectada para acceder a orden repository sin crearla manualmente.
    private final OrdenRepository ordenRepository;
    // Dependencia inyectada para acceder a carrito repository sin crearla manualmente.
    private final CarritoRepository carritoRepository;
    // Dependencia inyectada para acceder a catalog client sin crearla manualmente.
    private final CatalogClient catalogClient;
    // Dependencia inyectada para acceder a notificaciones client sin crearla manualmente.
    private final NotificacionesClient notificacionesClient;
    // Dato accounts client del registro.
    private final AccountsClient accountsClient;
    // Dato transaccion del registro.
    private final TransactionTemplate transaccion;

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método OrdenService (orden service).
    public OrdenService(OrdenRepository ordenRepository, CarritoRepository carritoRepository,
                        CatalogClient catalogClient, NotificacionesClient notificacionesClient,
                        AccountsClient accountsClient, PlatformTransactionManager transactionManager) {
        this.ordenRepository = ordenRepository;
        this.carritoRepository = carritoRepository;
        this.catalogClient = catalogClient;
        this.notificacionesClient = notificacionesClient;
        this.accountsClient = accountsClient;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    // ===================== Lote 5: contrato interno para Accounts (HU-78) =====================

    /** RN-36: pedidos activos = PENDIENTE, CONFIRMADO, PREPARANDO o DESPACHADO. Solo lectura: no modifica pedidos. */
    @Transactional(readOnly = true)
    // Procesa tiene pedidos activos aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public ExisteResponse tienePedidosActivos(Long userId) {
        return new ExisteResponse(userId, ordenRepository.existsByIdUsuarioAndEstadoIn(userId,
                List.of(EstadoOrden.PENDIENTE, EstadoOrden.CONFIRMADO, EstadoOrden.PREPARANDO, EstadoOrden.DESPACHADO)));
    }

    // ===================== Compra (la llama CheckoutService) =====================

    /**
     * HU-25 paso 1: crea el pedido PENDIENTE con el snapshot de la cotización y vacía el carrito, en UNA
     * transacción. El carrito se bloquea: un doble clic encuentra el carrito vacío (409) y no crea otro pedido.
     */
    @Transactional
    public Long crearDesdeCarrito(Long userId, CarritoService.CarritoCotizado cotizado, DatosEntrega datos,
                                  MetodoPago metodoPago) {
        Carrito carrito = carritoRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new OperacionNoPermitidaException("Tu carrito está vacío"));
        Map<Long, Integer> cotizadas = new HashMap<>();
        cotizado.cotizacion().lineas().forEach(l -> cotizadas.put(l.productId(), l.cantidad()));
        Map<Long, Integer> actuales = new HashMap<>();
        carrito.getItems().forEach(i -> actuales.put(i.getProductId(), i.getCantidad()));
        if (carrito.estaVacio()) {
            throw new OperacionNoPermitidaException("Tu carrito está vacío");
        }
        if (!cotizadas.equals(actuales)) {
            throw new OperacionNoPermitidaException("Tu carrito cambió mientras confirmabas la compra; revisa el resumen");
        }
        List<ItemPedido> items = new ArrayList<>();
        BigDecimal descuento = BigDecimal.ZERO;
        for (ItemCarrito i : carrito.getItems()) {
            LineaCotizada l = cotizado.linea(i.getProductId());
            items.add(new ItemPedido(i.getProductId(), l.nombre(), l.precioFinalUnitario(), i.getCantidad()));
            descuento = descuento.add(l.descuentoUnitario().multiply(BigDecimal.valueOf(i.getCantidad())));
        }
        // Lote 5 (HU-78, RN-37, RN-44, T-06): justo antes de crear el pedido, con el carrito ya bloqueado, Accounts
        // debe confirmar que la cuenta está ACTIVA. Si no (403) o no responde (503), se lanza la excepción y la
        // transacción se deshace: no se crea pedido y el carrito queda igual. Ver REGISTRO_LOTE_5 (carrera residual).
        accountsClient.exigirCuentaActiva(userId);
        Orden orden = Orden.crear(userId, items, descuento, datos,
                "Pago simulado aprobado (" + metodoPago + ", simulación académica)");
        orden = ordenRepository.save(orden);
        orden.asignarNumeroPedido();
        carrito.vaciar();
        return orden.getId();
    }

    /**
     * HU-25 paso 2: descuenta el stock en Catalog (SALIDA idempotente) y deja el resultado en el pedido.
     *  - aplicada            -> CONFIRMADO + aviso PEDIDO_CREADO;
     *  - SIN_STOCK confirmado -> CANCELADO (sin stock que reponer) + carrito restaurado + 409;
     *  - cualquier otra cosa  -> sigue PENDIENTE con un evento en el historial (202). Revisión Lote 3: un 409 por
     *    operacionId repetida, un 400/404 o la falta de respuesta NO cancelan la compra: no demuestran falta de stock.
     *    El Administrador revisa y confirma reintentando la MISMA operación (no descuenta dos veces).
     * La llamada a Catalog va FUERA de transacción; cada resultado se guarda en su propia transacción.
     */
    public PedidoDetalleResponse descontarStockDeCompra(Long ordenId) {
        DatosVenta venta = transaccion.execute(s -> {
            Orden o = ordenRepository.findById(ordenId).orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
            return new DatosVenta(items(o), o.getNumeroPedido());
        });
        RespuestaOperacion r = venta(ordenId, venta.numeroPedido(), venta.items());
        if (r.resultado() == ResultadoOperacion.SIN_STOCK) {
            transaccion.executeWithoutResult(s -> {
                Orden o = bloquear(ordenId);
                o.cambiarEstado(EstadoOrden.CANCELADO, "Sin stock al confirmar la compra",
                        "Catalog rechazó el descuento por falta de stock: " + r.mensaje() + ". No se descontó stock", null);
                restaurarCarrito(o);
            });
            throw new OperacionNoPermitidaException("No se pudo completar la compra: " + r.mensaje()
                    + ". No se descontó stock y tu carrito se restauró", Map.of("carrito", String.valueOf(r.mensaje())));
        }
        return transaccion.execute(s -> {
            Orden o = bloquear(ordenId);
            if (r.resultado() == ResultadoOperacion.APLICADA) {
                if (o.getEstado() == EstadoOrden.PENDIENTE) {
                    o.cambiarEstado(EstadoOrden.CONFIRMADO, "Stock descontado",
                            "Pago simulado aprobado y stock descontado en Catalog", null);
                }
                notificarTrasCommit(evento(o, "CREADO"), o.getIdUsuario(), "PEDIDO_CREADO", "Compra confirmada",
                        "Tu pedido " + o.getNumeroPedido() + " fue confirmado. Total: $" + o.getTotal().toPlainString(),
                        o.getId());
            } else {
                String detalle = r.resultado() == ResultadoOperacion.NO_CONFIRMADA
                        ? "Catalog no respondió; el Administrador debe confirmar el pedido"
                        : "Catalog respondió " + r.status() + " sin confirmar falta de stock (" + r.mensaje()
                        + "); requiere revisión del Administrador";
                o.registrarEvento("Descuento de stock por confirmar", detalle, null);
                notificarTrasCommit(evento(o, "CREADO"), o.getIdUsuario(), "PEDIDO_CREADO", "Pedido recibido",
                        "Tu pedido " + o.getNumeroPedido() + " quedó pendiente de confirmación", o.getId());
            }
            return PedidoDetalleResponse.from(o, false);
        });
    }

    // Servicio: concentra reglas de negocio y coordina operaciones entre repositorios y otros servicios. Clase DatosVenta.
    private record DatosVenta(List<ItemSolicitado> items, String numeroPedido) {
    }

    // ===================== Cliente (EP-06) =====================

    /** HU-34 / HU-37: solo pedidos del usuario del token; búsqueda por número o id, estado y fechas. */
    @Transactional(readOnly = true)
    public PageResponse<PedidoResumenResponse> listarMios(Long userId, String q, EstadoOrden estado, LocalDate desde,
                                                          LocalDate hasta, int page, int size) {
        return listar(userId, q, estado, desde, hasta, page, size, false);
    }

    /** HU-35: un pedido ajeno se responde como inexistente (no revela que existe, T-09). */
    @Transactional(readOnly = true)
    // Procesa detalle mio aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public PedidoDetalleResponse detalleMio(Long userId, Long id) {
        return PedidoDetalleResponse.from(propio(userId, ordenRepository.findById(id).orElse(null)), false);
    }

    /** HU-36 / RN-40: por id ("15") o por número ("PED-000015"), siempre comprobando que sea del Cliente. */
    @Transactional(readOnly = true)
    // Procesa seguimiento aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public SeguimientoResponse seguimiento(Long userId, String referencia) {
        String ref = referencia == null ? "" : referencia.trim();
        Orden o = ref.matches("\\d{1,18}")
                ? ordenRepository.findById(Long.valueOf(ref)).orElse(null)
                : ordenRepository.findByNumeroPedido(ref.toUpperCase(Locale.ROOT)).orElse(null);
        o = propio(userId, o);
        boolean cancelado = o.getEstado() == EstadoOrden.CANCELADO;
        Set<EstadoOrden> alcanzados = new HashSet<>();
        o.getHistorial().forEach(h -> alcanzados.add(h.getEstadoNuevo()));
        int actual = FLUJO.indexOf(o.getEstado());
        List<SeguimientoResponse.Paso> pasos = new ArrayList<>();
        for (int i = 0; i < FLUJO.size(); i++) {
            EstadoOrden paso = FLUJO.get(i);
            boolean completado = cancelado ? alcanzados.contains(paso) : i <= actual;
            pasos.add(new SeguimientoResponse.Paso(paso, completado, !cancelado && i == actual));
        }
        return new SeguimientoResponse(o.getId(), o.getNumeroPedido(), o.getEstado(), mensajeSeguimiento(o), cancelado,
                o.estaFinalizado(), pasos, DespachoResponse.from(o.getDespacho()),
                o.getHistorial().stream().map(h -> HistorialPedidoResponse.from(h, false)).toList());
    }

    // ===================== Administrador (EP-05) =====================

    /** HU-26 / HU-27: búsqueda por número, id o nombre del receptor; filtros por estado, fechas y cliente. */
    @Transactional(readOnly = true)
    public PageResponse<PedidoResumenResponse> listarAdmin(String q, EstadoOrden estado, LocalDate desde, LocalDate hasta,
                                                           Long userId, int page, int size) {
        return listar(userId, q, estado, desde, hasta, page, size, true);
    }

    @Transactional(readOnly = true)
    // Procesa detalle admin aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public PedidoDetalleResponse detalleAdmin(Long id) {
        return PedidoDetalleResponse.from(buscar(id), true);
    }

    /**
     * HU-29 / HU-31. Transiciones del modelo (RN-16). Dos casos coordinan con Catalog:
     *  - PENDIENTE -> CONFIRMADO: reintenta el descuento con la misma operación (si ya estaba, no descuenta otra vez);
     *  - -> CANCELADO: motivo obligatorio y reposición de stock una sola vez (RF-PED-06, RN-26); ver cancelar().
     *    Repetir CANCELADO sobre un pedido con la reposición pendiente reintenta solo la reposición.
     */
    public PedidoDetalleResponse cambiarEstadoAdmin(Long id, CambioEstadoRequest req, Long adminId) {
        if (req.estado() == EstadoOrden.CANCELADO) {
            return cancelar(id, req.motivo(), "Cancelación administrativa", adminId, false).detalle();
        }
        return transaccion.execute(s -> {
            Orden o = bloquear(id);
            EstadoOrden nuevo = req.estado();
            if (!o.puedeTransicionarA(nuevo)) {
                throw new OperacionNoPermitidaException("No se puede cambiar el pedido de " + o.getEstado() + " a " + nuevo);
            }
            String descripcion = null;
            if (o.getEstado() == EstadoOrden.PENDIENTE && nuevo == EstadoOrden.CONFIRMADO && !o.getItems().isEmpty()) {
                exigirAplicada(venta(o.getId(), o.getNumeroPedido(), items(o)), "el descuento de stock");
                descripcion = "Stock descontado en Catalog";
            }
            o.cambiarEstado(nuevo, req.motivo(), descripcion, adminId);
            avisarCambioDeEstado(o, req.motivo());
            return PedidoDetalleResponse.from(o, true);
        });
    }

    /** HU-30 / RF-PED-05: solo datos de entrega permitidos, antes de DESPACHADO; cada corrección queda en el historial. */
    @Transactional
    // Procesa corregir entrega aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public PedidoDetalleResponse corregirEntrega(Long id, CorreccionEntregaRequest req, Long adminId) {
        Orden o = bloquear(id);
        List<String> cambios = o.corregirDatosEntrega(req.nombreReceptor(), req.telefono(), req.direccion(), req.comuna(),
                req.referencia(), adminId);
        if (cambios.isEmpty()) {
            throw new ValidacionNegocioException("No hay cambios: los datos enviados son iguales a los registrados");
        }
        return PedidoDetalleResponse.from(o, true);
    }

    /** HU-33 / RF-PED-08: PREPARANDO -> registra y pasa a DESPACHADO; DESPACHADO -> actualiza. */
    @Transactional
    // Procesa registrar despacho aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public PedidoDetalleResponse registrarDespacho(Long id, DespachoRequest req, Long adminId) {
        Orden o = bloquear(id);
        boolean eraPreparando = o.getEstado() == EstadoOrden.PREPARANDO;
        o.registrarDespacho(req.transportista(), req.numeroSeguimiento(), req.fechaDespacho(), adminId);
        if (eraPreparando) {
            avisarCambioDeEstado(o, null);
        } else {
            notificarTrasCommit(evento(o, "DESPACHO-" + o.getHistorial().size()), o.getIdUsuario(), "PEDIDO_ACTUALIZADO",
                    "Despacho actualizado", "Se actualizó la información de despacho de tu pedido " + o.getNumeroPedido(),
                    o.getId());
        }
        return PedidoDetalleResponse.from(o, true);
    }

    // ===================== Interno (PostSale, Lote 4) =====================

    @Transactional(readOnly = true)
    public PedidoInternoResponse consultaInterna(Long id) {
        return PedidoInternoResponse.from(buscar(id));
    }

    /**
     * Cancelación APROBADA en PostVenta. Revalida el estado actual (RN-24). Si ya estaba CANCELADO responde
     * yaCancelado=true sin volver a reponer: PostSale puede reintentar sin duplicar la reposición (RN-39, T-07).
     * Si la reposición había quedado pendiente, el reintento la completa (misma operación idempotente).
     */
    public CancelacionInternaResponse cancelarDesdePostVenta(Long id, CancelacionInternaRequest req) {
        ResultadoCancelacion r = cancelar(id, req.motivo(),
                "Cancelación aprobada en PostVenta (solicitud " + req.solicitudId() + ")", null, true);
        return new CancelacionInternaResponse(r.detalle().id(), r.detalle().numeroPedido(), r.detalle().estado(),
                r.yaCancelado(), r.detalle().reposicionStockPendiente());
    }

    // ===================== Apoyo =====================

    private record ResultadoCancelacion(PedidoDetalleResponse detalle, boolean yaCancelado) {
    }

    private record PasoCancelacion(List<ItemSolicitado> items, String numeroPedido, boolean yaCancelado,
                                   boolean reponer) {
    }

    /**
     * RF-PED-06 / RN-26 / RN-39. Revisión Lote 3: antes se reponía en Catalog y DESPUÉS se guardaba el CANCELADO;
     * si esa transacción fallaba, el stock quedaba repuesto con el pedido todavía activo (y despachable). Ahora:
     *  1) transacción con la fila bloqueada: valida, guarda CANCELADO y una marca "Reposición de stock pendiente"
     *     en el historial (si hay stock que reponer). Desde aquí el pedido ya no puede avanzar ni despacharse;
     *  2) fuera de la transacción: ENTRADA con ORDERS-PEDIDO-{id}-CANCELACION (idempotente en Catalog);
     *  3) transacción: si Catalog la confirmó, cierra la marca ("Stock repuesto").
     * Si 2) o 3) fallan, el pedido queda CANCELADO con la reposición pendiente (nunca sobra stock) y repetir la
     * cancelación reintenta SOLO la reposición, con la misma operación: Catalog no repone dos veces.
     */
    private ResultadoCancelacion cancelar(Long id, String motivo, String origen, Long responsableId,
                                          boolean desdePostVenta) {
        PasoCancelacion paso = transaccion.execute(s -> {
            Orden o = bloquear(id);
            if (o.getEstado() == EstadoOrden.CANCELADO) {
                if (o.reposicionPendiente()) {
                    return new PasoCancelacion(items(o), o.getNumeroPedido(), true, true);
                }
                if (desdePostVenta) {
                    return new PasoCancelacion(List.of(), o.getNumeroPedido(), true, false);
                }
                throw new OperacionNoPermitidaException("Un pedido CANCELADO no se puede cancelar");
            }
            if (!o.puedeCancelarse()) {
                throw new OperacionNoPermitidaException("Un pedido " + o.getEstado() + " no se puede cancelar");
            }
            if (motivo == null || motivo.isBlank()) {
                throw new ValidacionNegocioException("La cancelación requiere un motivo",
                        Map.of("motivo", "Indica el motivo de la cancelación"));
            }
            String sinReponer = sinStockQueReponer(o);
            if (sinReponer == null) {
                o.cambiarEstado(EstadoOrden.CANCELADO, motivo, origen, responsableId);
                o.marcarReposicionPendiente("Reponer en Catalog con " + String.format(OP_CANCELACION, o.getId()),
                        responsableId);
            } else {
                o.cambiarEstado(EstadoOrden.CANCELADO, motivo, origen + ". " + sinReponer, responsableId);
            }
            avisarCambioDeEstado(o, motivo);
            return new PasoCancelacion(items(o), o.getNumeroPedido(), false, sinReponer == null);
        });
        if (paso.reponer()) {
            RespuestaOperacion r = catalogClient.aplicarOperacion(String.format(OP_CANCELACION, id),
                    "Cancelación pedido " + paso.numeroPedido(), "ENTRADA", paso.items());
            transaccion.executeWithoutResult(s -> {
                Orden o = bloquear(id);
                if (r.resultado() == ResultadoOperacion.APLICADA) {
                    o.confirmarReposicion(responsableId);
                } else if (r.resultado() != ResultadoOperacion.NO_CONFIRMADA) {
                    o.registrarEvento("Reposición rechazada por Catalog", "HTTP " + r.status() + ": " + r.mensaje()
                            + ". La reposición sigue pendiente; requiere revisión", responsableId);
                }
            });
            if (r.resultado() != ResultadoOperacion.APLICADA) {
                log.warn("Pedido {} cancelado con la reposición de stock PENDIENTE ({}): {}", id, r.resultado(), r.mensaje());
            }
        }
        PedidoDetalleResponse detalle = transaccion.execute(s -> PedidoDetalleResponse.from(buscar(id), true));
        return new ResultadoCancelacion(detalle, paso.yaCancelado());
    }

    /**
     * null = hay stock que reponer. Si el pedido seguía PENDIENTE no se sabe si el descuento se aplicó: se reenvía
     * la MISMA venta. Solo una falta de stock CONFIRMADA prueba que nunca se descontó (Catalog revisa la operacionId
     * antes que el stock: si ya estaba aplicada habría respondido "ya aplicada"). Cualquier otro rechazo no prueba
     * nada y la cancelación se detiene sin cambiar el pedido.
     */
    private String sinStockQueReponer(Orden o) {
        if (o.getItems().isEmpty()) {
            return "Pedido sin productos registrados: no hay stock que reponer";
        }
        if (o.getEstado() != EstadoOrden.PENDIENTE) {
            return null;
        }
        RespuestaOperacion v = venta(o.getId(), o.getNumeroPedido(), items(o));
        if (v.resultado() == ResultadoOperacion.APLICADA) {
            return null;
        }
        if (v.resultado() == ResultadoOperacion.SIN_STOCK) {
            return "El descuento de stock nunca se aplicó: no hay stock que reponer";
        }
        if (v.resultado() == ResultadoOperacion.NO_CONFIRMADA) {
            throw new ServicioNoDisponibleException("No fue posible confirmar con el catálogo si el stock se descontó;"
                    + " el pedido no cambió. Intenta nuevamente");
        }
        throw new OperacionNoPermitidaException("Catalog respondió " + v.status() + " (" + v.mensaje() + ") y no se puede"
                + " saber si el stock se descontó; el pedido no cambió y requiere revisión");
    }

    /**
     * Venta (SALIDA) idempotente. Revisión Lote 3: Catalog usa 409 para "stock insuficiente" y para "operacionId ya
     * registrada con otro contenido". Solo se informa SIN_STOCK si NO es lo segundo y la consulta de disponibilidad
     * confirma que falta stock; si no se puede confirmar, queda CONFLICTO (no se cancela nada automáticamente).
     */
    private RespuestaOperacion venta(Long ordenId, String numeroPedido, List<ItemSolicitado> items) {
        RespuestaOperacion r = catalogClient.aplicarOperacion(String.format(OP_VENTA, ordenId), "Venta pedido " + numeroPedido,
                "SALIDA", items);
        if (r.resultado() == ResultadoOperacion.CONFLICTO && !esOperacionRepetida(r) && faltaStockConfirmada(items)) {
            return new RespuestaOperacion(ResultadoOperacion.SIN_STOCK, r.mensaje(), r.status());
        }
        return r;
    }

    /** Mensaje de Catalog para una operacionId ya usada con otro contenido (InventarioService del Lote 2). */
    // Procesa es operacion repetida aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private static boolean esOperacionRepetida(RespuestaOperacion r) {
        return r.mensaje() != null && r.mensaje().contains("ya fue registrada");
    }

    // Procesa falta stock confirmada aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private boolean faltaStockConfirmada(List<ItemSolicitado> items) {
        try {
            return catalogClient.disponibilidad(items).stream().anyMatch(d -> !d.haySuficiente());
        } catch (RuntimeException e) {
            log.warn("No se pudo confirmar la falta de stock con Catalog: {}", e.getMessage());
            return false;
        }
    }

    // Procesa exigir aplicada aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private static void exigirAplicada(RespuestaOperacion r, String accion) {
        switch (r.resultado()) {
            case APLICADA -> {
            }
            case SIN_STOCK -> throw new OperacionNoPermitidaException("No hay stock suficiente para " + accion + ": "
                    + r.mensaje() + ". Puedes cancelar el pedido");
            case NO_CONFIRMADA -> throw new ServicioNoDisponibleException("No fue posible confirmar " + accion
                    + " con el catálogo; el pedido no cambió. Intenta nuevamente");
            default -> throw new OperacionNoPermitidaException("Catalog rechazó " + accion + " (HTTP " + r.status() + "): "
                    + r.mensaje() + ". El pedido no cambió; requiere revisión");
        }
    }

    /** Si la compra no se completó por falta de stock, el Cliente recupera su carrito. */
    // Procesa restaurar carrito aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private void restaurarCarrito(Orden o) {
        Carrito carrito = carritoRepository.findByUserIdForUpdate(o.getIdUsuario())
                .orElseGet(() -> carritoRepository.save(new Carrito(o.getIdUsuario())));
        for (ItemPedido i : o.getItems()) {
            if (carrito.buscar(i.getProductId()).isEmpty()) {
                carrito.agregar(i.getProductId(), i.getCantidad(), i.getPrecioHistorico());
            }
        }
    }

    // Procesa avisar cambio de estado aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private void avisarCambioDeEstado(Orden o, String motivo) {
        String mensaje = switch (o.getEstado()) {
            case CONFIRMADO -> "Tu pedido " + o.getNumeroPedido() + " fue confirmado";
            case PREPARANDO -> "Estamos preparando tu pedido " + o.getNumeroPedido();
            case DESPACHADO -> "Tu pedido va en camino (" + o.getNumeroPedido() + ")";
            case ENTREGADO -> "Tu pedido " + o.getNumeroPedido() + " fue entregado";
            case CANCELADO -> "Tu pedido " + o.getNumeroPedido() + " fue cancelado. Motivo: " + motivo;
            case PENDIENTE -> "Tu pedido " + o.getNumeroPedido() + " está pendiente";
        };
        notificarTrasCommit(evento(o, o.getEstado().name()), o.getIdUsuario(), "PEDIDO_ACTUALIZADO",
                "Tu pedido cambió de estado", mensaje, o.getId());
    }

    /** RN-17: "Tu pedido va en camino" en DESPACHADO; un cancelado no se muestra avanzando. */
    // Procesa mensaje seguimiento aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private static String mensajeSeguimiento(Orden o) {
        String sinDespacho = o.getDespacho() == null ? ". Aún no tiene información de despacho" : "";
        return switch (o.getEstado()) {
            case PENDIENTE -> "Recibimos tu pedido y estamos confirmando el stock" + sinDespacho;
            case CONFIRMADO -> "Tu pedido está confirmado" + sinDespacho;
            case PREPARANDO -> "Estamos preparando tu pedido" + sinDespacho;
            case DESPACHADO -> "Tu pedido va en camino";
            case ENTREGADO -> "Tu pedido fue entregado";
            case CANCELADO -> "Tu pedido fue cancelado";
        };
    }

    private PageResponse<PedidoResumenResponse> listar(Long userId, String q, EstadoOrden estado, LocalDate desde,
                                                       LocalDate hasta, int page, int size, boolean admin) {
        if (page < 0 || size < 1 || size > TAMANO_MAXIMO) {
            throw new ValidacionNegocioException("Paginación inválida: page >= 0 y size entre 1 y " + TAMANO_MAXIMO);
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ValidacionNegocioException("La fecha 'desde' no puede ser posterior a 'hasta'",
                    Map.of("desde", "Debe ser anterior o igual a 'hasta'"));
        }
        Specification<Orden> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (userId != null) {
                p.add(cb.equal(root.get("idUsuario"), userId));
            }
            if (estado != null) {
                p.add(cb.equal(root.get("estado"), estado));
            }
            if (desde != null) {
                p.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fechaCreacion"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                p.add(cb.lessThan(root.<LocalDateTime>get("fechaCreacion"), hasta.plusDays(1).atStartOfDay()));
            }
            if (q != null && !q.isBlank()) {
                String t = q.trim().toLowerCase(Locale.ROOT);
                List<Predicate> texto = new ArrayList<>();
                texto.add(cb.like(cb.lower(root.<String>get("numeroPedido")), "%" + t + "%"));
                if (t.matches("\\d{1,18}")) {
                    texto.add(cb.equal(root.get("id"), Long.valueOf(t)));
                }
                if (admin) {
                    Join<Orden, DatosEntrega> d = root.join("datosEntrega", JoinType.LEFT);
                    texto.add(cb.like(cb.lower(d.<String>get("nombreReceptor")), "%" + t + "%"));
                }
                p.add(cb.or(texto.toArray(new Predicate[0])));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        Page<Orden> pagina = ordenRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaCreacion", "id")));
        return PageResponse.of(pagina, pagina.getContent().stream().map(PedidoResumenResponse::from).toList());
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método buscar (buscar).
    private Orden buscar(Long id) {
        return ordenRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método bloquear (bloquear).
    private Orden bloquear(Long id) {
        Orden o = ordenRepository.findForUpdate(id).orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
        o.asignarNumeroPedido(); // pedidos antiguos: reciben su número la primera vez que se gestionan
        return o;
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método propio (propio).
    private static Orden propio(Long userId, Orden o) {
        if (o == null || !o.perteneceA(userId)) {
            throw new RecursoNoEncontradoException("Pedido no encontrado");
        }
        return o;
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método items (items).
    private static List<ItemSolicitado> items(Orden o) {
        return o.getItems().stream().map(i -> new ItemSolicitado(i.getProductId(), i.getCantidad())).toList();
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método evento (evento).
    private static String evento(Orden o, String accion) {
        return "ORDERS-PEDIDO-" + o.getId() + "-" + accion;
    }

    /** Igual que Soporte (Lote 1): el aviso se envía DESPUÉS del commit; sin transacción, de inmediato. */
    private void notificarTrasCommit(String eventoId, Long usuarioId, String tipo, String titulo, String mensaje,
                                     Long pedidoId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            enviar(eventoId, usuarioId, tipo, titulo, mensaje, pedidoId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                enviar(eventoId, usuarioId, tipo, titulo, mensaje, pedidoId);
            }
        });
    }

    // Procesa enviar aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private void enviar(String eventoId, Long usuarioId, String tipo, String titulo, String mensaje, Long pedidoId) {
        try {
            notificacionesClient.notificar(eventoId, usuarioId, tipo, titulo, mensaje, pedidoId);
        } catch (RuntimeException e) {
            log.warn("Aviso {} no enviado: {}", eventoId, e.getMessage());
        }
    }
}
