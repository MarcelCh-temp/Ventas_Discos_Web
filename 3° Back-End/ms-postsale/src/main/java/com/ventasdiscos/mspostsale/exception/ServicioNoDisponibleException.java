package com.ventasdiscos.mspostsale.exception;

import org.springframework.http.HttpStatus;

/** Lote 4: Orders no respondió; sin él no se puede validar el pedido (ERS 11.7) -> 503. Nada cambia en PostSale. */
public class ServicioNoDisponibleException extends ApiException {
    // Ejecuta servicio no disponible exception como parte de las responsabilidades de esta clase.
    public ServicioNoDisponibleException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }
}
