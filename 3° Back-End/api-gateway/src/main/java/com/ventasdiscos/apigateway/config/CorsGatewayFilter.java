package com.ventasdiscos.apigateway.config;

import com.ventasdiscos.apigateway.trazabilidad.CorrelationId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Lote 6 · Decisión 2/4: CORS SOLO en el Gateway (los MS ya no lo configuran, así no se duplican cabeceras).
 * <ul>
 *   <li>Orígenes exactos desde CORS_ORIGINS (por defecto http://localhost:5500 y http://127.0.0.1:5500). No se
 *       aceptan comodines "*": si alguien los configura, el Gateway no arranca.</li>
 *   <li>Métodos de los contratos: GET, POST, PUT, PATCH, DELETE y OPTIONS (preflight).</li>
 *   <li>Cabeceras permitidas: Authorization (JWT de usuario), Content-Type, Accept y X-Correlation-Id (DOC-06);
 *       el navegador puede leer X-Correlation-Id de la respuesta.</li>
 *   <li>Sin credenciales de navegador (cookies): el token viaja en Authorization.</li>
 * </ul>
 * Va antes del filtro de seguridad: el preflight OPTIONS se responde aquí (sin token) y los errores 401/403/404 del
 * Gateway también llevan las cabeceras CORS para que el frontend pueda leer el ApiError.
 * CORS NO es autenticación: solo indica al navegador qué páginas pueden leer las respuestas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CorsGatewayFilter extends CorsFilter {

    static final List<String> METODOS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    static final List<String> CABECERAS = List.of("Authorization", "Content-Type", "Accept", CorrelationId.HEADER);

    // Ejecuta cors gateway filter como parte de las responsabilidades de esta clase.
    public CorsGatewayFilter(@Value("${gateway.cors.allowed-origins}") String[] origenes) {
        super(fuente(origenes));
    }

    // Ejecuta fuente como parte de las responsabilidades de esta clase.
    static UrlBasedCorsConfigurationSource fuente(String[] origenes) {
        List<String> limpios = origenes == null ? List.of()
                : Arrays.stream(origenes).map(String::trim).filter(o -> !o.isEmpty()).toList();
        if (limpios.isEmpty()) {
            throw new IllegalStateException("CORS_ORIGINS debe indicar al menos un origen del frontend");
        }
        for (String origen : limpios) {
            if (origen.contains("*")) {
                throw new IllegalStateException("CORS_ORIGINS no admite comodines (*): " + origen);
            }
        }
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(limpios);
        config.setAllowedMethods(METODOS);
        config.setAllowedHeaders(CABECERAS);
        config.setExposedHeaders(List.of(CorrelationId.HEADER));
        config.setAllowCredentials(false);
        config.setMaxAge(1800L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
