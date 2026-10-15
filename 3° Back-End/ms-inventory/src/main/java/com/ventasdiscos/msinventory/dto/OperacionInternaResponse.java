package com.ventasdiscos.msinventory.dto;

import java.util.List;

/** yaAplicada = true cuando la operación se había procesado antes (respuesta idempotente). */
public record OperacionInternaResponse(
        String operacionId,
        boolean yaAplicada,
        List<MovimientoResponse> movimientos
) {
}
