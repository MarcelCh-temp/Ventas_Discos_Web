package com.ventasdiscos.msnotifications.dto;

/**
 * Lote 5 · Contrato INTERNO para Accounts (HU-78, RN-43, ERS 13.8): notificaciones personales borradas en ESTA
 * llamada. Idempotente: repetirla responde eliminados = 0.
 */
public record DepuracionResponse(Long userId, int eliminados) {
}
