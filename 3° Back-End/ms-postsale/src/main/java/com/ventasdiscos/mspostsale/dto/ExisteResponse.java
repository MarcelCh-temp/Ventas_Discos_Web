package com.ventasdiscos.mspostsale.dto;

/** Lote 5 · Contrato INTERNO para Accounts (HU-78, ERS 13.8): ¿el usuario tiene solicitudes de postventa PENDIENTE? */
// Ejecuta existe response como parte de las responsabilidades de esta clase.
public record ExisteResponse(Long userId, boolean existe) {
}
