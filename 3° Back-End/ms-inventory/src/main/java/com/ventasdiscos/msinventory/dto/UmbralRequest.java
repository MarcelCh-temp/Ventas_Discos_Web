package com.ventasdiscos.msinventory.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** HU-42: umbral de stock crítico, entero no negativo. */
public record UmbralRequest(
        @NotNull(message = "El umbral es obligatorio")
        @Digits(integer = 9, fraction = 0, message = "El umbral debe ser un número entero")
        @PositiveOrZero(message = "El umbral no puede ser negativo") BigDecimal umbralCritico
) {
}
