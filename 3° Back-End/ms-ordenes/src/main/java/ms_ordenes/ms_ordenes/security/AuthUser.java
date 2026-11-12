package ms_ordenes.ms_ordenes.security;

import ms_ordenes.ms_ordenes.exception.AccesoDenegadoException;

/**
 * Lote 3 (copia de ms-soporte, Lote 1): identidad del usuario autenticado, sacada del token que emite MS-Accounts.
 * Mismo record que usan ms-accounts y ms-catalog.
 */
public record AuthUser(Long userId, String rol) {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    public boolean esAdministrador() {
        return ADMINISTRADOR.equals(rol);
    }

    /** 403 si no es Administrador (HU-26 a HU-33 son acciones del Administrador). */
    public void requireAdmin() {
        if (!esAdministrador()) {
            throw new AccesoDenegadoException("No tienes permisos para realizar esta acción");
        }
    }
}
