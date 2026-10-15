package com.ventasdiscos.mspostsale.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class OrdersClient {
    private final RestClient client;

    public OrdersClient(@Value("${services.orders.url}") String url) {
        client=RestClient.builder().baseUrl(url).build();
    }

    public JsonNode obtener(Long id) {
        try {
            return client.get().uri("/api/ordenes/{id}",id).retrieve().body(JsonNode.class);
        }
        catch(RestClientException e) {
            throw new IllegalStateException("No se pudo verificar el pedido en Orders",e);
        }
    }

    public JsonNode cambiarEstado(Long id,String estado) {
        try {
            return client.patch().uri(b->b.path("/api/ordenes/{id}/estado").queryParam("estado",estado).build(id)).retrieve().body(JsonNode.class);
        }
        catch(RestClientException e) {
            throw new IllegalStateException("No se pudo actualizar el estado del pedido en Orders",e);
        }
    }
}
