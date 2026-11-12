package com.ventasdiscos.msnotifications.exception;

/** Cierre (RN-44): 503, no fue posible confirmar con MS-Accounts el estado de la cuenta. */
public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
