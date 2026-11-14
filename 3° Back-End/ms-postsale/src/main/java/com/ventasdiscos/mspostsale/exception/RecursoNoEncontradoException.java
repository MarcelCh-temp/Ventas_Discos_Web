package com.ventasdiscos.mspostsale.exception;

import org.springframework.http.HttpStatus;

/**
 * Lote 4 (copia de ms-ordenes): 404.
 * También se usa para un pedido AJENO: se responde como inexistente para no revelar que existe (T-09).
 */
public class RecursoNoEncontradoException extends ApiException {
    // Ejecuta recurso no encontrado exception como parte de las responsabilidades de esta clase.
    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
