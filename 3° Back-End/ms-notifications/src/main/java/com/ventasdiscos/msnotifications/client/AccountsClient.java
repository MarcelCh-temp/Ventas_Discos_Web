package com.ventasdiscos.msnotifications.client;

import com.ventasdiscos.msnotifications.exception.AccesoDenegadoException;
import com.ventasdiscos.msnotifications.exception.ServicioNoDisponibleException;
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
 * Cierre del backend · RN-44 y RF-CTA-08 CA-06 (decisión de cierre 1/4): MS-Notifications pregunta a MS-Accounts si la
 * cuenta del token puede operar antes de atender cualquier petición de un CLIENTE (CuentaActivaInterceptor).
 * Mismo cliente que Orders, Support, PostSale y Catalog (Lote 5): si la cuenta está EN_ELIMINACION, ELIMINADA, con un
 * estado no reconocido o no existe → 403; si Accounts no responde o la respuesta no permite confirmarlo → 503 (nunca se
 * asume ACTIVA).
 * Contrato: GET {accounts}/api/v1/internal/accounts/{userId}/operation-status → { userId, estado, puedeOperar }.
 * 1 envío + 1 reintento ante conexión o 5xx (consulta: repetir es seguro).
 * El RestTemplate agrega un JWT de servicio nuevo en cada llamada (LlamadaInternaInterceptor). Sin X-Internal-Key.
 */
@Component
public class AccountsClient {

    private static final Logger log = LoggerFactory.getLogger(AccountsClient.class);
    // Constante MAX_INTENTOS: valor compartido que no cambia durante la ejecución.
    static final int MAX_INTENTOS = 2;
    // Constante PAUSA_ENTRE_INTENTOS_MS: valor compartido que no cambia durante la ejecución.
    private static final long PAUSA_ENTRE_INTENTOS_MS = 300;
    static final String NO_OPERA = "Tu cuenta no puede realizar nuevas operaciones (está en proceso de eliminación,"
            + " fue eliminada o no está habilitada)";
    static final String NO_DISPONIBLE = "No fue posible confirmar el estado de tu cuenta. No se realizó la operación;"
            + " intenta nuevamente en unos minutos";

    /** Respuesta de Accounts. estado puede ser null si el valor guardado no es reconocido. */
    // Cliente de integración: realiza estado operacion con otro microservicio y trata su respuesta.
    public record EstadoOperacion(Long userId, String estado, Boolean puedeOperar) {
    }

    /** Cierre (RN-44): cuenta ya confirmada ACTIVA en ESTA petición; evita repetir la consulta a Accounts. */
    static final String VERIFICADA = AccountsClient.class.getName() + ".verificada.";

    // Atributo restTemplate: dato o dependencia utilizada por esta clase.
    private final RestTemplate restTemplate;
    // Atributo baseUrl: dato o dependencia utilizada por esta clase.
    private final String baseUrl;

    // Cliente de integración: realiza accounts client con otro microservicio y trata su respuesta.
    public AccountsClient(RestTemplate restTemplate, @Value("${services.accounts.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /** 403 si la cuenta no puede operar o no existe; 503 si no se pudo confirmar. */
    // Cliente de integración: realiza exigir cuenta activa con otro microservicio y trata su respuesta.
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

    // Comunicación con otro microservicio; verificar autenticación y respuesta del destino. Método esperar (esperar).
    private static void esperar() {
        try {
            Thread.sleep(PAUSA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Comunicación con otro microservicio; verificar autenticación y respuesta del destino. Método yaVerificada (ya verificada).
    private static boolean yaVerificada(Long userId) {
        RequestAttributes peticion = RequestContextHolder.getRequestAttributes();
        return peticion != null
                && Boolean.TRUE.equals(peticion.getAttribute(VERIFICADA + userId, RequestAttributes.SCOPE_REQUEST));
    }

    // Comunicación con otro microservicio; verificar autenticación y respuesta del destino. Método marcarVerificada (marcar verificada).
    private static void marcarVerificada(Long userId) {
        RequestAttributes peticion = RequestContextHolder.getRequestAttributes();
        if (peticion != null) {
            peticion.setAttribute(VERIFICADA + userId, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
