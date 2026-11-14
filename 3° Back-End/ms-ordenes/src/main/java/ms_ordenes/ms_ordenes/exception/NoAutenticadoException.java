package ms_ordenes.ms_ordenes.exception;

import org.springframework.http.HttpStatus;

/** Lote 3 (igual que ms-soporte): falta el token o no es válido -> 401. */
public class NoAutenticadoException extends ApiException {
    // Ejecuta no autenticado exception como parte de las responsabilidades de esta clase.
    public NoAutenticadoException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
