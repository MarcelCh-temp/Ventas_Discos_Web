package com.ventasdiscos.msaccounts.security;

import com.ventasdiscos.msaccounts.exception.AccesoDenegadoException;
import com.ventasdiscos.msaccounts.model.Role;

/** Identidad del usuario autenticado, obtenida del token (nunca del body/URL). */
public record AuthUser(Long userId, Role rol) {

    /** 403 si no es Administrador (RN-04). */
    public void requireAdmin() {
        if (rol != Role.ADMINISTRADOR) {
            throw new AccesoDenegadoException("No tienes permisos para realizar esta acción");
        }
    }
}
