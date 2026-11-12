package com.ventasdiscos.mspostsale.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Lote 4: el estado actual no permite la acción (pedido ya despachado, solicitud ya resuelta, duplicada) -> 409. */
public class OperacionNoPermitidaException extends ApiException {
    public OperacionNoPermitidaException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }

    public OperacionNoPermitidaException(String mensaje, Map<String, String> errores) {
        super(HttpStatus.CONFLICT, mensaje, errores);
    }
}
