package com.ventasdiscos.apigateway.trazabilidad;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lote 6 · DOC-06: identificador de correlación (cabecera X-Correlation-Id). El Gateway lo GENERA cuando la petición
 * del frontend no trae uno válido y lo reenvía al microservicio, que lo propaga a sus llamadas internas.
 * Solo se aceptan 8 a 64 letras, números o guiones (evita inyectar texto en los logs).
 */
public final class CorrelationId {

    // Constante HEADER: valor compartido que no cambia durante la ejecución.
    public static final String HEADER = "X-Correlation-Id";
    // Constante MDC_KEY: valor compartido que no cambia durante la ejecución.
    public static final String MDC_KEY = "correlationId";
    public static final String ATRIBUTO = CorrelationId.class.getName();
    private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9-]{8,64}");

    // Ejecuta correlation id como parte de las responsabilidades de esta clase.
    private CorrelationId() {
    }

    // Ejecuta es valido como parte de las responsabilidades de esta clase.
    public static boolean esValido(String valor) {
        return valor != null && VALIDO.matcher(valor).matches();
    }

    // Ejecuta nuevo como parte de las responsabilidades de esta clase.
    public static String nuevo() {
        return UUID.randomUUID().toString();
    }

    /** El de la petición (atributo) o, si no hay, el del hilo actual. */
    // Ejecuta de como parte de las responsabilidades de esta clase.
    public static String de(HttpServletRequest request) {
        Object valor = request != null ? request.getAttribute(ATRIBUTO) : null;
        return valor instanceof String s ? s : MDC.get(MDC_KEY);
    }
}
