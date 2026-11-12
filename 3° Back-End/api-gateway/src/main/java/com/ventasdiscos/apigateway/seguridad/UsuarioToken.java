package com.ventasdiscos.apigateway.seguridad;

/** Lote 6: datos del JWT de usuario que necesita el Gateway (solo para la primera capa de control). */
public record UsuarioToken(Long userId, String rol) {

    public boolean esAdministrador() {
        return "ADMINISTRADOR".equals(rol);
    }
}
