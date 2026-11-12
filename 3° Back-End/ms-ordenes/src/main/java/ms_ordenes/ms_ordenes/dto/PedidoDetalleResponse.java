package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.Orden;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RF-PED-03 / RF-MIP-02: detalle completo. subtotal = total + descuento (precios antes de promociones).
 * Nunca se expone la entidad JPA.
 * Revisión Lote 3: reposicionStockPendiente = pedido CANCELADO cuyo stock Catalog todavía no confirmó haber repuesto
 * (se resuelve repitiendo la cancelación; no repone dos veces). Campo agregado al final.
 */
public record PedidoDetalleResponse(Long id, String numeroPedido, Long userId, LocalDateTime fechaCompra,
                                    EstadoOrden estado, List<ItemPedidoResponse> items, BigDecimal subtotal,
                                    BigDecimal descuento, BigDecimal total, DatosEntregaResponse datosEntrega,
                                    DespachoResponse despacho, List<HistorialPedidoResponse> historial,
                                    boolean reposicionStockPendiente) {

    public static PedidoDetalleResponse from(Orden o, boolean vistaAdministrador) {
        return new PedidoDetalleResponse(o.getId(), o.getNumeroPedido(), o.getIdUsuario(), o.getFechaCreacion(),
                o.getEstado(), o.getItems().stream().map(ItemPedidoResponse::from).toList(),
                o.getTotal().add(o.getDescuentoHistorico()), o.getDescuentoHistorico(), o.getTotal(),
                DatosEntregaResponse.from(o.getDatosEntrega()), DespachoResponse.from(o.getDespacho()),
                o.getHistorial().stream().map(h -> HistorialPedidoResponse.from(h, vistaAdministrador)).toList(),
                o.reposicionPendiente());
    }
}
