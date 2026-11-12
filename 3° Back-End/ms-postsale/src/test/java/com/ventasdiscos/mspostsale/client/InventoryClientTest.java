package com.ventasdiscos.mspostsale.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lote 4: PostSale -> Catalog (reposición de devoluciones). RestTemplate simulado: no se levanta Catalog. */
class InventoryClientTest {

    private static final String OPS = "http://127.0.0.1:8082/api/v1/internal/inventory/operations";
    private RestTemplate restTemplate;
    private InventoryClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new InventoryClient(restTemplate, "http://127.0.0.1:8082");
    }

    @Test
    @SuppressWarnings("unchecked")
    void reponeEnCatalogConElPrefijoPostsaleYUnaEntrada() {
        when(restTemplate.postForEntity(eq(OPS), any(), eq(String.class))).thenReturn(ResponseEntity.status(201).body("{}"));
        InventoryClient.Respuesta r = client.reponer("POSTSALE-RECEPCION-9", "Devolución", 2L, 3);
        assertEquals(InventoryClient.Resultado.APLICADA, r.resultado());
        ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
        verify(restTemplate).postForEntity(eq(OPS), cuerpo.capture(), eq(String.class));
        Map<String, Object> enviado = (Map<String, Object>) cuerpo.getValue();
        assertEquals("POSTSALE-RECEPCION-9", enviado.get("operacionId"));
        Map<String, Object> item = ((List<Map<String, Object>>) enviado.get("items")).get(0);
        assertEquals("ENTRADA", item.get("tipo"));
        assertEquals(2L, item.get("productId"));
        assertEquals(3, item.get("cantidad"));
    }

    @Test
    void yaAplicada200TambienCuentaComoAplicada() {
        when(restTemplate.postForEntity(eq(OPS), any(), eq(String.class))).thenReturn(ResponseEntity.ok("{}"));
        assertEquals(InventoryClient.Resultado.APLICADA, client.reponer("POSTSALE-RECEPCION-9", "x", 2L, 1).resultado());
    }

    @Test
    void rechazo4xxNoSeReintenta() {
        when(restTemplate.postForEntity(eq(OPS), any(), eq(String.class))).thenThrow(HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(), "{}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8));
        InventoryClient.Respuesta r = client.reponer("POSTSALE-RECEPCION-9", "x", 2L, 1);
        assertEquals(InventoryClient.Resultado.RECHAZADA, r.resultado());
        assertEquals(404, r.status());
        verify(restTemplate, times(1)).postForEntity(eq(OPS), any(), eq(String.class));
    }

    @Test
    void sinRespuestaSeReintentaYQuedaNoConfirmada() {
        when(restTemplate.postForEntity(eq(OPS), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
        assertEquals(InventoryClient.Resultado.NO_CONFIRMADA, client.reponer("POSTSALE-RECEPCION-9", "x", 2L, 1).resultado());
        verify(restTemplate, times(InventoryClient.MAX_INTENTOS)).postForEntity(eq(OPS), any(), eq(String.class));
    }
}
