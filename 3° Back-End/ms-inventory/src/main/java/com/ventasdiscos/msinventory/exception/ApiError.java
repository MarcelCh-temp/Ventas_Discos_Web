package com.ventasdiscos.msinventory.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de error de la API.
 * "errores" lista campo -> qué corregir (RNF-04); es null cuando no aplica.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        Map<String, String> errores
) {
}
