package com.ventasdiscos.mspostsale.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de error de la API (el mismo de ms-accounts, ms-catalog y ms-inventory).
 * "errores" lista campo -> qué corregir (RNF-04); es null cuando no aplica.
 * Lote 4: se adopta en MS-PostSale (antes respondía {"mensaje": "..."}), igual que en los Lotes 1 a 3.
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
