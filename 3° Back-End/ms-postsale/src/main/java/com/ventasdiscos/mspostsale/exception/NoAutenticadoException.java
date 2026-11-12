package com.ventasdiscos.mspostsale.exception;

import org.springframework.http.HttpStatus;

/** Lote 4 (igual que ms-ordenes): falta el token o no es válido -> 401. */
public class NoAutenticadoException extends ApiException {
    public NoAutenticadoException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
