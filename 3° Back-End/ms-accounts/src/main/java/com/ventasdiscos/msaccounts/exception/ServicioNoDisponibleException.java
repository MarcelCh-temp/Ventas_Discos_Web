package com.ventasdiscos.msaccounts.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Lote 5: otro MS no respondió y no se puede confirmar algo obligatorio (RN-36: "si una verificación no es
 * concluyente, no se finaliza") -> 503.
 */
public class ServicioNoDisponibleException extends ApiException {
    public ServicioNoDisponibleException(String mensaje, Map<String, String> errores) {
        super(HttpStatus.SERVICE_UNAVAILABLE, mensaje, errores);
    }
}
