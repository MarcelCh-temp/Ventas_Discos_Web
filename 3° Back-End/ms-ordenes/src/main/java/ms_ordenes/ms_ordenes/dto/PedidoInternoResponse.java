package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.Orden;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato INTERNO Orders -> PostSale (Lote 4): lo mínimo para validar una cancelación o devolución
 * (dueño, estado, productos y cantidades compradas; RN-22, RN-24). No es para el frontend.
 */
public record PedidoInternoResponse(Long id, String numeroPedido, Long userId, EstadoOrden estado,
                                    LocalDateTime fechaCompra, BigDecimal total, List<Item> items) {

    // Ejecuta item como parte de las responsabilidades de esta clase.
    public record Item(Long productId, String nombreProducto, int cantidad) {
    }

    // Ejecuta from como parte de las responsabilidades de esta clase.
    public static PedidoInternoResponse from(Orden o) {
        return new PedidoInternoResponse(o.getId(), o.getNumeroPedido(), o.getIdUsuario(), o.getEstado(),
                o.getFechaCreacion(), o.getTotal(),
                o.getItems().stream().map(i -> new Item(i.getProductId(), i.getNombreProductoSnapshot(), i.getCantidad()))
                        .toList());
    }
}
