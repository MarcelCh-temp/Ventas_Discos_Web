package ms_ordenes.ms_ordenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** HU-33 / RF-PED-08. numeroSeguimiento opcional (sin tracking externo obligatorio). fechaDespacho: ahora si no viene. */
public record DespachoRequest(
        @NotBlank(message = "El transportista es obligatorio")
        @Size(max = 100, message = "El transportista admite hasta 100 caracteres") String transportista,
        @Size(max = 100, message = "El número de seguimiento admite hasta 100 caracteres") String numeroSeguimiento,
        LocalDateTime fechaDespacho) {
}
