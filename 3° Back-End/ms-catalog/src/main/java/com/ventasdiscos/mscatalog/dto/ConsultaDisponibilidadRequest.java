package com.ventasdiscos.mscatalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Lote 2 (ERS 12, POST /api/v1/inventory/availability/query): consulta en UNA llamada si hay stock
 * suficiente para varias líneas (carrito y revalidación previa al checkout, RN-13). No descuenta nada.
 */
public record ConsultaDisponibilidadRequest(
        @NotEmpty(message = "Debes indicar al menos un producto")
        @Size(max = 200, message = "Máximo 200 productos por consulta")
        List<@Valid Item> items
) {
    public record Item(
            @NotNull(message = "El producto es obligatorio") Long productId,
            @NotNull(message = "La cantidad es obligatoria")
            @Positive(message = "La cantidad debe ser mayor que cero") Integer cantidad
    ) {
    }
}
