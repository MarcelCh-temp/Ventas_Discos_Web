package com.ventasdiscos.msinventory.dto;

import com.ventasdiscos.msinventory.model.TipoMovimiento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** HU-44: una fila de la operación masiva. */
public record OperacionMasivaItem(
        @NotNull(message = "El producto es obligatorio") Long productId,
        @NotNull(message = "El tipo es obligatorio") TipoMovimiento tipo,
        @NotNull(message = "La cantidad es obligatoria")
        @Digits(integer = 9, fraction = 0, message = "La cantidad debe ser un número entero")
        @PositiveOrZero(message = "La cantidad no puede ser negativa") BigDecimal cantidad,
        @Size(max = 255, message = "Máximo 255 caracteres") String motivo
) {
}
