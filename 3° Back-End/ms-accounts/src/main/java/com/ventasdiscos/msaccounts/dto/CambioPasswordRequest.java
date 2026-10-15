package com.ventasdiscos.msaccounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HU-06: exige la contraseña actual + nueva + confirmación. */
public record CambioPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria") String passwordActual,
        @NotBlank(message = "La nueva contraseña es obligatoria") @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String nuevaPassword,
        @NotBlank(message = "Debes confirmar la nueva contraseña") String confirmacionPassword
) {
}
