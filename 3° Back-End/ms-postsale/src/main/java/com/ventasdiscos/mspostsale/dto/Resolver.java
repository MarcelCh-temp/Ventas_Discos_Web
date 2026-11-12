package com.ventasdiscos.mspostsale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Clase original. HU-74 / HU-75: aprobar (true) o rechazar (false), con motivo. El Administrador responsable sale de
 * su token (DOC-09); no se acepta desde el frontend.
 */
public record Resolver(
        @NotNull(message = "Indica si apruebas (true) o rechazas (false)") Boolean aprobar,
        @NotBlank(message = "El motivo es obligatorio") @Size(max = 255, message = "Máximo 255 caracteres") String motivo
) {
}
