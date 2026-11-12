package com.ventasdiscos.msnotifications.security;

/**
 * Lote 1: identidad del usuario autenticado, sacada del token que emite MS-Accounts.
 * Mismo record que usan ms-accounts y ms-catalog.
 */
public record AuthUser(Long userId, String rol) {
}
