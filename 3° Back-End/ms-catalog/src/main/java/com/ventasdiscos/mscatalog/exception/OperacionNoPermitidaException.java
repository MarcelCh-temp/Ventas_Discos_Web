package com.ventasdiscos.mscatalog.exception;

import org.springframework.http.HttpStatus;

/** 409: la operación choca con el estado actual del recurso. */
public class OperacionNoPermitidaException extends ApiException {
    public OperacionNoPermitidaException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
