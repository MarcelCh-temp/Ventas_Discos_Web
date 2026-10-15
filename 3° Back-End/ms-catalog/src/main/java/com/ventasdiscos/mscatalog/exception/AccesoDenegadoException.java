package com.ventasdiscos.mscatalog.exception;

import org.springframework.http.HttpStatus;

/** 403: autenticado, pero su rol no permite la acción (RN-04). */
public class AccesoDenegadoException extends ApiException {
    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
