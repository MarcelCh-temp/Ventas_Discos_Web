package com.ventasdiscos.msinventory.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
 * Traduce TODAS las excepciones a un JSON coherente (ApiError).
 * Para qué: Controller y Service no se llenan de try/catch; el cliente siempre
 * recibe código HTTP correcto + mensaje claro (RNF-04, RNF-16).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Excepciones propias del negocio: cada una ya trae su código HTTP. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getMessage(), req, ex.getErrores());
    }

    /** @Valid falló en el DTO: devolvemos campo -> mensaje para corregir sin perder lo válido. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud", req, errores);
    }

    /** JSON mal formado o tipo incorrecto (ej. texto donde va número). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido: revisa el formato y el tipo de cada dato", req, null);
    }

    /** Parámetro de URL con tipo incorrecto (ej. /products/abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTipo(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido", req, null);
    }

    /** 2.ª barrera: constraint UNIQUE de la BD (por si dos peticiones simultáneas pasan la validación). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violación de integridad en {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "La operación entra en conflicto con datos ya registrados", req, null);
    }

    /** Errores de Spring MVC (404 de ruta, 405 método, parámetro faltante) y cualquier error no previsto (500). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            String detalle = er.getBody().getDetail() != null ? er.getBody().getDetail() : status.getReasonPhrase();
            return build(status, detalle, req, null);
        }
        // Detalle técnico solo en el log; al cliente, mensaje seguro (observabilidad).
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intenta nuevamente", req, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String mensaje, HttpServletRequest req,
                                           Map<String, String> errores) {
        ApiError body = new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                mensaje, req.getRequestURI(), errores);
        return ResponseEntity.status(status).body(body);
    }
}
