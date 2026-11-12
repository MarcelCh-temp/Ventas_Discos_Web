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

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String ATRIBUTO = CorrelationId.class.getName();
    private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9-]{8,64}");

    private CorrelationId() {
    }

    public static boolean esValido(String valor) {
        return valor != null && VALIDO.matcher(valor).matches();
    }

    public static String nuevo() {
        return UUID.randomUUID().toString();
    }

    /** El de la petición (atributo) o, si no hay, el del hilo actual. */
    public static String de(HttpServletRequest request) {
        Object valor = request != null ? request.getAttribute(ATRIBUTO) : null;
        return valor instanceof String s ? s : MDC.get(MDC_KEY);
    }
}
