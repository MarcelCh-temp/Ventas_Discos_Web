package ms_soporte.ms_soporte.dto;

import ms_soporte.ms_soporte.model.EstadoTicket;
import ms_soporte.ms_soporte.model.Ticket;

import java.time.LocalDateTime;

/** Lote 1 (HU-60, HU-63): fila de un listado (identificador, cliente, fecha, categoría/asunto, estado). */
public record TicketResumenResponse(
        Long id,
        String numeroTicket,
        Long idUsuario,
        Long orderId,
        String asunto,
        String categoria,
        EstadoTicket estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaCierre
) {
    // Ejecuta desde como parte de las responsabilidades de esta clase.
    public static TicketResumenResponse desde(Ticket t) {
        return new TicketResumenResponse(t.getId(), t.getNumeroTicket(), t.getIdUsuario(), t.getOrderId(),
                t.getAsunto(), t.getCategoria(), t.getEstado(), t.getFechaCreacion(), t.getFechaCierre());
    }
}
