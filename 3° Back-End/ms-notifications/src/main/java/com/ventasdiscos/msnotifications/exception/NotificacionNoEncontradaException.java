
package com.ventasdiscos.msnotifications.exception;

public class NotificacionNoEncontradaException
        extends RuntimeException {

    public NotificacionNoEncontradaException() {
        super("Notificacion no encontrada");
    }

    public NotificacionNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
