package com.ventasdiscos.msaccounts.dto;

import com.ventasdiscos.msaccounts.model.Role;

/** HU-02: token + rol para que el frontend dirija a la interfaz Cliente o Admin. */
public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        Long userId,
        String nombres,
        Role rol
) {
}
