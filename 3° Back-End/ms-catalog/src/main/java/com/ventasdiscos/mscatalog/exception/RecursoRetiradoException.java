package com.ventasdiscos.mscatalog.exception;

import org.springframework.http.HttpStatus;

/** 410 Gone: producto Retirado al que se accede por URL antigua (RN-07, HU-17). */
public class RecursoRetiradoException extends ApiException {
    // Ejecuta recurso retirado exception como parte de las responsabilidades de esta clase.
    public RecursoRetiradoException(String mensaje) {
        super(HttpStatus.GONE, mensaje);
    }
}
