package com.ventasdiscos.mspostsale.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lote 4: respuestas de los contratos INTERNOS de Orders (Lote 3, CONTRATOS_API_LOTE_3 §5.3). Solo los campos que
 * PostSale usa; si Orders agrega campos, se ignoran. El estado llega como texto (EstadoOrden de Orders).
 */
public final class OrdersDtos {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String CONFIRMADO = "CONFIRMADO";
    public static final String PREPARANDO = "PREPARANDO";
    public static final String ENTREGADO = "ENTREGADO";
    public static final String CANCELADO = "CANCELADO";

    /** RN-18: estados en que todavía se puede pedir la cancelación (antes del despacho). */
    public static final List<String> CANCELABLES = List.of(PENDIENTE, CONFIRMADO, PREPARANDO);

    private OrdersDtos() {
    }

    /** GET /api/v1/internal/orders/{id}. */
    public record PedidoOrders(Long id, String numeroPedido, Long userId, String estado, LocalDateTime fechaCompra,
                               BigDecimal total, List<ItemPedido> items) {

        public List<ItemPedido> lineas() {
            return items == null ? List.of() : items;
        }

        /** Unidades compradas de ese producto (RN-22). */
        public int cantidadComprada(Long productoId) {
            return lineas().stream().filter(i -> productoId.equals(i.productId())).mapToInt(ItemPedido::cantidad).sum();
        }

        /** Texto para los mensajes: número de pedido o, en pedidos antiguos sin número, su id. */
        public String referencia() {
            return numeroPedido != null ? numeroPedido : "#" + id;
        }
    }

    public record ItemPedido(Long productId, String nombreProducto, int cantidad) {
    }

    /** POST /api/v1/internal/orders/{id}/cancel. */
    public record CancelacionOrders(Long pedidoId, String numeroPedido, String estado, boolean yaCancelado,
                                    boolean reposicionStockPendiente) {

        public String referencia() {
            return numeroPedido != null ? numeroPedido : "#" + pedidoId;
        }
    }
}
