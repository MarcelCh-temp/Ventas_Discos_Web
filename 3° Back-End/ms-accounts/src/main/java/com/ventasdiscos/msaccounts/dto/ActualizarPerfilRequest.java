package com.ventasdiscos.msaccounts.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * HU-05: PATCH parcial, solo campos permitidos. Un campo null = no se modifica.
 * RUT y rol NO están aquí: no son editables desde el perfil.
 */
public record ActualizarPerfilRequest(
        @Size(max = 80, message = "Máximo 80 caracteres") @Pattern(regexp = ".*\\S.*", message = "No puede quedar vacío") String nombres,
        @Size(max = 80, message = "Máximo 80 caracteres") @Pattern(regexp = ".*\\S.*", message = "No puede quedar vacío") String apellidos,
        @Email(message = "El correo no tiene un formato válido") @Size(max = 120) @Pattern(regexp = ".*\\S.*", message = "No puede quedar vacío") String correo
) {
}
