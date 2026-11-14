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

    // Constante MAX_INTENTOS: valor compartido que no cambia durante la ejecución.
    static final int MAX_INTENTOS = 2;
    // Constante PAUSA_ENTRE_INTENTOS_MS: valor compartido que no cambia durante la ejecución.
    private static final long PAUSA_ENTRE_INTENTOS_MS = 300;

    /** Respuesta de las tres consultas "¿existe algo pendiente?". */
    // Cliente de integración: realiza existe response con otro microservicio y trata su respuesta.
    public record ExisteResponse(Long userId, Boolean existe) {
    }

    /** Respuesta de las dos depuraciones (cuántas filas se borraron en esta llamada; 0 si ya estaba hecho). */
    // Cliente de integración: realiza depuracion response con otro microservicio y trata su respuesta.
    public record DepuracionResponse(Long userId, Integer eliminados) {
    }

    /** Un MS no confirmó la consulta o la depuración. {@code servicio} identifica cuál (para el motivo). */
    public static class DependenciaNoConfirmadaException extends RuntimeException {
        // Atributo servicio: dato o dependencia utilizada por esta clase.
        private final String servicio;

        // Cliente de integración: realiza dependencia no confirmada exception con otro microservicio y trata su respuesta.
        public DependenciaNoConfirmadaException(String servicio, String detalle) {
            super(servicio + ": " + detalle);
            this.servicio = servicio;
        }

        // Cliente de integración: realiza get servicio con otro microservicio y trata su respuesta.
        public String getServicio() {
            return servicio;
        }
    }

    // Atributo restTemplate: dato o dependencia utilizada por esta clase.
    private final RestTemplate restTemplate;
    // Atributo ordersUrl: dato o dependencia utilizada por esta clase.
    private final String ordersUrl;
    // Atributo supportUrl: dato o dependencia utilizada por esta clase.
    private final String supportUrl;
    // Atributo postSaleUrl: dato o dependencia utilizada por esta clase.
    private final String postSaleUrl;
    // Atributo catalogUrl: dato o dependencia utilizada por esta clase.
    private final String catalogUrl;
    // Dato notifications url del registro.
    private final String notificationsUrl;

    // Comunicación con otro microservicio; verificar autenticación y respuesta del destino. Método DependenciasCuentaClient (dependencias cuenta client).
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
    // Cliente de integración: realiza tiene pedidos activos con otro microservicio y trata su respuesta.
    public boolean tienePedidosActivos(Long userId) {
        return existe("Orders", ordersUrl + "/api/v1/internal/orders/users/{userId}/active-exists", userId);
    }

    /** RN-36: tickets ABIERTA o EN_PROCESO. */
    // Cliente de integración: realiza tiene tickets abiertos con otro microservicio y trata su respuesta.
    public boolean tieneTicketsAbiertos(Long userId) {
        return existe("Support", supportUrl + "/api/v1/internal/support/users/{userId}/open-exists", userId);
    }

    /** RN-36: solicitudes de postventa PENDIENTE. */
    // Cliente de integración: realiza tiene solicitudes pendientes con otro microservicio y trata su respuesta.
    public boolean tieneSolicitudesPendientes(Long userId) {
        return existe("PostSale", postSaleUrl + "/api/v1/internal/post-sale/users/{userId}/pending-exists", userId);
    }

    /** RN-43: depuración idempotente de favoritos en Catalog. Devuelve cuántos borró en esta llamada. */
    // Cliente de integración: realiza depurar favoritos con otro microservicio y trata su respuesta.
    public int depurarFavoritos(Long userId) {
        return depurar("Catalog", catalogUrl + "/api/v1/internal/catalog/users/{userId}/favorites/purge", userId);
    }

    /** RN-43: depuración idempotente de notificaciones personales en Notifications. */
    // Cliente de integración: realiza depurar notificaciones con otro microservicio y trata su respuesta.
    public int depurarNotificaciones(Long userId) {
        return depurar("Notifications", notificationsUrl + "/api/v1/internal/notifications/users/{userId}/purge", userId);
    }

    // Cliente de integración: realiza existe con otro microservicio y trata su respuesta.
    private boolean existe(String servicio, String url, Long userId) {
        ExisteResponse r = llamar(servicio, () -> restTemplate.getForObject(url, ExisteResponse.class, userId));
        if (r.existe() == null) {
            throw new DependenciaNoConfirmadaException(servicio, "respuesta sin el dato 'existe'");
        }
        return r.existe();
    }

    // Cliente de integración: realiza depurar con otro microservicio y trata su respuesta.
    private int depurar(String servicio, String url, Long userId) {
        DepuracionResponse r = llamar(servicio, () -> restTemplate.postForObject(url, null, DepuracionResponse.class, userId));
        return r.eliminados() == null ? 0 : r.eliminados();
    }

    // Comunicación con otro microservicio; verificar autenticación y respuesta del destino. Método llamar (llamar).
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

    // Comunicación con otro microservicio; verificar autenticación y respuesta del destino. Método esperar (esperar).
    private static void esperar() {
        try {
            Thread.sleep(PAUSA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
