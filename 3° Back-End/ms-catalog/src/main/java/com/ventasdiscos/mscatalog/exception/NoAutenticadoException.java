package com.ventasdiscos.mscatalog.exception;

import org.springframework.http.HttpStatus;

/** 401: falta token o es inválido/expirado (RNF-06). */
public class NoAutenticadoException extends ApiException {
    public NoAutenticadoException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
