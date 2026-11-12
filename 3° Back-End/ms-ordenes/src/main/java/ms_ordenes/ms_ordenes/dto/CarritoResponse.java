package ms_ordenes.ms_ordenes.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * HU-19 a HU-21. total lo calcula el backend. listoParaComprar = no está vacío, todo es comprable y hay stock.
 * preciosActualizados = false si Catalog no respondió y se muestran los últimos precios guardados.
 */
public record CarritoResponse(List<ItemCarritoResponse> items, int cantidadProductos, int unidades,
                              BigDecimal subtotal, BigDecimal descuento, BigDecimal total, boolean vacio,
                              boolean listoParaComprar, boolean preciosActualizados, List<String> avisos) {
}
