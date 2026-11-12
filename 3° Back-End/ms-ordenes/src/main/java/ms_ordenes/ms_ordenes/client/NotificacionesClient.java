package ms_ordenes.ms_ordenes.client;

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
 * Lote 3 (RF-COM-08, RF-PED-04/06/08): avisos de pedido a MS-Notifications. Copia del cliente de ms-soporte
 * (Lote 1, aprobado): mismo contrato POST /api/v1/internal/notifications/events, mismos reintentos limitados
 * (conexión o 5xx; ante 4xx no se reintenta) y eventoId fijo por acción para que Notifications no duplique (RN-33).
 * OrdenService lo llama DESPUÉS de confirmar la transacción: si Notifications falla, el pedido ya está guardado
 * (ERS 11.5: la caída de Notifications no impide crear ni actualizar un pedido).
 */
@Component
public class NotificacionesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesClient.class);
    private static final String ORIGEN = "ORDERS";
    static final int MAX_INTENTOS = 2;                    // 1 envío + 1 reintento (ERS 12.7: número limitado)
    private static final long PAUSA_ENTRE_INTENTOS_MS = 500;

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public NotificacionesClient(RestTemplate restTemplate,
                                @Value("${services.notifications.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /** tipo: PEDIDO_CREADO o PEDIDO_ACTUALIZADO (enum TipoNotificacion de ms-notifications). */
    public void notificar(String eventoId, Long usuarioId, String tipo, String titulo, String mensaje, Long pedidoId) {
        Map<String, Object> evento = new HashMap<>();
        evento.put("eventoId", eventoId);
        evento.put("origen", ORIGEN);
        evento.put("usuarioId", usuarioId);
        evento.put("titulo", titulo);
        evento.put("mensaje", mensaje);
        evento.put("tipo", tipo);
        evento.put("referenciaId", pedidoId);
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                ResponseEntity<Void> r = restTemplate.postForEntity(
                        baseUrl + "/api/v1/internal/notifications/events", evento, Void.class);
                if (r.getStatusCode() == HttpStatus.CREATED) {
                    log.info("Aviso {} registrado en MS-Notifications (usuario {}, pedido {}, intento {})",
                            eventoId, usuarioId, pedidoId, intento);
                } else {
                    // 200: Notifications ya tenía ese eventoId y no lo duplicó (RN-33)
                    log.info("Aviso {} ya estaba registrado en MS-Notifications; no se duplicó", eventoId);
                }
                return;
            } catch (HttpClientErrorException e) {
                // 4xx: Notifications rechazó los datos; repetir el mismo envío no lo arreglaría
                log.warn("AVISO RECHAZADO {} por MS-Notifications ({}): no se reintenta. El pedido quedó guardado.",
                        eventoId, e.getStatusCode());
                return;
            } catch (RestClientException e) {
                if (intento < MAX_INTENTOS) {
                    log.info("Aviso {}: intento {} de {} falló ({}); se reintenta", eventoId, intento, MAX_INTENTOS, e.getMessage());
                    esperarAntesDeReintentar();
                } else {
                    log.warn("AVISO NO ENTREGADO {} (tipo {}, usuario {}, pedido {}) tras {} intentos: {}. "
                                    + "El pedido quedó guardado.",
                            eventoId, tipo, usuarioId, pedidoId, MAX_INTENTOS, e.getMessage());
                }
            }
        }
    }

    private static void esperarAntesDeReintentar() {
        try {
            Thread.sleep(PAUSA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
