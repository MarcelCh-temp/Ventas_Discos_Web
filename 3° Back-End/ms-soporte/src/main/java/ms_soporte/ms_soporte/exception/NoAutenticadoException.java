package ms_soporte.ms_soporte.exception;

/** Lote 1: 401, falta el token o es inválido/expiró. */
public class NoAutenticadoException extends RuntimeException {
    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
