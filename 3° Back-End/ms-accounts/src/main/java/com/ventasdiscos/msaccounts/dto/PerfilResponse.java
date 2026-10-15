package com.ventasdiscos.msaccounts.dto;

import com.ventasdiscos.msaccounts.model.Role;
import com.ventasdiscos.msaccounts.model.Usuario;

/** HU-04: lo que se MUESTRA del usuario. Nunca incluye passwordHash. */
public record PerfilResponse(
        Long id,
        String rut,
        String nombres,
        String apellidos,
        String correo,
        Role rol
) {
    public static PerfilResponse from(Usuario u) {
        return new PerfilResponse(u.getId(), u.getRut(), u.getNombres(), u.getApellidos(), u.getCorreo(), u.getRol());
    }
}
