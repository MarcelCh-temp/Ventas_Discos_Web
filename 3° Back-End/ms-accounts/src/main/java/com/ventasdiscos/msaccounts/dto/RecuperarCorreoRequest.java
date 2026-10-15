package com.ventasdiscos.msaccounts.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HU-07 (correo): identifica la cuenta por RUT y registra el nuevo correo con confirmación. */
public record RecuperarCorreoRequest(
        @NotBlank(message = "El RUT es obligatorio") String rut,
        @NotBlank(message = "El nuevo correo es obligatorio") @Email(message = "El correo no tiene un formato válido") @Size(max = 120) String nuevoCorreo,
        @NotBlank(message = "Debes confirmar el nuevo correo") String confirmacionCorreo
) {
}
