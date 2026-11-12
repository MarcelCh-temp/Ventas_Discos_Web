package com.ventasdiscos.mspostsale.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Lote 4 (copia de ms-ordenes): base de los errores de negocio (mismo criterio que ms-catalog). El Service lanza la excepción con su
 * código HTTP y, si corresponde, el detalle por campo; GlobalExceptionHandler la convierte en ApiError.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final transient Map<String, String> errores;

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
