
package com.ventasdiscos.msnotifications.exception;

// Excepción específica: representa un fallo o rechazo que la API debe manejar. Clase NotificacionNoEncontradaException.
public class NotificacionNoEncontradaException
        extends RuntimeException {

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método NotificacionNoEncontradaException (notificacion no encontrada exception).
    public NotificacionNoEncontradaException() {
        super("Notificacion no encontrada");
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método NotificacionNoEncontradaException (notificacion no encontrada exception).
    public NotificacionNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
