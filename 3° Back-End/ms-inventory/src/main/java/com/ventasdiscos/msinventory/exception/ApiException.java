package com.ventasdiscos.msinventory.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Base de las excepciones de negocio: cada subclase define su código HTTP.
 * Para qué: el Service solo "lanza" y GlobalExceptionHandler traduce a JSON (RNF-04).
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> errores;

    protected ApiException(HttpStatus status, String mensaje) {
        this(status, mensaje, null);
    }

    protected ApiException(HttpStatus status, String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.status = status;
        this.errores = errores;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
