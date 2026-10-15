package com.ventasdiscos.mscatalog.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Precio comercial del carrito con promociones vigentes aplicadas.
 * Orders lo usa para el resumen y el snapshot del pedido; el STOCK lo valida aparte con Inventory.
 */
public record PricingResponse(
        List<Linea> lineas,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal total,
        boolean todosComprables
) {
    public record Linea(
            Long productId,
            String nombre,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuentoUnitario,
            BigDecimal precioFinalUnitario,
            BigDecimal subtotal,
            Long promocionId,
            boolean comprable,
            String motivo
    ) {
    }
}
