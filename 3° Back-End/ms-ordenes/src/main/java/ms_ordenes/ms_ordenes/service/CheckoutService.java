package ms_ordenes.ms_ordenes.service;

import ms_ordenes.ms_ordenes.dto.CarritoResponse;
import ms_ordenes.ms_ordenes.dto.ConfirmacionCompraResponse;
import ms_ordenes.ms_ordenes.dto.ConfirmarCompraRequest;
import ms_ordenes.ms_ordenes.dto.DatosEntregaRequest;
import ms_ordenes.ms_ordenes.dto.DatosEntregaResponse;
import ms_ordenes.ms_ordenes.dto.ItemCarritoResponse;
import ms_ordenes.ms_ordenes.dto.MetodoEntregaResponse;
import ms_ordenes.ms_ordenes.dto.OpcionesPagoResponse;
import ms_ordenes.ms_ordenes.dto.PagoSimuladoResponse;
import ms_ordenes.ms_ordenes.dto.PedidoDetalleResponse;
import ms_ordenes.ms_ordenes.dto.ResumenCompraResponse;
import ms_ordenes.ms_ordenes.dto.ValidarCheckoutRequest;
import ms_ordenes.ms_ordenes.exception.OperacionNoPermitidaException;
import ms_ordenes.ms_ordenes.exception.PagoRechazadoException;
import ms_ordenes.ms_ordenes.exception.ValidacionNegocioException;
import ms_ordenes.ms_ordenes.model.Carrito;
import ms_ordenes.ms_ordenes.model.DatosEntrega;
import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.MetodoEntrega;
import ms_ordenes.ms_ordenes.model.MetodoPago;
import ms_ordenes.ms_ordenes.model.ResultadoPagoSimulado;
import ms_ordenes.ms_ordenes.repository.CarritoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lote 3 · HU-22 a HU-25 (checkout). Productos, cantidades y precios salen del carrito guardado y de Catalog;
 * el total lo calcula el backend (RN-13). El pago es una SIMULACIÓN ACADÉMICA: el resultado lo indica el request
 * (por defecto APROBADO), sin pasarela real ni datos de tarjeta.
 * Orden de confirmación (ERS 12.8): datos de entrega -> productos y stock -> total -> pago simulado ->
 * pedido PENDIENTE (snapshot) -> descuento idempotente en Catalog -> CONFIRMADO + aviso.
 */
@Service
public class CheckoutService {

    static final String AVISO_SIMULACION = "Simulación académica de pago: no se usa una pasarela real ni se piden "
            + "datos de tarjeta. El resultado lo indica el campo resultadoSimulado (por defecto APROBADO).";

    private final CarritoService carritoService;
    private final OrdenService ordenService;
    private final CarritoRepository carritoRepository;

    public CheckoutService(CarritoService carritoService, OrdenService ordenService, CarritoRepository carritoRepository) {
        this.carritoService = carritoService;
        this.ordenService = ordenService;
        this.carritoRepository = carritoRepository;
    }

    /** RF-COM-05: métodos de entrega disponibles y sus campos obligatorios. */
    public List<MetodoEntregaResponse> metodosEntrega() {
        return Arrays.stream(MetodoEntrega.values()).map(MetodoEntregaResponse::from).toList();
    }

    /** RF-COM-08: métodos de pago permitidos de la simulación. */
    public OpcionesPagoResponse opcionesPago() {
        return new OpcionesPagoResponse(true, AVISO_SIMULACION,
                Arrays.stream(MetodoPago.values()).map(m -> new OpcionesPagoResponse.MetodoPagoResponse(m, m.getNombre()))
                        .toList(),
                List.of(ResultadoPagoSimulado.values()), ResultadoPagoSimulado.APROBADO);
    }

    /** HU-24 / RF-COM-07: resumen recalculado (productos, cantidades, precios, descuentos, entrega y total). */
    @Transactional
    public ResumenCompraResponse validar(Long userId, ValidarCheckoutRequest req) {
        DatosEntrega datos = datosEntrega(req.datosEntrega());
        Carrito carrito = carritoRepository.findByUserId(userId).orElse(null);
        if (carrito == null || carrito.estaVacio()) {
            return new ResumenCompraResponse(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    DatosEntregaResponse.from(datos), false, Map.of("carrito", "Tu carrito está vacío"));
        }
        CarritoService.CarritoCotizado c = carritoService.cotizar(carrito);
        CarritoService.actualizarPrecios(c);
        CarritoResponse r = CarritoService.armar(c);
        return new ResumenCompraResponse(r.items(), r.subtotal(), r.descuento(), r.total(),
                DatosEntregaResponse.from(datos), r.listoParaComprar(), problemas(r));
    }

    /**
     * HU-25 / RF-COM-08 / RN-13 / RN-14 / RN-15. 201 = CONFIRMADO; 202 = PENDIENTE (Catalog no confirmó el
     * descuento). Rechazos: 400 datos, 402 pago simulado rechazado, 409 carrito vacío / producto no comprable /
     * sin stock / total cambiado, 503 Catalog no disponible. En 402 y 409 previos al pedido no se guarda nada.
     */
    public ConfirmacionCompraResponse confirmar(Long userId, ConfirmarCompraRequest req) {
        DatosEntrega datos = datosEntrega(req.datosEntrega());
        CarritoService.CarritoCotizado c = carritoService.cotizarParaCompra(userId);
        CarritoResponse r = CarritoService.armar(c);
        if (!r.listoParaComprar()) {
            throw new OperacionNoPermitidaException("Hay productos que no se pueden comprar; corrige tu carrito",
                    problemas(r));
        }
        if (req.totalEsperado().compareTo(r.total()) != 0) {
            throw new OperacionNoPermitidaException("El total de tu compra cambió: ahora es $" + r.total().toPlainString()
                    + ". Revisa el resumen antes de pagar",
                    Map.of("totalEsperado", "El total actualizado es " + r.total().toPlainString()));
        }
        if (req.resultadoEfectivo() == ResultadoPagoSimulado.RECHAZADO) {
            throw new PagoRechazadoException("El pago simulado fue rechazado (simulación académica). No se generó el "
                    + "pedido ni se descontó stock; tu carrito sigue disponible para reintentar");
        }
        Long id = ordenService.crearDesdeCarrito(userId, c, datos, req.metodoPago());
        PedidoDetalleResponse pedido = ordenService.descontarStockDeCompra(id);
        String mensaje = pedido.estado() == EstadoOrden.CONFIRMADO
                ? "Compra confirmada. Tu número de pedido es " + pedido.numeroPedido()
                : "Recibimos tu pedido " + pedido.numeroPedido() + "; quedó pendiente de confirmación de stock";
        return new ConfirmacionCompraResponse(mensaje,
                new PagoSimuladoResponse(req.metodoPago(), ResultadoPagoSimulado.APROBADO, true), pedido);
    }

    /** Errores con la misma clave que la validación del DTO ("datosEntrega.campo"), para marcar el campo exacto. */
    private static DatosEntrega datosEntrega(DatosEntregaRequest d) {
        try {
            return DatosEntrega.crear(d.metodoEntrega(), d.nombreReceptor(), d.telefono(), d.direccion(), d.comuna(),
                    d.referencia());
        } catch (ValidacionNegocioException e) {
            Map<String, String> errores = new LinkedHashMap<>();
            if (e.getErrores() != null) {
                e.getErrores().forEach((campo, msg) -> errores.put("datosEntrega." + campo, msg));
            }
            throw new ValidacionNegocioException(e.getMessage(), errores);
        }
    }

    private static Map<String, String> problemas(CarritoResponse r) {
        Map<String, String> problemas = new LinkedHashMap<>();
        for (ItemCarritoResponse i : r.items()) {
            if (i.motivo() != null) {
                problemas.put("items[" + i.productId() + "]", i.motivo());
            }
        }
        return problemas;
    }
}
