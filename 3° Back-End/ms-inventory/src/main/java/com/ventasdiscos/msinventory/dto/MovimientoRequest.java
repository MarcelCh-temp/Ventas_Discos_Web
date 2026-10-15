package com.ventasdiscos.msinventory.dto;

import com.ventasdiscos.msinventory.model.TipoMovimiento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * HU-41: entrada o salida de stock.
 * cantidad es BigDecimal + @Digits(fraction = 0) para RECHAZAR decimales (RN-11)
 * en vez de que el JSON 2.5 se trunque silenciosamente a 2.
 */
public record MovimientoRequest(
        @NotNull(message = "El tipo es obligatorio (ENTRADA o SALIDA)") TipoMovimiento tipo,
        @NotNull(message = "La cantidad es obligatoria")
        @Digits(integer = 9, fraction = 0, message = "La cantidad debe ser un número entero")
        @Positive(message = "La cantidad debe ser mayor que cero") BigDecimal cantidad,
        @Size(max = 255, message = "Máximo 255 caracteres") String motivo
) {
}
