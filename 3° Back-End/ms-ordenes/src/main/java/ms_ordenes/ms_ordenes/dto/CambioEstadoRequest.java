package ms_ordenes.ms_ordenes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ms_ordenes.ms_ordenes.model.EstadoOrden;

/** HU-29 / HU-31: nuevo estado. motivo obligatorio para CANCELADO (lo valida el modelo). */
public record CambioEstadoRequest(
        @NotNull(message = "Indica el nuevo estado") EstadoOrden estado,
        @Size(max = 255, message = "El motivo admite hasta 255 caracteres") String motivo) {
}
