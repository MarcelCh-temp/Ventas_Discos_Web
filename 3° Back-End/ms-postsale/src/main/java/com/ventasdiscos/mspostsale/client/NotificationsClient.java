package com.ventasdiscos.mspostsale.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase original conservada (mismo nombre y mismo contrato). Lote 4 (RF-POS-01/02/07/08/10, RNF-14): ahora con el
 * patrón aprobado en los Lotes 1 y 3 (copia del cliente de ms-ordenes): reintentos limitados (conexión o 5xx; ante
 * 4xx no se reintenta) y eventoId fijo por acción para que Notifications no duplique (RN-33). Antes no reintentaba.
 * Se llama DESPUÉS de confirmar la transacción: si Notifications falla, la solicitud ya está guardada (ERS 11.7).
 */
@Component
public class NotificationsClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationsClient.class);
    // Constante ORIGEN: valor compartido que no cambia durante la ejecución.
    private static final String ORIGEN = "POSTSALE";
    static final int MAX_INTENTOS = 2;                    // 1 envío + 1 reintento (ERS 12.7: número limitado)
    // Constante PAUSA_ENTRE_INTENTOS_MS: valor compartido que no cambia durante la ejecución.
    private static final long PAUSA_ENTRE_INTENTOS_MS = 500;

    // Atributo restTemplate: dato o dependencia utilizada por esta clase.
    private final RestTemplate restTemplate;
    // Atributo baseUrl: dato o dependencia utilizada por esta clase.
    private final String baseUrl;

    public NotificationsClient(RestTemplate restTemplate,
                                @Value("${services.notifications.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * tipo: CANCELACION_CREADA, CANCELACION_RESUELTA, DEVOLUCION_CREADA, DEVOLUCION_RESUELTA o REEMBOLSO_REGISTRADO
     * (enum TipoNotificacion de ms-notifications). referenciaId = id de la solicitud.
     */
    public void enviar(String eventoId, Long usuarioId, String tipo, String titulo, String mensaje, Long referenciaId) {
        Map<String, Object> evento = new HashMap<>();
        evento.put("eventoId", eventoId);
        evento.put("origen", ORIGEN);
        evento.put("usuarioId", usuarioId);
        evento.put("titulo", titulo);
        evento.put("mensaje", mensaje);
        evento.put("tipo", tipo);
        evento.put("referenciaId", referenciaId);
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                ResponseEntity<Void> r = restTemplate.postForEntity(
                        baseUrl + "/api/v1/internal/notifications/events", evento, Void.class);
                if (r.getStatusCode() == HttpStatus.CREATED) {
                    log.info("Aviso {} registrado en MS-Notifications (usuario {}, solicitud {}, intento {})",
                            eventoId, usuarioId, referenciaId, intento);
                } else {
                    // 200: Notifications ya tenía ese eventoId y no lo duplicó (RN-33)
                    log.info("Aviso {} ya estaba registrado en MS-Notifications; no se duplicó", eventoId);
                }
                return;
            } catch (HttpClientErrorException e) {
                // 4xx: Notifications rechazó los datos; repetir el mismo envío no lo arreglaría
                log.warn("AVISO RECHAZADO {} por MS-Notifications ({}): no se reintenta. La solicitud quedó guardada.",
                        eventoId, e.getStatusCode());
                return;
            } catch (RestClientException e) {
                if (intento < MAX_INTENTOS) {
                    log.info("Aviso {}: intento {} de {} falló ({}); se reintenta", eventoId, intento, MAX_INTENTOS, e.getMessage());
                    esperarAntesDeReintentar();
                } else {
                    log.warn("AVISO NO ENTREGADO {} (tipo {}, usuario {}, solicitud {}) tras {} intentos: {}. "
                                    + "La solicitud quedó guardada.",
                            eventoId, tipo, usuarioId, referenciaId, MAX_INTENTOS, e.getMessage());
                }
            }
        }
    }

    // Comunicación con otro microservicio; verificar autenticación y respuesta del destino. Método esperarAntesDeReintentar (esperar antes de reintentar).
    private static void esperarAntesDeReintentar() {
        try {
            Thread.sleep(PAUSA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
