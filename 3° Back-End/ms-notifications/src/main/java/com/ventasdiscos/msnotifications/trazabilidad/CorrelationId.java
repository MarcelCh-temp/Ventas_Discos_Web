package com.ventasdiscos.msnotifications.trazabilidad;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lote 6 · DOC-06: identificador de correlación de una petición (cabecera X-Correlation-Id).
 * Lo genera el API Gateway (o este MS si la petición llegó sin uno válido), viaja en las llamadas MS -> MS, aparece
 * en los logs ([correlationId]) y en el campo "correlationId" de ApiError.
 * Solo se aceptan 8 a 64 letras, números o guiones: así nadie puede inyectar texto arbitrario en los logs.
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

    /** El de la petición en curso (hilo actual), o null. */
    public static String actual() {
        return MDC.get(MDC_KEY);
    }

    /** Para llamadas salientes: el de la petición en curso o uno nuevo (tareas programadas). */
    public static String actualOGenerar() {
        String id = actual();
        return esValido(id) ? id : nuevo();
    }

    /** Para ApiError: el guardado en la petición (sirve también en el despacho de error) o el del hilo. */
    public static String de(HttpServletRequest request) {
        Object valor = request != null ? request.getAttribute(ATRIBUTO) : null;
        return valor instanceof String s ? s : actual();
    }
}
