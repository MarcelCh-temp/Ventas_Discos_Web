package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.TipoMovimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Lote 2: trasladado desde ms-inventory (ERS 11.4) sin cambios de contrato.
/** Línea de una operación pedida por otro MS (Orders descuenta / PostSale repone). */
public record OperacionInternaItem(
        @NotNull Long productId,
        @NotNull TipoMovimiento tipo,
        @NotNull @Positive Integer cantidad
) {
}
