package com.ventasdiscos.msinventory.dto;

import com.ventasdiscos.msinventory.model.TipoMovimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Línea de una operación pedida por otro MS (Orders descuenta / PostSale repone). */
public record OperacionInternaItem(
        @NotNull Long productId,
        @NotNull TipoMovimiento tipo,
        @NotNull @Positive Integer cantidad
) {
}
