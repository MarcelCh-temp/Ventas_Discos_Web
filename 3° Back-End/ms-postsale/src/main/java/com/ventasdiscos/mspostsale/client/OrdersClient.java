package com.ventasdiscos.mspostsale.client;

import com.ventasdiscos.mspostsale.client.OrdersDtos.CancelacionOrders;
import com.ventasdiscos.mspostsale.client.OrdersDtos.PedidoOrders;
import com.ventasdiscos.mspostsale.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mspostsale.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mspostsale.exception.ServicioNoDisponibleException;
import com.ventasdiscos.mspostsale.exception.ValidacionNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase original conservada (mismo nombre). Lote 4: usa los contratos INTERNOS de Orders del Lote 3
 * (CONTRATOS_API_LOTE_3 §5.3). Las rutas antiguas /api/ordenes/{id} y PATCH /api/ordenes/{id}/estado ya no existen.
 *  - GET  /api/v1/internal/orders/{id}         dueño (userId), estado y productos comprados (RN-22, RN-24)
 *  - POST /api/v1/internal/orders/{id}/cancel  aplica una cancelación APROBADA; Orders revalida el estado y repone el
 *                                              stock UNA vez (idempotente: repetirla responde yaCancelado: true)
 * Reintentos limitados ante conexión o 5xx (mismo patrón aprobado de los Lotes 1 y 3). Repetir es seguro: la
 * consulta no cambia nada y la cancelación es idempotente en Orders. Ante 4xx no se reintenta.
 * Lote 6 · DOC-05: el RestTemplate agrega un JWT de servicio nuevo en cada llamada. Sin X-Internal-Key.
 */
@Component
public class OrdersClient {

    private static final Logger log = LoggerFactory.getLogger(OrdersClient.class);
    static final int MAX_INTENTOS = 2;
    private static final long PAUSA_ENTRE_INTENTOS_MS = 300;
    private static final Pattern MENSAJE = Pattern.compile("\"mensaje\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    static final String NO_DISPONIBLE = "No fue posible comunicarse con el servicio de pedidos. Intenta nuevamente en unos minutos";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrdersClient(@Qualifier("ordersRestTemplate") RestTemplate restTemplate,
                        @Value("${services.orders.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /** 404 si el pedido no existe; 503 si Orders no responde. */
    public PedidoOrders obtener(Long pedidoId) {
        return llamar(() -> restTemplate.getForObject(baseUrl + "/api/v1/internal/orders/{id}", PedidoOrders.class,
                pedidoId), "consulta del pedido " + pedidoId);
    }

    /**
     * 409 si Orders ya no permite cancelar (DESPACHADO o ENTREGADO, RN-19); 404 si no existe; 503 si no responde
     * (en ese caso no se sabe si alcanzó a cancelar: repetir la llamada es seguro).
     */
    public CancelacionOrders cancelar(Long pedidoId, Long solicitudId, String motivo) {
        Map<String, Object> body = Map.of("solicitudId", solicitudId, "motivo", motivo);
        return llamar(() -> restTemplate.postForObject(baseUrl + "/api/v1/internal/orders/{id}/cancel", body,
                CancelacionOrders.class, pedidoId), "cancelación del pedido " + pedidoId);
    }

    private <T> T llamar(Supplier<T> llamada, String que) {
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                T r = llamada.get();
                if (r == null) {
                    throw new RestClientException("Orders respondió sin cuerpo");
                }
                return r;
            } catch (HttpClientErrorException e) {
                String mensaje = mensaje(e);
                int status = e.getStatusCode().value();
                log.warn("Orders respondió {} a la {}: {}", status, que, mensaje);
                if (status == 404) {
                    throw new RecursoNoEncontradoException("Pedido no encontrado");
                }
                if (status == 409) {
                    throw new OperacionNoPermitidaException(mensaje);
                }
                throw new ValidacionNegocioException(mensaje);
            } catch (RestClientException e) {
                if (intento < MAX_INTENTOS) {
                    esperar();
                } else {
                    log.warn("Orders no respondió a la {} tras {} intentos: {}", que, MAX_INTENTOS, e.getMessage());
                }
            }
        }
        throw new ServicioNoDisponibleException(NO_DISPONIBLE);
    }

    /** Toma "mensaje" del ApiError de Orders; si no viene, un texto genérico. */
    static String mensaje(HttpClientErrorException e) {
        Matcher m = MENSAJE.matcher(e.getResponseBodyAsString());
        return m.find() ? m.group(1).replace("\\\"", "\"")
                : "El servicio de pedidos rechazó la solicitud (" + e.getStatusCode().value() + ")";
    }

    private static void esperar() {
        try {
            Thread.sleep(PAUSA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
