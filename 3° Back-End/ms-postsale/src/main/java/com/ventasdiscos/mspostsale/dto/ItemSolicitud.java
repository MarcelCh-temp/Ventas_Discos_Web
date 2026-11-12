package com.ventasdiscos.mspostsale.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Clase original. RF-POS-02: la cantidad debe ser un ENTERO mayor que cero. Lote 4: se recibe como BigDecimal +
 * {@code @Digits(fraction = 0)} (igual que el carrito del Lote 3) porque con Integer el lector JSON convertiría 1.5 en
 * 1 sin avisar; así un decimal responde 400. Texto, vacío o negativo también responden 400.
 */
public record ItemSolicitud(
        @NotNull(message = "El producto es obligatorio") @Positive(message = "El producto debe ser positivo") Long productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        @Digits(integer = 6, fraction = 0, message = "La cantidad debe ser un número entero")
        BigDecimal cantidadSolicitada
) {
}
