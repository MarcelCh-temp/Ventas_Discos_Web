package com.ventasdiscos.msaccounts.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** HU-02: mismo formulario para Cliente y Administrador; no hay selector de rol. */
public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no tiene un formato válido") String correo,
        @NotBlank(message = "La contraseña es obligatoria") String password
) {
}
