package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.EstadoOrden;

import java.util.List;

/**
 * HU-36 / RF-MIP-03: estado, avance, mensaje, despacho (si existe) e historial. Un pedido CANCELADO no se
 * muestra avanzando: sus pasos quedan sin completar y cancelado = true.
 */
public record SeguimientoResponse(Long id, String numeroPedido, EstadoOrden estado, String mensaje, boolean cancelado,
                                  boolean finalizado, List<Paso> pasos, DespachoResponse despacho,
                                  List<HistorialPedidoResponse> historial) {

    public record Paso(EstadoOrden estado, boolean completado, boolean actual) {
    }
}
