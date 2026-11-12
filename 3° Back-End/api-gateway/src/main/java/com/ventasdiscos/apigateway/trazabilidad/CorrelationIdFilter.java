package com.ventasdiscos.apigateway.trazabilidad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lote 6 · DOC-06 (ERS 12.12): PRIMER filtro del Gateway.
 * <ol>
 *   <li>Si la petición trae un X-Correlation-Id válido, se conserva; si no trae o no es válido, se genera uno.</li>
 *   <li>La petición que sigue (y que el Gateway reenvía al MS) lleva SIEMPRE ese identificador: se reemplaza
 *       cualquier valor inválido que haya mandado el navegador.</li>
 *   <li>Se devuelve en la respuesta y queda en el MDC de los logs; al terminar se limpia el MDC para que no pase a
 *       otra petición atendida por el mismo hilo.</li>
 * </ol>
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
            chain.doFilter(new PeticionConCorrelationId(request, id), response);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    /** Muestra el X-Correlation-Id definitivo a todo lo que viene después (incluido el reenvío al MS). */
    static final class PeticionConCorrelationId extends HttpServletRequestWrapper {

        private final String id;

        PeticionConCorrelationId(HttpServletRequest request, String id) {
            super(request);
            this.id = id;
        }

        @Override
        public String getHeader(String name) {
            return CorrelationId.HEADER.equalsIgnoreCase(name) ? id : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return CorrelationId.HEADER.equalsIgnoreCase(name)
                    ? Collections.enumeration(List.of(id)) : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> nombres = new LinkedHashSet<>();
            Enumeration<String> originales = super.getHeaderNames();
            while (originales != null && originales.hasMoreElements()) {
                String nombre = originales.nextElement();
                if (!CorrelationId.HEADER.equalsIgnoreCase(nombre)) {
                    nombres.add(nombre);
                }
            }
            nombres.add(CorrelationId.HEADER);
            return Collections.enumeration(nombres);
        }
    }
}
