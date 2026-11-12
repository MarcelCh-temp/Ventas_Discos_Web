package ms_ordenes.ms_ordenes.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * HU-24 (RF-COM-07): resumen recalculado en el backend. listo = se puede pagar.
 * problemas: clave "items[productId]" (o "carrito") -> qué corregir antes de pagar.
 */
public record ResumenCompraResponse(List<ItemCarritoResponse> items, BigDecimal subtotal, BigDecimal descuento,
                                    BigDecimal total, DatosEntregaResponse datosEntrega, boolean listo,
                                    Map<String, String> problemas) {
}
