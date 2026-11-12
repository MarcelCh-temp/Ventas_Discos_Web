package com.ventasdiscos.msaccounts.dto;

import com.ventasdiscos.msaccounts.model.EstadoCuenta;
import com.ventasdiscos.msaccounts.model.Usuario;

/**
 * Lote 5 · Contrato INTERNO (ERS 13.8, RN-37, RN-44): ¿esta cuenta puede hacer nuevas operaciones?
 * estado: ACTIVA (también una cuenta anterior al Lote 5 con estado NULL), EN_ELIMINACION, ELIMINADA o null si el
 * valor guardado no es reconocido. puedeOperar = true SOLO si es ACTIVA. Sin datos personales.
 */
public record EstadoOperacionResponse(Long userId, EstadoCuenta estado, boolean puedeOperar) {

    public static EstadoOperacionResponse from(Usuario u) {
        return new EstadoOperacionResponse(u.getId(), u.getEstado(), u.puedeOperar());
    }
}
