package ms_ordenes.ms_ordenes.exception;

import org.springframework.http.HttpStatus;

/** Lote 3 (igual que ms-soporte): el rol no permite la acción -> 403. */
public class AccesoDenegadoException extends ApiException {
    // Ejecuta acceso denegado exception como parte de las responsabilidades de esta clase.
    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
