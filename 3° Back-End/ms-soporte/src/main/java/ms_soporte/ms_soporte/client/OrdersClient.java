package ms_soporte.ms_soporte.client;

import ms_soporte.ms_soporte.exception.RecursoNoEncontradoException;
import ms_soporte.ms_soporte.exception.ServicioNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cierre del backend · RF-SOP-01 / HU-59 (decisión de cierre 2/4; pendiente desde el Lote 1): si el Cliente asocia un
 * pedido a su ticket, MS-Orders confirma que el pedido existe y es suyo.
 * Contrato existente (Lote 4, sin rutas nuevas): GET {orders}/api/v1/internal/orders/{id} → PedidoInternoResponse.
 * Solo se usan id y userId; el resto se recibe tal cual para no depender de campos opcionales.
 * <ul>
 *   <li>Existe y su userId es el del JWT → se puede crear el ticket.</li>
 *   <li>No existe o es de otro cliente → 404 con el MISMO mensaje (no se revela que el pedido existe).</li>
 *   <li>Orders no responde, responde sin datos o rechaza la llamada (otro 4xx, 5xx) → 503. Nunca se asume válido.</li>
 * </ul>
 * 1 envío + 1 reintento ante conexión o 5xx (consulta: repetir es seguro). El RestTemplate agrega el JWT de servicio de
 * ms-support (DOC-05) y X-Correlation-Id (DOC-06). Sin X-Internal-Key.
 */
@Component
public class OrdersClient {

    private static final Logger log = LoggerFactory.getLogger(OrdersClient.class);
    static final int MAX_INTENTOS = 2;
    private static final long PAUSA_ENTRE_INTENTOS_MS = 300;
    static final String NO_ENCONTRADO = "El pedido indicado no existe o no corresponde a tu cuenta";
    static final String NO_DISPONIBLE = "No fue posible verificar el pedido asociado. No se creó la solicitud;"
            + " intenta nuevamente en unos minutos";

    /** Respuesta interna de Orders (mismos campos que PedidoInternoResponse). */
    public record PedidoOrders(Long id, String numeroPedido, Long userId, String estado, LocalDateTime fechaCompra,
                               BigDecimal total, List<ItemPedido> items) {
    }

    public record ItemPedido(Long productId, String nombreProducto, int cantidad) {
    }

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrdersClient(RestTemplate restTemplate, @Value("${services.orders.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /** 404 si el pedido no existe o es de otro cliente; 503 si no se pudo confirmar. */
    public void exigirPedidoPropio(Long orderId, Long userId) {
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                PedidoOrders pedido = restTemplate.getForObject(baseUrl + "/api/v1/internal/orders/{id}",
                        PedidoOrders.class, orderId);
                if (pedido == null || pedido.userId() == null) {
                    log.warn("Orders respondió sin el dueño del pedido {}", orderId);
                    throw new ServicioNoDisponibleException(NO_DISPONIBLE);
                }
                if (!pedido.userId().equals(userId)) {
                    log.info("Ticket rechazado: el pedido {} no pertenece al usuario {}", orderId, userId);
                    throw new RecursoNoEncontradoException(NO_ENCONTRADO);
                }
                return;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 404) {
                    throw new RecursoNoEncontradoException(NO_ENCONTRADO);
                }
                log.warn("Orders rechazó la consulta del pedido {} ({})", orderId, e.getStatusCode().value());
                throw new ServicioNoDisponibleException(NO_DISPONIBLE);
            } catch (RestClientException e) {
                if (intento < MAX_INTENTOS) {
                    esperar();
                } else {
                    log.warn("Orders no respondió tras {} intentos: {}", MAX_INTENTOS, e.getMessage());
                }
            }
        }
        throw new ServicioNoDisponibleException(NO_DISPONIBLE);
    }

    private static void esperar() {
        try {
            Thread.sleep(PAUSA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
