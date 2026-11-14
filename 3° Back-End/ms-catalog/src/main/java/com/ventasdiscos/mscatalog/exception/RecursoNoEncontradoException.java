package com.ventasdiscos.mscatalog.exception;

import org.springframework.http.HttpStatus;

/** 404: el recurso pedido no existe. */
public class RecursoNoEncontradoException extends ApiException {
    // Ejecuta recurso no encontrado exception como parte de las responsabilidades de esta clase.
    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
