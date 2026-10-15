package com.ventasdiscos.mscatalog.exception;

import org.springframework.http.HttpStatus;

/** 410 Gone: producto Retirado al que se accede por URL antigua (RN-07, HU-17). */
public class RecursoRetiradoException extends ApiException {
    public RecursoRetiradoException(String mensaje) {
        super(HttpStatus.GONE, mensaje);
    }
}
