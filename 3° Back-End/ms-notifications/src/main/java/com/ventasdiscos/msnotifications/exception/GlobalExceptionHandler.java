
package com.ventasdiscos.msnotifications.exception;

import com.ventasdiscos.msnotifications.dto.ErrorResponse;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotificacionNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrada(
            NotificacionNoEncontradaException ex
    ) {
        return respuesta(
                HttpStatus.NOT_FOUND,
                "NOTIFICACION_NO_ENCONTRADA",
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacionDTO(
            MethodArgumentNotValidException ex
    ) {
        var error = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst();

        return respuesta(
                HttpStatus.BAD_REQUEST,
                "VALIDACION_FALLIDA",
                error.map(e -> e.getDefaultMessage())
                        .orElse("Los datos ingresados no son validos"),
                error.map(e -> e.getField())
                        .orElse(null)
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> manejarValidacionParametros(
            HandlerMethodValidationException ex
    ) {
        return respuesta(
                HttpStatus.BAD_REQUEST,
                "VALIDACION_FALLIDA",
                "Uno o mas parametros no cumplen las restricciones",
                null
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> manejarRestrcciones(
            ConstraintViolationException ex
    ) {
        return respuesta(
                HttpStatus.BAD_REQUEST,
                "VALIDACION_FALLIDA",
                "Uno o mas valores no cumplen las restricciones",
                null
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoInvalido(
            MethodArgumentTypeMismatchException ex
    ) {
        return respuesta(
                HttpStatus.BAD_REQUEST,
                "PARAMETRO_INVALIDO",
                "El parametro tiene un tipo o valor incorrecto",
                ex.getName()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoInvalido(
            HttpMessageNotReadableException ex
    ) {
        return respuesta(
                HttpStatus.BAD_REQUEST,
                "CUERPO_INVALIDO",
                "El cuerpo de la solicitud no es valido",
                null
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> manejarErrorHTTP(
            ResponseStatusException ex
    ) {
        ErrorResponse cuerpo = new ErrorResponse(
                "ERROR_HTTP",
                ex.getReason() != null
                        ? ex.getReason()
                        : "Solicitud rechazada",
                null,
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(cuerpo);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumentosInvalidos(
            IllegalArgumentException ex
    ) {
        return respuesta(
                HttpStatus.BAD_REQUEST,
                "DATOS_INVALIDOS",
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarGeneral(
            Exception ex
    ) {
        return respuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ERROR_INTERNO",
                "Ocurrio un error interno",
                null
        );
    }

    private ResponseEntity<ErrorResponse> respuesta(
            HttpStatus estado,
            String codigo,
            String mensaje,
            String campo
    ) {
        ErrorResponse cuerpo = new ErrorResponse(
                codigo,
                mensaje,
                campo,
                LocalDateTime.now()
        );

        return ResponseEntity.status(estado).body(cuerpo);
    }
}
