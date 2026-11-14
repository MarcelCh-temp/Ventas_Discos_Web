package com.ventasdiscos.msaccounts.dto;

import com.ventasdiscos.msaccounts.model.EstadoCuenta;
import com.ventasdiscos.msaccounts.model.Usuario;

/**
 * Cierre del backend · RF-PED-01, RF-PED-02 y RF-SOP-05 (decisión de cierre 3/4): lo mínimo para que el administrador
 * identifique al Cliente de un pedido o ticket a partir de su userId. Sin RUT, contraseña ni hash.
 * Las cuentas ELIMINADA se devuelven con sus datos ya anonimizados (Lote 5) y su estado real: no se reconstruye nada.
 * estado es null si el valor guardado no es reconocido.
 */
public record CuentaAdminResponse(Long id, String nombres, String apellidos, String correo, EstadoCuenta estado) {

    // Ejecuta from como parte de las responsabilidades de esta clase.
    public static CuentaAdminResponse from(Usuario u) {
        return new CuentaAdminResponse(u.getId(), u.getNombres(), u.getApellidos(), u.getCorreo(), u.getEstado());
    }
}
