package com.ventasdiscos.apigateway.seguridad;

/** Lote 6: datos del JWT de usuario que necesita el Gateway (solo para la primera capa de control). */
// Ejecuta usuario token como parte de las responsabilidades de esta clase.
public record UsuarioToken(Long userId, String rol) {

    // Ejecuta es administrador como parte de las responsabilidades de esta clase.
    public boolean esAdministrador() {
        return "ADMINISTRADOR".equals(rol);
    }
}
