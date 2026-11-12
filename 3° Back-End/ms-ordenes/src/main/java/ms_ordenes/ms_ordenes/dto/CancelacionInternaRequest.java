package ms_ordenes.ms_ordenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Contrato INTERNO PostSale -> Orders (Lote 4): cancelación APROBADA en PostVenta (RN-24, RN-26, RN-39). */
public record CancelacionInternaRequest(
        @NotNull(message = "La solicitud es obligatoria") @Positive Long solicitudId,
        @NotBlank(message = "El motivo es obligatorio") @Size(max = 255) String motivo) {
}
