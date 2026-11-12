package com.ventasdiscos.msaccounts.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

/**
 * Lote 5 · HU-78 / RF-CTA-08 / RN-36 / RN-43: Accounts coordina la eliminación llamando a las rutas INTERNAS de los
 * otros MS (ERS 13.8). Nunca entra a sus bases de datos.
 * <ul>
 *   <li>GET  {orders}/api/v1/internal/orders/users/{userId}/active-exists</li>
 *   <li>GET  {support}/api/v1/internal/support/users/{userId}/open-exists</li>
 *   <li>GET  {postsale}/api/v1/internal/post-sale/users/{userId}/pending-exists</li>
 *   <li>POST {catalog}/api/v1/internal/catalog/users/{userId}/favorites/purge (idempotente)</li>
 *   <li>POST {notifications}/api/v1/internal/notifications/users/{userId}/purge (idempotente)</li>
 * </ul>
 * Reintentos limitados ante conexión o 5xx (patrón aprobado en los Lotes 1, 3 y 4): repetir es seguro porque las
 * consultas no cambian nada y las depuraciones son idempotentes. Cualquier fallo (sin respuesta, 5xx, 4xx o cuerpo
 * vacío) lanza {@link DependenciaNoConfirmadaException}: Accounts NUNCA asume que no hay pendientes.
 * Lote 6 · DOC-05: el RestTemplate agrega a cada llamada un JWT de servicio nuevo (LlamadaInternaInterceptor).
 * Sin X-Internal-Key.
 */
@Slf4j
@Component
public class DependenciasCuentaClient {

    static final int MAX_INTENTOS = 2;
    private static final long PAUSA_ENTRE_INTENTOS_MS = 300;

    /** Respuesta de las tres consultas "¿existe algo pendiente?". */
    public record ExisteResponse(Long userId, Boolean existe) {
    }

    /** Respuesta de las dos depuraciones (cuántas filas se borraron en esta llamada; 0 si ya estaba hecho). */
    public record DepuracionResponse(Long userId, Integer eliminados) {
    }

    /** Un MS no confirmó la consulta o la depuración. {@code servicio} identifica cuál (para el motivo). */
    public static class DependenciaNoConfirmadaException extends RuntimeException {
        private final String servicio;

        public DependenciaNoConfirmadaException(String servicio, String detalle) {
            super(servicio + ": " + detalle);
            this.servicio = servicio;
        }

        public String getServicio() {
            return servicio;
        }
    }

    private final RestTemplate restTemplate;
    private final String ordersUrl;
    private final String supportUrl;
    private final String postSaleUrl;
    private final String catalogUrl;
    private final String notificationsUrl;

    public DependenciasCuentaClient(RestTemplate restTemplate,
                                    @Value("${services.orders.url}") String ordersUrl,
                                    @Value("${services.support.url}") String supportUrl,
                                    @Value("${services.postsale.url}") String postSaleUrl,
                                    @Value("${services.catalog.url}") String catalogUrl,
                                    @Value("${services.notifications.url}") String notificationsUrl) {
        this.restTemplate = restTemplate;
        this.ordersUrl = ordersUrl;
        this.supportUrl = supportUrl;
        this.postSaleUrl = postSaleUrl;
        this.catalogUrl = catalogUrl;
        this.notificationsUrl = notificationsUrl;
    }

    /** RN-36: pedidos PENDIENTE, CONFIRMADO, PREPARANDO o DESPACHADO. */
    public boolean tienePedidosActivos(Long userId) {
        return existe("Orders", ordersUrl + "/api/v1/internal/orders/users/{userId}/active-exists", userId);
    }

    /** RN-36: tickets ABIERTA o EN_PROCESO. */
    public boolean tieneTicketsAbiertos(Long userId) {
        return existe("Support", supportUrl + "/api/v1/internal/support/users/{userId}/open-exists", userId);
    }

    /** RN-36: solicitudes de postventa PENDIENTE. */
    public boolean tieneSolicitudesPendientes(Long userId) {
        return existe("PostSale", postSaleUrl + "/api/v1/internal/post-sale/users/{userId}/pending-exists", userId);
    }

    /** RN-43: depuración idempotente de favoritos en Catalog. Devuelve cuántos borró en esta llamada. */
    public int depurarFavoritos(Long userId) {
        return depurar("Catalog", catalogUrl + "/api/v1/internal/catalog/users/{userId}/favorites/purge", userId);
    }

    /** RN-43: depuración idempotente de notificaciones personales en Notifications. */
    public int depurarNotificaciones(Long userId) {
        return depurar("Notifications", notificationsUrl + "/api/v1/internal/notifications/users/{userId}/purge", userId);
    }

    private boolean existe(String servicio, String url, Long userId) {
        ExisteResponse r = llamar(servicio, () -> restTemplate.getForObject(url, ExisteResponse.class, userId));
        if (r.existe() == null) {
            throw new DependenciaNoConfirmadaException(servicio, "respuesta sin el dato 'existe'");
        }
        return r.existe();
    }

    private int depurar(String servicio, String url, Long userId) {
        DepuracionResponse r = llamar(servicio, () -> restTemplate.postForObject(url, null, DepuracionResponse.class, userId));
        return r.eliminados() == null ? 0 : r.eliminados();
    }

    private <T> T llamar(String servicio, Supplier<T> llamada) {
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                T r = llamada.get();
                if (r == null) {
                    throw new DependenciaNoConfirmadaException(servicio, "respondió sin cuerpo");
                }
                return r;
            } catch (HttpClientErrorException e) {
                // 4xx: repetir daría lo mismo. No se interpreta como "sin pendientes".
                log.warn("{} rechazó la llamada interna ({}); no se reintenta", servicio, e.getStatusCode().value());
                throw new DependenciaNoConfirmadaException(servicio, "rechazó la consulta (" + e.getStatusCode().value() + ")");
            } catch (RestClientException e) {
                if (intento < MAX_INTENTOS) {
                    esperar();
                } else {
                    log.warn("{} no respondió tras {} intentos: {}", servicio, MAX_INTENTOS, e.getMessage());
                }
            }
        }
        throw new DependenciaNoConfirmadaException(servicio, "no respondió");
    }

    private static void esperar() {
        try {
            Thread.sleep(PAUSA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
