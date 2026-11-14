package com.ventasdiscos.mspostsale.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Lote 4: dato que no cumple una regla de negocio (producto no comprado, cantidad mayor a la comprada) -> 400. */
public class ValidacionNegocioException extends ApiException {
    // Ejecuta validacion negocio exception como parte de las responsabilidades de esta clase.
    public ValidacionNegocioException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }

    // Ejecuta validacion negocio exception como parte de las responsabilidades de esta clase.
    public ValidacionNegocioException(String mensaje, Map<String, String> errores) {
        super(HttpStatus.BAD_REQUEST, mensaje, errores);
    }
}
