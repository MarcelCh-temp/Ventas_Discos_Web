package com.ventasdiscos.msaccounts.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HU-01: datos que LLEGAN del formulario de registro.
 * No trae "rol": así nadie puede autoasignarse Administrador (RN-01, evita mass assignment).
 * Las confirmaciones existen solo aquí; no se guardan en la BD.
 */
public record RegistroRequest(
        @NotBlank(message = "El RUT es obligatorio") String rut,
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 80, message = "Máximo 80 caracteres") String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 80, message = "Máximo 80 caracteres") String apellidos,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no tiene un formato válido") @Size(max = 120) String correo,
        @NotBlank(message = "Debes confirmar el correo") String confirmacionCorreo,
        @NotBlank(message = "La contraseña es obligatoria") @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
        @NotBlank(message = "Debes confirmar la contraseña") String confirmacionPassword
) {
}
