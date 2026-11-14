package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.ItemPedido;

import java.math.BigDecimal;

/** RF-MIP-02 / RF-PED-03: precios HISTÓRICOS (snapshot); los cambios del catálogo no los alteran. */
public record ItemPedidoResponse(Long productId, String nombreProducto, BigDecimal precioUnitario, int cantidad,
                                 BigDecimal subtotal) {

    // Ejecuta from como parte de las responsabilidades de esta clase.
    public static ItemPedidoResponse from(ItemPedido i) {
        return new ItemPedidoResponse(i.getProductId(), i.getNombreProductoSnapshot(), i.getPrecioHistorico(),
                i.getCantidad(), i.getSubtotalHistorico());
    }
}
