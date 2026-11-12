package com.ventasdiscos.mspostsale.dto;

import com.ventasdiscos.mspostsale.model.CondicionProducto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Clase original. RF-POS-09. Lote 4: cantidad entera como en ItemSolicitud (un decimal responde 400). */
public record ItemRecibido(
        @NotNull(message = "El producto de la devolución es obligatorio") @Positive Long itemDevolucionId,
        @NotNull(message = "La cantidad recibida es obligatoria")
        @Positive(message = "La cantidad recibida debe ser mayor que cero")
        @Digits(integer = 6, fraction = 0, message = "La cantidad recibida debe ser un número entero")
        BigDecimal cantidadRecibida,
        @NotNull(message = "La condición es obligatoria (BUENA o DANADA)") CondicionProducto condicion
) {
}
