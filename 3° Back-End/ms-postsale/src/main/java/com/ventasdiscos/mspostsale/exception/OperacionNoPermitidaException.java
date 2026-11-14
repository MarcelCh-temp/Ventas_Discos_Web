package com.ventasdiscos.mspostsale.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Lote 4: el estado actual no permite la acción (pedido ya despachado, solicitud ya resuelta, duplicada) -> 409. */
public class OperacionNoPermitidaException extends ApiException {
    // Ejecuta operacion no permitida exception como parte de las responsabilidades de esta clase.
    public OperacionNoPermitidaException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }

    // Ejecuta operacion no permitida exception como parte de las responsabilidades de esta clase.
    public OperacionNoPermitidaException(String mensaje, Map<String, String> errores) {
        super(HttpStatus.CONFLICT, mensaje, errores);
    }
}
