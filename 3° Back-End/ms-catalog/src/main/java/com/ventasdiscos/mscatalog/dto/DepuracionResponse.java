package com.ventasdiscos.mscatalog.dto;

/**
 * Lote 5 · Contrato INTERNO para Accounts (HU-78, RN-43, ERS 13.8): favoritos borrados en ESTA llamada.
 * Idempotente: repetirla responde eliminados = 0 y no cambia nada más.
 */
public record DepuracionResponse(Long userId, int eliminados) {
}
