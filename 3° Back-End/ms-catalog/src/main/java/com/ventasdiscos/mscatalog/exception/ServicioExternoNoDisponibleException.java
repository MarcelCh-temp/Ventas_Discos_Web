package com.ventasdiscos.mscatalog.exception;

import org.springframework.http.HttpStatus;

/** 503: un MS del que dependemos no respondió (resiliencia). */
public class ServicioExternoNoDisponibleException extends ApiException {
    public ServicioExternoNoDisponibleException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }
}
