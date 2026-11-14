package ms_ordenes.ms_ordenes.exception;

import jakarta.servlet.http.HttpServletRequest;
import ms_ordenes.ms_ordenes.trazabilidad.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones a un JSON coherente (ApiError), con el mismo criterio de ms-catalog y ms-accounts.
 * Solo convierte excepciones en respuestas HTTP; las reglas siguen en Service/Model (CSR).
 * Lote 1.1: antes respondía {"error": "..."} o un mapa de campos suelto.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Lote 3: errores de negocio (400, 401, 402, 403, 404, 409, 503) con su código y el detalle por campo. */
    @ExceptionHandler(ApiException.class)
    // Ejecuta handle api como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getMessage(), req, ex.getErrores());
    }

    /** Lote 3: dos operaciones simultáneas chocaron con una clave única (por ejemplo, el primer carrito) -> 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    // Ejecuta handle integridad como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleIntegridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Conflicto de integridad en {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "La operación chocó con otra simultánea. Intenta nuevamente", req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // Ejecuta handle validacion como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleValidacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud", req, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método handleJson (handle json).
    public ResponseEntity<ApiError> handleJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido: revisa el formato y el tipo de cada dato", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método handleTipo (handle tipo).
    public ResponseEntity<ApiError> handleTipo(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido", req, null);
    }

    /** Transición de estado no permitida (EstadoOrden.esTransicionValida; RN-39) -> 409. */
    @ExceptionHandler(IllegalStateException.class)
    // Ejecuta handle estado como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleEstado(IllegalStateException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** Errores de Spring MVC (404 de ruta, 405 método, parámetro faltante) y cualquier error no previsto (500). */
    @ExceptionHandler(Exception.class)
    // Ejecuta handle general como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            String detalle = er.getBody().getDetail() != null ? er.getBody().getDetail() : status.getReasonPhrase();
            return build(status, detalle, req, null);
        }
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intenta nuevamente", req, null);
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método build (build).
    private ResponseEntity<ApiError> build(HttpStatus status, String mensaje, HttpServletRequest req,
                                           Map<String, String> errores) {
        ApiError body = new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                mensaje, req.getRequestURI(), errores, CorrelationId.de(req));
        return ResponseEntity.status(status).body(body);
    }
}
