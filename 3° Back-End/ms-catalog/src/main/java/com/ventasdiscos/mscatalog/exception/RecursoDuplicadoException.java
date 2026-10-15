package com.ventasdiscos.mscatalog.exception;

import org.springframework.http.HttpStatus;

/** 409: se intentó crear un dato que debe ser único (RN-33). */
public class RecursoDuplicadoException extends ApiException {
    public RecursoDuplicadoException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
