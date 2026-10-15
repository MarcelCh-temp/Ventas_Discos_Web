package com.ventasdiscos.msinventory.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** HU-40: fija el stock al valor contado físicamente; el motivo es obligatorio. */
public record AjusteRequest(
        @NotNull(message = "El nuevo stock es obligatorio")
        @Digits(integer = 9, fraction = 0, message = "El stock debe ser un número entero")
        @PositiveOrZero(message = "El stock no puede ser negativo") BigDecimal nuevoStock,
        @NotBlank(message = "El motivo del ajuste es obligatorio")
        @Size(max = 255, message = "Máximo 255 caracteres") String motivo
) {
}
