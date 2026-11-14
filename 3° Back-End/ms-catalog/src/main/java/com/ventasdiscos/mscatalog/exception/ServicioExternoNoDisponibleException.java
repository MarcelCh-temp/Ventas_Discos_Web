package com.ventasdiscos.mscatalog.exception;

import org.springframework.http.HttpStatus;

/** 503: un MS del que dependemos no respondió (resiliencia). */
public class ServicioExternoNoDisponibleException extends ApiException {
    // Ejecuta servicio externo no disponible exception como parte de las responsabilidades de esta clase.
    public ServicioExternoNoDisponibleException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }
}
