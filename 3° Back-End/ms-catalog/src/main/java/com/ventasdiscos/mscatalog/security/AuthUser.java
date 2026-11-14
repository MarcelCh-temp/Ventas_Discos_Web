package com.ventasdiscos.mscatalog.security;

import com.ventasdiscos.mscatalog.exception.AccesoDenegadoException;

/** Identidad del usuario autenticado, obtenida del token emitido por MS-Accounts. */
// Ejecuta auth user como parte de las responsabilidades de esta clase.
public record AuthUser(Long userId, String rol) {

    // Constante ADMINISTRADOR: valor compartido que no cambia durante la ejecución.
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    // Ejecuta es administrador como parte de las responsabilidades de esta clase.
    public boolean esAdministrador() {
        return ADMINISTRADOR.equals(rol);
    }

    /** 403 si no es Administrador (RN-04). */
    // Ejecuta require admin como parte de las responsabilidades de esta clase.
    public void requireAdmin() {
        if (!esAdministrador()) {
            throw new AccesoDenegadoException("No tienes permisos para realizar esta acción");
        }
    }
}
