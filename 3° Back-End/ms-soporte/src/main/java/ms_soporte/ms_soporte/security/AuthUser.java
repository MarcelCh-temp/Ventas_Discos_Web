package ms_soporte.ms_soporte.security;

import ms_soporte.ms_soporte.exception.AccesoDenegadoException;

/**
 * Lote 1: identidad del usuario autenticado, sacada del token que emite MS-Accounts.
 * Mismo record que usan ms-accounts y ms-catalog.
 */
public record AuthUser(Long userId, String rol) {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    public boolean esAdministrador() {
        return ADMINISTRADOR.equals(rol);
    }

    /** 403 si no es Administrador (HU-63 a HU-67 son acciones del Administrador). */
    public void requireAdmin() {
        if (!esAdministrador()) {
            throw new AccesoDenegadoException("No tienes permisos para realizar esta acción");
        }
    }
}
