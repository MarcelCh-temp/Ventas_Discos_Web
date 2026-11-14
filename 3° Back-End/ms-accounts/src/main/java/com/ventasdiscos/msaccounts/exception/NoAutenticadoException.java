package com.ventasdiscos.msaccounts.exception;

import org.springframework.http.HttpStatus;

/** 401: falta token o es inválido/expirado (RNF-06). */
public class NoAutenticadoException extends ApiException {
    // Ejecuta no autenticado exception como parte de las responsabilidades de esta clase.
    public NoAutenticadoException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
