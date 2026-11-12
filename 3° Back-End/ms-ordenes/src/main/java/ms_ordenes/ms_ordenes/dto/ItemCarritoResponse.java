package ms_ordenes.ms_ordenes.dto;

import java.math.BigDecimal;

/**
 * HU-19: una línea del carrito o del resumen. Precios en pesos según Catalog en ese momento.
 * disponibilidad: DISPONIBLE, STOCK_CRITICO, AGOTADO (null si Catalog no respondió).
 */
public record ItemCarritoResponse(Long productId, String nombre, int cantidad, BigDecimal precioUnitario,
                                  BigDecimal descuentoUnitario, BigDecimal precioFinalUnitario, BigDecimal subtotal,
                                  String disponibilidad, Boolean haySuficiente, Boolean comprable, String motivo) {
}
