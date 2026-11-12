package com.ventasdiscos.mspostsale.dto;

/**
 * Lote 4 · HU-74: resultado de resolver una cancelación (vista Administrador).
 * estadoPedido: estado del pedido en Orders tras la decisión. reposicionStockPendiente: solo al aprobar; true si
 * Catalog todavía no confirmó la reposición que hace Orders. Repetir la aprobación (misma solicitud) la completa sin
 * reponer dos veces ni cambiar al responsable. En un rechazo va null.
 */
public record CancelacionResueltaResponse(SolicitudDetalleResponse solicitud, String estadoPedido,
                                          Boolean reposicionStockPendiente) {
}
