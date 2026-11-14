package ms_soporte.ms_soporte.exception;

// Excepción específica: representa un fallo o rechazo que la API debe manejar. Clase RecursoNoEncontradoException.
public class RecursoNoEncontradoException extends RuntimeException {
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método RecursoNoEncontradoException (recurso no encontrado exception).
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}