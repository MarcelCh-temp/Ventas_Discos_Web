package com.ventasdiscos.mspostsale.service;

import com.ventasdiscos.mspostsale.client.NotificationsClient;
import com.ventasdiscos.mspostsale.client.OrdersClient;
import com.ventasdiscos.mspostsale.client.OrdersDtos.PedidoOrders;
import com.ventasdiscos.mspostsale.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Clase original: operaciones compartidas por los Services de PostSale.
 * Lote 4: el pedido se verifica con el contrato interno de Orders (userId e items[].productId) y un pedido AJENO
 * responde 404, igual que en Orders (no se revela que existe). Ya no arma respuestas con Map (ahora hay DTO).
 */
@Component
class PostSaleSupport {

    private static final Logger log = LoggerFactory.getLogger(PostSaleSupport.class);

    private final OrdersClient orders;
    private final NotificationsClient notifications;

    PostSaleSupport(OrdersClient orders, NotificationsClient notifications) {
        this.orders = orders;
        this.notifications = notifications;
    }

    /** RF-POS-01/02: el pedido debe existir y ser del Cliente del token; si no, 404 (no se distingue el motivo). */
    PedidoOrders pedidoDelCliente(Long pedidoId, Long usuarioId) {
        PedidoOrders p = orders.obtener(pedidoId);
        if (p.userId() == null || !p.userId().equals(usuarioId)) {
            throw new RecursoNoEncontradoException("Pedido no encontrado");
        }
        return p;
    }

    /**
     * RNF-14: el aviso sale DESPUÉS del commit (si la transacción falla no se avisa nada). Si Notifications falla,
     * la solicitud ya quedó guardada: el cliente reintenta y luego solo deja registro en el log.
     */
    void notificarTrasCommit(String eventoId, Long usuarioId, String tipo, String titulo, String mensaje, Long referenciaId) {
        Runnable envio = () -> {
            try {
                notifications.enviar(eventoId, usuarioId, tipo, titulo, mensaje, referenciaId);
            } catch (RuntimeException ex) {
                log.error("No se pudo entregar el evento {} a Notifications", eventoId, ex);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            envio.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                envio.run();
            }
        });
    }
}
