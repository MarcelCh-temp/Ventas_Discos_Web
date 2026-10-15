package com.ventasdiscos.mspostsale.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.util.*;

@Component
public class NotificationsClient {
    private final RestClient client;

    public NotificationsClient(@Value("${services.notifications.url}") String url) {
        client=RestClient.builder().baseUrl(url).build();
    }

    public void enviar(String eventoId,Long usuarioId,String tipo,String titulo,String mensaje,Long referenciaId) {
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("eventoId",eventoId);
        body.put("origen","POSTSALE");
        body.put("usuarioId",usuarioId);
        body.put("tipo",tipo);
        body.put("titulo",titulo);
        body.put("mensaje",mensaje);
        body.put("referenciaId",referenciaId);
        try {
            client.post().uri("/api/v1/internal/notifications/events").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        }
        catch(RestClientException ex) {
            throw new IllegalStateException("No se pudo informar el evento a Notifications",ex);
        }
    }
}
