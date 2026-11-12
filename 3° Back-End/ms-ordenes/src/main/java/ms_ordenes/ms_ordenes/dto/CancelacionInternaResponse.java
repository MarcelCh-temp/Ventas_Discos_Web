package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.EstadoOrden;

/**
 * yaCancelado = true si el pedido ya estaba CANCELADO: no se vuelve a reponer stock (reintento seguro).
 * Revisión Lote 3: reposicionStockPendiente = Catalog aún no confirmó la reposición; PostSale puede reintentar.
 */
public record CancelacionInternaResponse(Long pedidoId, String numeroPedido, EstadoOrden estado, boolean yaCancelado,
                                         boolean reposicionStockPendiente) {
}
