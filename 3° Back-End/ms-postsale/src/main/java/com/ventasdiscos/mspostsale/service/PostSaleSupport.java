package com.ventasdiscos.mspostsale.service;

import com.ventasdiscos.mspostsale.client.*;
import com.ventasdiscos.mspostsale.dto.*;
import com.ventasdiscos.mspostsale.model.*;
import com.ventasdiscos.mspostsale.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.TransactionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class PostSaleSupport {
    private static final Logger log = LoggerFactory.getLogger(PostSaleSupport.class);
    private final OrdersClient orders;
    private final NotificationsClient notifications;
    PostSaleSupport(OrdersClient orders, NotificationsClient notifications) {
        this.orders = orders;
        this.notifications = notifications;
    }
    void notificarTrasCommit(String eventoId, Long usuarioId, String tipo,
    String titulo, String mensaje, Long referenciaId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    notifications.enviar(eventoId, usuarioId, tipo, titulo, mensaje, referenciaId);
                }
                catch (RuntimeException ex) {
                    log.error("No se pudo entregar el evento {} a Notifications", eventoId, ex);
                }
            }
        }
        );
    }
    JsonNode verificarPedido(Long pedidoId, Long usuarioId) {
        JsonNode p = orders.obtener(pedidoId);
        if (p == null || !p.hasNonNull("idUsuario") || p.path("idUsuario").asLong() != usuarioId)
        throw new IllegalArgumentException("El pedido no corresponde al usuario indicado");
        return p;
    }
    String estado(JsonNode p) {
        return p.path("estado").asText("").toUpperCase(Locale.ROOT);
    }
    void validarCancelable(String estado) {
        if (!Set.of("PENDIENTE","PAGADA").contains(estado))
        throw new IllegalArgumentException("Solo es posible cancelar antes del despacho. Estado: "+estado);
    }
    Map<String,Object> resumen(SolicitudPostVenta s, String tipo, String estado) {
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("id",s.getId());
        m.put("pedidoId",s.getPedidoId());
        m.put("usuarioId",s.getUsuarioId());
        m.put("fechaSolicitud",s.getFechaSolicitud());
        m.put("tipo",tipo);
        m.put("estado",estado);
        return m;
    }
}
