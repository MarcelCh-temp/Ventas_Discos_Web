package com.ventasdiscos.msinventory.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** 409: una salida dejaría el stock bajo cero (RN-10). "errores" indica qué fila del lote falló. */
public class StockInsuficienteException extends ApiException {
    public StockInsuficienteException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }

    public StockInsuficienteException(String mensaje, Map<String, String> errores) {
        super(HttpStatus.CONFLICT, mensaje, errores);
    }
}
