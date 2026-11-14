package com.ventasdiscos.apigateway.error;

import com.ventasdiscos.apigateway.trazabilidad.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lote 6 · DOC-06: respuestas de error generadas POR EL GATEWAY (401, 403, 404, 400, 502, 503, 504) con el MISMO
 * formato ApiError de los microservicios: timestamp, status, error, mensaje, path, errores, correlationId.
 * Los errores que devuelve un microservicio pasan tal cual (ya vienen en ApiError con su correlationId).
 * El JSON se arma a mano (solo textos simples y escapados) para no depender de la configuración de Jackson.
 */
public final class ApiErrorJson {

    // Ejecuta api error json como parte de las responsabilidades de esta clase.
    private ApiErrorJson() {
    }

    public static void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                                String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(json(status, mensaje, request.getRequestURI(), CorrelationId.de(request)));
        response.getWriter().flush();
    }

    // Ejecuta json como parte de las responsabilidades de esta clase.
    static String json(HttpStatus status, String mensaje, String path, String correlationId) {
        return "{"
                + "\"timestamp\":\"" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\","
                + "\"status\":" + status.value() + ","
                + "\"error\":" + texto(status.getReasonPhrase()) + ","
                + "\"mensaje\":" + texto(mensaje) + ","
                + "\"path\":" + texto(path) + ","
                + "\"errores\":null,"
                + "\"correlationId\":" + texto(correlationId)
                + "}";
    }

    /** Cadena JSON escapada, o null. */
    // Ejecuta texto como parte de las responsabilidades de esta clase.
    static String texto(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(valor.length() + 2).append('"');
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
