package ms_soporte.ms_soporte.dto;

import ms_soporte.ms_soporte.model.EstadoTicket;
import ms_soporte.ms_soporte.model.HistorialEstadoSoporte;
import ms_soporte.ms_soporte.model.MensajeSoporte;
import ms_soporte.ms_soporte.model.RolAutor;
import ms_soporte.ms_soporte.model.Ticket;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lote 1 (HU-61): detalle con conversación e historial en orden cronológico.
 * Se usa un DTO y no la entidad para no depender de la carga "lazy" de JPA al convertir a JSON.
 */
public record TicketDetalleResponse(
        Long id,
        String numeroTicket,
        Long idUsuario,
        Long orderId,
        String asunto,
        String categoria,
        EstadoTicket estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaCierre,
        List<Mensaje> mensajes,
        List<CambioEstado> historial
) {
    public record Mensaje(Long id, RolAutor autor, Long autorId, String contenido, LocalDateTime fechaEnvio) {
        static Mensaje desde(MensajeSoporte m) {
            return new Mensaje(m.getId(), m.getAutor(), m.getAutorId(), m.getContenido(), m.getFechaEnvio());
        }
    }

    // Lote 1.1 (DOC-01, RNF-09): usuarioResponsableId = quién hizo el cambio (null en registros antiguos)
    public record CambioEstado(EstadoTicket estadoAnterior, EstadoTicket estadoNuevo, LocalDateTime fechaHora,
                               Long usuarioResponsableId) {
        static CambioEstado desde(HistorialEstadoSoporte h) {
            return new CambioEstado(h.getEstadoAnterior(), h.getEstadoNuevo(), h.getFechaHora(),
                    h.getUsuarioResponsableId());
        }
    }

    public static TicketDetalleResponse desde(Ticket t) {
        return new TicketDetalleResponse(t.getId(), t.getNumeroTicket(), t.getIdUsuario(), t.getOrderId(),
                t.getAsunto(), t.getCategoria(), t.getEstado(), t.getFechaCreacion(), t.getFechaCierre(),
                t.getMensajes().stream().map(Mensaje::desde).toList(),
                t.getHistorial().stream().map(CambioEstado::desde).toList());
    }
}
