package com.ventasdiscos.mspostsale.security;

import com.ventasdiscos.mspostsale.exception.AccesoDenegadoException;

/**
 * Lote 4 (copia de ms-ordenes, Lote 3): identidad del usuario autenticado, sacada del token que emite MS-Accounts.
 * Mismo record que usan ms-accounts y ms-catalog.
 */
public record AuthUser(Long userId, String rol) {

    // Constante ADMINISTRADOR: valor compartido que no cambia durante la ejecución.
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    // Ejecuta es administrador como parte de las responsabilidades de esta clase.
    public boolean esAdministrador() {
        return ADMINISTRADOR.equals(rol);
    }

    /** 403 si no es Administrador (HU-72 a HU-76 son acciones del Administrador). */
    // Ejecuta require admin como parte de las responsabilidades de esta clase.
    public void requireAdmin() {
        if (!esAdministrador()) {
            throw new AccesoDenegadoException("No tienes permisos para realizar esta acción");
        }
    }
}
