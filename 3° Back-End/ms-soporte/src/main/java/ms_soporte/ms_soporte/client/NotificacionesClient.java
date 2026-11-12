package ms_soporte.ms_soporte.client;

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
 * Lote 1 (HU-59, HU-65, HU-67): avisa a MS-Notifications.
 * Contrato real del receptor: POST /api/v1/internal/notifications/events
 * (EventoNotificacionController + EventoNotificacionRequest de ms-notifications).
 * El eventoId es fijo por acción: si se reintenta, Notifications no duplica el aviso.
 * Si Notifications falla, el ticket se guarda igual (la notificación no debe bloquear la operación).
 *
 * Lote 1.1: TicketService llama a este método DESPUÉS de confirmar la transacción.
 *
 * Lote 1.1 (ERS 12.7 "el evento debe poder reintentarse o quedar pendiente"; ERS 12.3 "REST y reintentos
 * controlados"): si el envío falla por conexión, tiempo de espera o error del servidor (5xx), se reintenta
 * con un número LIMITADO de intentos. Es seguro porque el eventoId no cambia y Notifications no duplica (RN-33).
 * Si Notifications rechaza los datos (4xx) NO se reintenta: repetir daría el mismo rechazo (12.7 "no reintentar ciegamente").
 * Límite (documentado): si Notifications sigue caído después del último intento, el aviso queda solo en el log.
 */
@Component
public class NotificacionesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesClient.class);
    private static final String ORIGEN = "SUPPORT";
    static final int MAX_INTENTOS = 2;                    // 1 envío + 1 reintento (ERS 12.7: número limitado)
    private static final long PAUSA_ENTRE_INTENTOS_MS = 500;

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public NotificacionesClient(RestTemplate restTemplate,
                                @Value("${services.notifications.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /** tipo: SOPORTE_CREADO, SOPORTE_RESPONDIDO o SOPORTE_CERRADO (enum TipoNotificacion de ms-notifications). */
    public void notificar(String eventoId, Long usuarioId, String tipo, String titulo, String mensaje, Long ticketId) {
        Map<String, Object> evento = new HashMap<>();
        evento.put("eventoId", eventoId);
        evento.put("origen", ORIGEN);
        evento.put("usuarioId", usuarioId);
        evento.put("titulo", titulo);
        evento.put("mensaje", mensaje);
        evento.put("tipo", tipo);
        evento.put("referenciaId", ticketId);
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                ResponseEntity<Void> r = restTemplate.postForEntity(
                        baseUrl + "/api/v1/internal/notifications/events", evento, Void.class);
                if (r.getStatusCode() == HttpStatus.CREATED) {
                    log.info("Aviso {} registrado en MS-Notifications (usuario {}, ticket {}, intento {})",
                            eventoId, usuarioId, ticketId, intento);
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
                    log.warn("AVISO NO ENTREGADO {} (tipo {}, usuario {}, ticket {}) tras {} intentos: {}. "
                                    + "La solicitud quedó guardada.",
                            eventoId, tipo, usuarioId, ticketId, MAX_INTENTOS, e.getMessage());
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
