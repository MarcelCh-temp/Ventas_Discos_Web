package com.ventasdiscos.mspostsale.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.util.*;

@Component
public class InventoryClient {
    private final RestClient client;

    public InventoryClient(@Value("${services.inventory.url}") String url) {
        client=RestClient.builder().baseUrl(url).build();
    }

    public void reponer(String operacionId,String motivo,Long productoId,Integer cantidad) {
        Map<String,Object> item=Map.of("productId",productoId,"tipo","ENTRADA","cantidad",cantidad);
        Map<String,Object> data=Map.of("operacionId",operacionId,"motivo",motivo,"items",List.of(item));
        try {
            client.post().uri("/internal/inventory/operations").contentType(MediaType.APPLICATION_JSON).body(data).retrieve().toBodilessEntity();
        }
        catch(RestClientException e) {
            throw new IllegalStateException("No fue posible confirmar la reposicion con Inventory",e);
        }
    }
}
