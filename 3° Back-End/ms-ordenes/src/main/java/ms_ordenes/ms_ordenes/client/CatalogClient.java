package ms_ordenes.ms_ordenes.client;

import ms_ordenes.ms_ordenes.client.CatalogoDtos.Cotizacion;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.Disponibilidad;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.ItemSolicitado;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.RespuestaOperacion;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.ResultadoOperacion;
import ms_ordenes.ms_ordenes.exception.RecursoNoEncontradoException;
import ms_ordenes.ms_ordenes.exception.ServicioNoDisponibleException;
import ms_ordenes.ms_ordenes.exception.ValidacionNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lote 3: Orders -> MS-Catalog (ERS 11.5 y 12.8). Contratos ya publicados por Catalog (Lote 2):
 *  - POST /api/v1/internal/catalog/pricing       precio y promociones vigentes (RN-13, RN-15)
 *  - POST /api/v1/inventory/availability/query   ¿alcanza el stock? (sin exponer la cantidad exacta)
 *  - POST /api/v1/internal/inventory/operations  descuento/reposición IDEMPOTENTE por operacionId (RN-42, DOC-08)
 * Reintentos limitados ante conexión o 5xx (igual que NotificacionesClient del Lote 1). Repetir es seguro: las
 * consultas no cambian nada y las operaciones usan siempre la misma operacionId. Ante 4xx no se reintenta.
 * Lote 6 · DOC-05: el RestTemplate agrega un JWT de servicio nuevo en cada llamada (LlamadaInternaInterceptor).
 */
@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);
    static final int MAX_INTENTOS = 2;
    private static final long PAUSA_ENTRE_INTENTOS_MS = 300;
    private static final Pattern MENSAJE = Pattern.compile("\"mensaje\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    static final String NO_DISPONIBLE = "No fue posible comunicarse con el catálogo. Intenta nuevamente en unos minutos";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CatalogClient(RestTemplate restTemplate, @Value("${services.catalog.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Cotizacion cotizar(List<ItemSolicitado> items) {
        Map<String, Object> body = Map.of("items", items);
        return consultar(() -> restTemplate.postForObject(baseUrl + "/api/v1/internal/catalog/pricing", body,
                Cotizacion.class));
    }

    public List<Disponibilidad> disponibilidad(List<ItemSolicitado> items) {
        HttpEntity<Map<String, Object>> body = new HttpEntity<>(Map.of("items", items));
        return consultar(() -> restTemplate.exchange(baseUrl + "/api/v1/inventory/availability/query",
                HttpMethod.POST, body, new ParameterizedTypeReference<List<Disponibilidad>>() {
                }).getBody());
    }

    /** tipo: "SALIDA" (venta) o "ENTRADA" (reposición por cancelación). */
    public RespuestaOperacion aplicarOperacion(String operacionId, String motivo, String tipo, List<ItemSolicitado> items) {
        List<Map<String, Object>> lineas = items.stream()
                .map(i -> Map.<String, Object>of("productId", i.productId(), "tipo", tipo, "cantidad", i.cantidad()))
                .toList();
        Map<String, Object> body = Map.of("operacionId", operacionId, "motivo", motivo, "items", lineas);
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                int status = restTemplate.postForEntity(baseUrl + "/api/v1/internal/inventory/operations", body,
                        String.class).getStatusCode().value();
                log.info("Operación de stock {} aplicada en Catalog ({}, intento {})", operacionId, status, intento);
                return new RespuestaOperacion(ResultadoOperacion.APLICADA, null, status);
            } catch (HttpClientErrorException e) {
                // Revisión Lote 3: 409 = conflicto (stock insuficiente u operacionId con otro contenido); el resto de
                // los 4xx = solicitud rechazada. Ninguno se reintenta: repetir daría la misma respuesta.
                String mensaje = mensaje(e);
                int status = e.getStatusCode().value();
                ResultadoOperacion resultado = status == 409 ? ResultadoOperacion.CONFLICTO : ResultadoOperacion.RECHAZADA;
                log.warn("Catalog respondió {} a la operación de stock {}: {}", status, operacionId, mensaje);
                return new RespuestaOperacion(resultado, mensaje, status);
            } catch (RestClientException e) {
                if (intento < MAX_INTENTOS) {
                    esperar();
                } else {
                    log.warn("Operación de stock {} NO CONFIRMADA tras {} intentos: {}", operacionId, MAX_INTENTOS,
                            e.getMessage());
                }
            }
        }
        return new RespuestaOperacion(ResultadoOperacion.NO_CONFIRMADA, NO_DISPONIBLE);
    }

    private <T> T consultar(Supplier<T> llamada) {
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                return llamada.get();
            } catch (HttpClientErrorException e) {
                String mensaje = mensaje(e);
                if (e.getStatusCode().value() == 404) {
                    throw new RecursoNoEncontradoException(mensaje);
                }
                throw new ValidacionNegocioException(mensaje);
            } catch (RestClientException e) {
                if (intento < MAX_INTENTOS) {
                    esperar();
                } else {
                    log.warn("Catalog no respondió tras {} intentos: {}", MAX_INTENTOS, e.getMessage());
                }
            }
        }
        throw new ServicioNoDisponibleException(NO_DISPONIBLE);
    }

    /** Toma "mensaje" del ApiError de Catalog; si no viene, un texto genérico. */
    static String mensaje(HttpClientErrorException e) {
        Matcher m = MENSAJE.matcher(e.getResponseBodyAsString());
        return m.find() ? m.group(1).replace("\\\"", "\"") : "El catálogo rechazó la solicitud (" + e.getStatusCode().value() + ")";
    }

    private static void esperar() {
        try {
            Thread.sleep(PAUSA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
