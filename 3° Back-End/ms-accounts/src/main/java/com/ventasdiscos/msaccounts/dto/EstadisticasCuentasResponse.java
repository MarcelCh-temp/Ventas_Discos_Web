package com.ventasdiscos.msaccounts.dto;

/** HU-45: dato informativo para el Dashboard (cantidad de usuarios). No es administración de cuentas (R-03). */
// Ejecuta estadisticas cuentas response como parte de las responsabilidades de esta clase.
public record EstadisticasCuentasResponse(long totalUsuarios, long totalClientes) {
}
