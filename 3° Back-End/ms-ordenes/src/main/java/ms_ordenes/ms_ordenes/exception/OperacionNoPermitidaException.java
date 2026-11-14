package ms_ordenes.ms_ordenes.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Lote 3: el estado actual no permite la acción (transición inválida, sin stock, total cambiado) -> 409. */
public class OperacionNoPermitidaException extends ApiException {
    // Ejecuta operacion no permitida exception como parte de las responsabilidades de esta clase.
    public OperacionNoPermitidaException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }

    // Ejecuta operacion no permitida exception como parte de las responsabilidades de esta clase.
    public OperacionNoPermitidaException(String mensaje, Map<String, String> errores) {
        super(HttpStatus.CONFLICT, mensaje, errores);
    }
}
