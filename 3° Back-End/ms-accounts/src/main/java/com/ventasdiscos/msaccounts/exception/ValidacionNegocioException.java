package com.ventasdiscos.msaccounts.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** 400: regla que necesita comparar/consultar datos (no se puede con anotaciones del DTO). */
public class ValidacionNegocioException extends ApiException {
    public ValidacionNegocioException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** Permite indicar varios campos a corregir a la vez (RNF-04). */
    public ValidacionNegocioException(String mensaje, Map<String, String> errores) {
        super(HttpStatus.BAD_REQUEST, mensaje, errores);
    }
}
