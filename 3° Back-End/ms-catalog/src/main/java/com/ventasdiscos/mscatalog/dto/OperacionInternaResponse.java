package com.ventasdiscos.mscatalog.dto;

import java.util.List;

// Lote 2: trasladado desde ms-inventory (ERS 11.4) sin cambios de contrato.
/** yaAplicada = true cuando la operación se había procesado antes (respuesta idempotente). */
public record OperacionInternaResponse(
        String operacionId,
        boolean yaAplicada,
        List<MovimientoResponse> movimientos
) {
}
