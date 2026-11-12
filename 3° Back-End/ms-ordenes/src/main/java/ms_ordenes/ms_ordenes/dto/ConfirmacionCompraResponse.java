package ms_ordenes.ms_ordenes.dto;

/** HU-25: confirmación inmediata con el identificador del pedido (201 CONFIRMADO o 202 PENDIENTE). */
public record ConfirmacionCompraResponse(String mensaje, PagoSimuladoResponse pago, PedidoDetalleResponse pedido) {
}
