package com.ventasdiscos.apigateway.error;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;

/**
 * Lote 6: si el Gateway no logra hablar con el microservicio (apagado, no responde o falla la conexión), el frontend
 * recibe un ApiError con correlationId en vez de una página de error genérica:
 * 503 si el MS no está disponible (incluido no poder conectarse dentro de spring.http.clients.connect-timeout), 504 si
 * no respondió dentro de spring.http.clients.read-timeout y 502 para cualquier otra falla del reenvío.
 * Un 504 NO demuestra que el MS haya cancelado la operación: puede haberla terminado después. El frontend debe
 * consultar el estado (por ejemplo, sus pedidos o solicitudes) antes de repetir una operación sensible.
 * No hay reintentos en el Gateway: repetir un POST podría duplicar una operación (los reintentos seguros ya los hacen
 * los propios MS en sus llamadas internas).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FalloReenvioFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FalloReenvioFilter.class);
    static final String NO_DISPONIBLE = "El servicio solicitado no está disponible en este momento. Intenta nuevamente en unos minutos";
    static final String SIN_RESPUESTA = "El servicio solicitado tardó demasiado en responder. Intenta nuevamente en unos minutos";
    static final String FALLA = "No fue posible completar la solicitud. Intenta nuevamente en unos minutos";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException e) {
            HttpStatus status = clasificar(e);
            log.warn("No se pudo completar {} {} ({}): {}", request.getMethod(), request.getRequestURI(),
                    status.value(), causaRaiz(e).getClass().getSimpleName());
            if (response.isCommitted()) {
                throw e;
            }
            response.resetBuffer();
            ApiErrorJson.escribir(request, response, status, mensaje(status));
        }
    }

    static HttpStatus clasificar(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof ConnectException || t instanceof UnknownHostException || t instanceof NoRouteToHostException
                    || t instanceof HttpConnectTimeoutException) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
            if (t instanceof HttpTimeoutException || t instanceof InterruptedIOException) {
                return HttpStatus.GATEWAY_TIMEOUT;
            }
        }
        return HttpStatus.BAD_GATEWAY;
    }

    private static String mensaje(HttpStatus status) {
        return switch (status) {
            case SERVICE_UNAVAILABLE -> NO_DISPONIBLE;
            case GATEWAY_TIMEOUT -> SIN_RESPUESTA;
            default -> FALLA;
        };
    }

    private static Throwable causaRaiz(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }
}
