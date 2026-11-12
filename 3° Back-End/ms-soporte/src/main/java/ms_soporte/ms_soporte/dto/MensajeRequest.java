package ms_soporte.ms_soporte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lote 1 (HU-62, HU-65): respuesta dentro de una solicitud. El autor y la fecha los pone el Service. */
public record MensajeRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 2000, message = "El mensaje no puede superar 2000 caracteres")
        String contenido
) {
}
