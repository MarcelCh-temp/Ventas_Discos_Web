package com.ventasdiscos.msaccounts.dto;

import com.ventasdiscos.msaccounts.model.EstadoCuenta;
import com.ventasdiscos.msaccounts.model.Usuario;

import java.time.LocalDateTime;

/**
 * Lote 5 · HU-78: resultado de DELETE /api/v1/accounts/me. 200 = ELIMINADA; 202 = EN_ELIMINACION (la cuenta ya quedó
 * bloqueada y la depuración pendiente se completará con reintentos). Sin datos personales.
 */
public record EliminacionCuentaResponse(Long userId, EstadoCuenta estado, LocalDateTime fechaSolicitudEliminacion,
                                        LocalDateTime fechaEliminacion, String mensaje) {

    // Ejecuta from como parte de las responsabilidades de esta clase.
    public static EliminacionCuentaResponse from(Usuario u, String mensaje) {
        return new EliminacionCuentaResponse(u.getId(), u.getEstado(), u.getFechaSolicitudEliminacion(),
                u.getFechaEliminacion(), mensaje);
    }
}
