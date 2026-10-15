package com.ventasdiscos.msinventory.security;

import com.ventasdiscos.msinventory.exception.AccesoDenegadoException;

/** Identidad del usuario autenticado, obtenida del token emitido por MS-Accounts. */
public record AuthUser(Long userId, String rol) {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    public boolean esAdministrador() {
        return ADMINISTRADOR.equals(rol);
    }

    /** 403 si no es Administrador (RN-04). */
    public void requireAdmin() {
        if (!esAdministrador()) {
            throw new AccesoDenegadoException("No tienes permisos para realizar esta acción");
        }
    }
}
