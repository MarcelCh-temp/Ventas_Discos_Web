package com.ventasdiscos.msaccounts.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de error de la API.
 * "errores" lista campo -> qué corregir (RNF-04); es null cuando no aplica.
 */
// Lote 6 · DOC-06: se agrega correlationId al final; los campos anteriores no cambian.
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        Map<String, String> errores,
        String correlationId
) {
}
