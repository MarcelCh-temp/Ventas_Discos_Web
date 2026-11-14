package ms_soporte.ms_soporte.exception;

/**
 * Lote 5 (integración de HU-78): MS-Accounts no respondió y no se pudo confirmar que la cuenta esté ACTIVA -> 503.
 * No se crea la solicitud (RN-37, RN-44).
 */
public class ServicioNoDisponibleException extends RuntimeException {
    // Ejecuta servicio no disponible exception como parte de las responsabilidades de esta clase.
    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
