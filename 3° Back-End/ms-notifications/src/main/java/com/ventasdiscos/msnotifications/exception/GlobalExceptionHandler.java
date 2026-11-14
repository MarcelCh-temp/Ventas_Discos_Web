package com.ventasdiscos.msnotifications.exception;

import com.ventasdiscos.msnotifications.trazabilidad.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
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
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones a un JSON coherente (ApiError), con el mismo criterio de ms-catalog y ms-accounts.
 * Solo convierte excepciones en respuestas HTTP; las reglas siguen en Service/Model (CSR).
 * Lote 1.1: antes usaba ErrorResponse {codigo, mensaje, campo, timestamp}. Se conservan los mismos
 * casos y códigos HTTP que ya manejaba el equipo; solo cambia el formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Notificación inexistente o de otro usuario (HU-54: no se revela que existe). */
    @ExceptionHandler(NotificacionNoEncontradaException.class)
    // Ejecuta handle no encontrada como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleNoEncontrada(NotificacionNoEncontradaException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    /** @Valid falló en el DTO (evento interno): campo -> mensaje (RNF-04). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    // Ejecuta handle validacion como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleValidacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud", req, errores);
    }

    /** Restricciones en parámetros del Controller (ej. @Positive en el id de la ruta). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleValidacionParametros(HandlerMethodValidationException ex,
                                                              HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(r -> r.getResolvableErrors().forEach(e ->
                errores.putIfAbsent(r.getMethodParameter().getParameterName(), e.getDefaultMessage())));
        return build(HttpStatus.BAD_REQUEST, "Uno o más parámetros no cumplen las restricciones", req,
                errores.isEmpty() ? null : errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método handleRestricciones (handle restricciones).
    public ResponseEntity<ApiError> handleRestricciones(ConstraintViolationException ex, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            errores.putIfAbsent(v.getPropertyPath().toString(), v.getMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Uno o más valores no cumplen las restricciones", req,
                errores.isEmpty() ? null : errores);
    }

    /** Parámetro con tipo o valor incorrecto (ej. ?type=INVENTADO o una fecha mal escrita). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    // Ejecuta handle tipo como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleTipo(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido", req, null);
    }

    /** JSON mal formado o tipo de notificación inexistente en el evento. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    // Ejecuta handle json como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido: revisa el formato y el tipo de cada dato", req, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    // Ejecuta handle response status como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return build(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(), req, null);
    }

    /** Dato inválido detectado en el Service (ej. fecha inicial mayor que la final, HU-55). */
    @ExceptionHandler(IllegalArgumentException.class)
    // Ejecuta handle argumento como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleArgumento(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(NoAutenticadoException.class)
    // Ejecuta handle no autenticado como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleNoAutenticado(NoAutenticadoException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, null);
    }

    /** Cierre (RN-44): MS-Accounts no confirmó el estado de la cuenta; nunca se asume ACTIVA. */
    @ExceptionHandler(ServicioNoDisponibleException.class)
    // Ejecuta handle no disponible como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleNoDisponible(ServicioNoDisponibleException ex, HttpServletRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req, null);
    }

    /** Lote 6 (DOC-05): token de servicio válido, pero el servicio emisor no está autorizado para la ruta. */
    @ExceptionHandler(AccesoDenegadoException.class)
    // Ejecuta handle acceso denegado como parte de las responsabilidades de esta clase.
    public ResponseEntity<ApiError> handleAccesoDenegado(AccesoDenegadoException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req, null);
    }

    /**
     * Conflicto con una restricción de la BD que NO es un evento repetido (los repetidos, incluso
     * simultáneos, los resuelve EventoNotificacionService y responden 200).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violación de integridad en {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "La operación entra en conflicto con datos ya registrados", req, null);
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
