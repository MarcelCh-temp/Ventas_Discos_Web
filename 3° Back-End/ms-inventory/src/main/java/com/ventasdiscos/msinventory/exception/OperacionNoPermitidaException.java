package com.ventasdiscos.msinventory.exception;

import org.springframework.http.HttpStatus;

/** 409: la operación choca con algo ya registrado (ej. operacionId reutilizada con otro contenido). */
public class OperacionNoPermitidaException extends ApiException {
    public OperacionNoPermitidaException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
