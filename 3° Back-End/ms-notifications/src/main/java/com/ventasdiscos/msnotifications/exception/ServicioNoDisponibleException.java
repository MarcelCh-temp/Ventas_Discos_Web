package com.ventasdiscos.msnotifications.exception;

/** Cierre (RN-44): 503, no fue posible confirmar con MS-Accounts el estado de la cuenta. */
public class ServicioNoDisponibleException extends RuntimeException {
    // Ejecuta servicio no disponible exception como parte de las responsabilidades de esta clase.
    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
