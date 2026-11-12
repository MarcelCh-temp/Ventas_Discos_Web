package ms_soporte.ms_soporte.client;

import ms_soporte.ms_soporte.exception.AccesoDenegadoException;
import ms_soporte.ms_soporte.exception.ServicioNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Lote 5 · Integración necesaria de HU-78 (RN-37, RN-44, prueba T-06), autorizada por el equipo. Misma clase que en
 * ms-ordenes. Antes de crear una solicitud de soporte (ticket) se pregunta a MS-Accounts si la cuenta del token puede operar.
 * Un token todavía válido NO basta: si la cuenta está EN_ELIMINACION, ELIMINADA o con un estado no reconocido, la
 * operación se rechaza (403).
 * Contrato: GET {accounts}/api/v1/internal/accounts/{userId}/operation-status → { userId, estado, puedeOperar }.
 * Cierre (RN-44, RF-CTA-08 CA-06): además lo usa CuentaActivaInterceptor en TODA petición de un CLIENTE.
 * Si Accounts no responde o la respuesta no permite confirmarlo → 503 y no se crea nada (nunca se asume ACTIVA).
 * 1 envío + 1 reintento ante conexión o 5xx (consulta: repetir es seguro).
 * Lote 6 · DOC-05: el RestTemplate agrega un JWT de servicio nuevo en cada llamada (LlamadaInternaInterceptor).
 * Sin X-Internal-Key.
 */
@Component
public class AccountsClient {

    private static final Logger log = LoggerFactory.getLogger(AccountsClient.class);
    static final int MAX_INTENTOS = 2;
    private static final long PAUSA_ENTRE_INTENTOS_MS = 300;
    static final String NO_OPERA = "Tu cuenta no puede realizar nuevas operaciones (está en proceso de eliminación,"
            + " fue eliminada o no está habilitada)";
    static final String NO_DISPONIBLE = "No fue posible confirmar el estado de tu cuenta. No se realizó la operación;"
            + " intenta nuevamente en unos minutos";

    /** Respuesta de Accounts. estado puede ser null si el valor guardado no es reconocido. */
    public record EstadoOperacion(Long userId, String estado, Boolean puedeOperar) {
    }

    /** Cierre (RN-44): cuenta ya confirmada ACTIVA en ESTA petición; evita repetir la consulta a Accounts. */
    static final String VERIFICADA = AccountsClient.class.getName() + ".verificada.";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AccountsClient(RestTemplate restTemplate, @Value("${services.accounts.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /** 403 si la cuenta no puede operar o no existe; 503 si no se pudo confirmar. */
    public void exigirCuentaActiva(Long userId) {
        if (yaVerificada(userId)) {
            return;
        }
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                EstadoOperacion r = restTemplate.getForObject(
                        baseUrl + "/api/v1/internal/accounts/{userId}/operation-status", EstadoOperacion.class, userId);
                if (r == null || r.puedeOperar() == null) {
                    log.warn("Accounts respondió sin el estado de la cuenta {}", userId);
                    throw new ServicioNoDisponibleException(NO_DISPONIBLE);
                }
                if (!r.puedeOperar()) {
                    log.info("Operación rechazada: la cuenta {} está {}", userId, r.estado());
                    throw new AccesoDenegadoException(NO_OPERA);
                }
                marcarVerificada(userId);
                return;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 404) {
                    throw new AccesoDenegadoException(NO_OPERA);
                }
                log.warn("Accounts rechazó la consulta de estado de la cuenta {} ({})", userId, e.getStatusCode().value());
                throw new ServicioNoDisponibleException(NO_DISPONIBLE);
            } catch (RestClientException e) {
                if (intento < MAX_INTENTOS) {
                    esperar();
                } else {
                    log.warn("Accounts no respondió tras {} intentos: {}", MAX_INTENTOS, e.getMessage());
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

    private static boolean yaVerificada(Long userId) {
        RequestAttributes peticion = RequestContextHolder.getRequestAttributes();
        return peticion != null
                && Boolean.TRUE.equals(peticion.getAttribute(VERIFICADA + userId, RequestAttributes.SCOPE_REQUEST));
    }

    private static void marcarVerificada(Long userId) {
        RequestAttributes peticion = RequestContextHolder.getRequestAttributes();
        if (peticion != null) {
            peticion.setAttribute(VERIFICADA + userId, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
