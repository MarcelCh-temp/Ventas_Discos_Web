package com.ventasdiscos.mspostsale.trazabilidad;

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

    /** El de la petición en curso (hilo actual), o null. */
    // Ejecuta actual como parte de las responsabilidades de esta clase.
    public static String actual() {
        return MDC.get(MDC_KEY);
    }

    /** Para llamadas salientes: el de la petición en curso o uno nuevo (tareas programadas). */
    // Ejecuta actual ogenerar como parte de las responsabilidades de esta clase.
    public static String actualOGenerar() {
        String id = actual();
        return esValido(id) ? id : nuevo();
    }

    /** Para ApiError: el guardado en la petición (sirve también en el despacho de error) o el del hilo. */
    // Ejecuta de como parte de las responsabilidades de esta clase.
    public static String de(HttpServletRequest request) {
        Object valor = request != null ? request.getAttribute(ATRIBUTO) : null;
        return valor instanceof String s ? s : actual();
    }
}
