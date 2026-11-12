package ms_soporte.ms_soporte.trazabilidad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lote 6 · DOC-06: primer filtro de cada petición. Conserva el X-Correlation-Id recibido si es válido (normalmente
 * lo puso el API Gateway o el MS que llama) o genera uno nuevo; lo deja en el MDC de los logs, en un atributo de la
 * petición (para ApiError) y en la respuesta. Al terminar SIEMPRE limpia el MDC: cada hilo atiende muchas
 * peticiones y el identificador de una no debe aparecer en otra.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibido = request.getHeader(CorrelationId.HEADER);
        String id = CorrelationId.esValido(recibido) ? recibido : CorrelationId.nuevo();
        request.setAttribute(CorrelationId.ATRIBUTO, id);
        response.setHeader(CorrelationId.HEADER, id);
        MDC.put(CorrelationId.MDC_KEY, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}
