package ms_soporte.ms_soporte.exception;

/** Lote 1: 403, el usuario está autenticado pero no tiene el rol necesario. */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
