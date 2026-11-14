package com.ventasdiscos.msaccounts.dto;

import jakarta.validation.constraints.Size;

/**
 * Lote 5 · HU-78 (decisión del equipo): la eliminación se confirma con la contraseña actual.
 * Es obligatoria cuando la cuenta está ACTIVA (lo valida el Service con BCrypt); al repetir el DELETE de una cuenta
 * que ya está EN_ELIMINACION no se vuelve a pedir (continuación de una operación ya autorizada).
 * Nunca se guarda, se registra en logs ni se devuelve.
 */
public record EliminarCuentaRequest(
        @Size(max = 72, message = "La contraseña tiene como máximo 72 caracteres") String password
) {
    /** Evita que la contraseña aparezca si alguien imprime el objeto en un log. */
    @Override
    // Ejecuta to string como parte de las responsabilidades de esta clase.
    public String toString() {
        return "EliminarCuentaRequest[password=***]";
    }
}
