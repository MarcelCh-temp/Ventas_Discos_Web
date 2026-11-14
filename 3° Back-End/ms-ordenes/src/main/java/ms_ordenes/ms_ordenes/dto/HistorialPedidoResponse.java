package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.HistorialPedido;

import java.time.LocalDateTime;

/** RF-PED-07: el Administrador ve el responsable; al Cliente se le entrega null (no necesita ids internos). */
public record HistorialPedidoResponse(LocalDateTime fechaHora, EstadoOrden estadoAnterior, EstadoOrden estadoNuevo,
                                      String motivo, String descripcion, Long usuarioResponsableId) {

    // Ejecuta from como parte de las responsabilidades de esta clase.
    public static HistorialPedidoResponse from(HistorialPedido h, boolean incluirResponsable) {
        return new HistorialPedidoResponse(h.getFechaHora(), h.getEstadoAnterior(), h.getEstadoNuevo(), h.getMotivo(),
                h.getDescripcion(), incluirResponsable ? h.getUsuarioResponsableId() : null);
    }
}
