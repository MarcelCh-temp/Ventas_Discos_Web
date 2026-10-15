package com.ventasdiscos.msaccounts.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HU-07 (contraseña): recuperación simulada en pantalla.
 * DECISIÓN TOMADA (validar con profesor): se pide correo + RUT para que no baste
 * con conocer el correo ajeno.
 */
public record RecuperarPasswordRequest(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no tiene un formato válido") String correo,
        @NotBlank(message = "El RUT es obligatorio") String rut,
        @NotBlank(message = "La nueva contraseña es obligatoria") @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String nuevaPassword,
        @NotBlank(message = "Debes confirmar la nueva contraseña") String confirmacionPassword
) {
}
