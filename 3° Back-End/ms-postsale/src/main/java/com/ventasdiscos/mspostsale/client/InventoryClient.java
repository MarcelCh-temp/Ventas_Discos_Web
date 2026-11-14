package com.ventasdiscos.mspostsale.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Clase original conservada (mismo nombre: sigue llamando a las operaciones de INVENTARIO). Lote 4: el inventario
 * vive en MS-Catalog desde el Lote 2, así que la URL pasa de MS-Inventory (8083, /internal/inventory/operations) a
 * Catalog (8082, /api/v1/internal/inventory/operations). Mismo JSON (CONTRATOS_API_LOTE_2 §3).
 * operacionId = "POSTSALE-RECEPCION-{idItem}" (formato original; prefijo POSTSALE- obligatorio por DOC-08). Es la
 * clave de idempotencia: reintentar con la misma no repone dos veces.
 * Reintentos limitados ante conexión o 5xx (patrón de los Lotes 1 y 3); ante 4xx no se reintenta.
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);
    // Constante MAX_INTENTOS: valor compartido que no cambia durante la ejecución.
    static final int MAX_INTENTOS = 2;
    // Constante PAUSA_ENTRE_INTENTOS_MS: valor compartido que no cambia durante la ejecución.
    private static final long PAUSA_ENTRE_INTENTOS_MS = 300;

    /** APLICADA: 201 o 200 (ya estaba). RECHAZADA: 4xx (no se reintenta). NO_CONFIRMADA: sin respuesta o 5xx. */
    public enum Resultado {
        APLICADA, RECHAZADA, NO_CONFIRMADA
    }

    // Cliente de integración: realiza respuesta con otro microservicio y trata su respuesta.
    public record Respuesta(Resultado resultado, int status, String detalle) {
    }

    // Atributo restTemplate: dato o dependencia utilizada por esta clase.
    private final RestTemplate restTemplate;
    // Atributo baseUrl: dato o dependencia utilizada por esta clase.
    private final String baseUrl;

    // Cliente de integración: realiza inventory client con otro microservicio y trata su respuesta.
    public InventoryClient(RestTemplate restTemplate, @Value("${services.catalog.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /** ENTRADA de unidades devueltas aptas (RF-POS-09, RN-25). */
    // Cliente de integración: realiza reponer con otro microservicio y trata su respuesta.
    public Respuesta reponer(String operacionId, String motivo, Long productoId, Integer cantidad) {
        Map<String, Object> item = Map.of("productId", productoId, "tipo", "ENTRADA", "cantidad", cantidad);
        Map<String, Object> body = Map.of("operacionId", operacionId, "motivo", motivo, "items", List.of(item));
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                int status = restTemplate.postForEntity(baseUrl + "/api/v1/internal/inventory/operations", body,
                        String.class).getStatusCode().value();
                log.info("Reposición {} aplicada en Catalog ({}, intento {})", operacionId, status, intento);
                return new Respuesta(Resultado.APLICADA, status, null);
            } catch (HttpClientErrorException e) {
                int status = e.getStatusCode().value();
                log.warn("Catalog RECHAZÓ la reposición {} ({}): {}", operacionId, status, e.getResponseBodyAsString());
                return new Respuesta(Resultado.RECHAZADA, status, e.getResponseBodyAsString());
            } catch (RestClientException e) {
                if (intento < MAX_INTENTOS) {
                    esperar();
                } else {
                    log.warn("Reposición {} NO CONFIRMADA tras {} intentos: {}", operacionId, MAX_INTENTOS, e.getMessage());
                }
            }
        }
        return new Respuesta(Resultado.NO_CONFIRMADA, 0, null);
    }

    // Comunicación con otro microservicio; verificar autenticación y respuesta del destino. Método esperar (esperar).
    private static void esperar() {
        try {
            Thread.sleep(PAUSA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
