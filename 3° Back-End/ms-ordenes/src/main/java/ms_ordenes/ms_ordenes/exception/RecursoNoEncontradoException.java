package ms_ordenes.ms_ordenes.exception;

import org.springframework.http.HttpStatus;

/**
 * 404. Clase original; Lote 3: ahora extiende ApiException (mismo comportamiento).
 * También se usa para un pedido AJENO: se responde como inexistente para no revelar que existe (T-09).
 */
public class RecursoNoEncontradoException extends ApiException {
    // Ejecuta recurso no encontrado exception como parte de las responsabilidades de esta clase.
    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
