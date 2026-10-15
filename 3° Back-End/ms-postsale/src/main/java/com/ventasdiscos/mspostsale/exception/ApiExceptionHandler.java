package com.ventasdiscos.mspostsale.exception;

import com.ventasdiscos.mspostsale.service.DependenciaIncompletaException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.client.RestClientException;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)

    public ResponseEntity<?> noExiste(NoSuchElementException ex) {
        return respuesta(404,ex.getMessage());
    }
    @ExceptionHandler
    ( {
        IllegalArgumentException.class,MethodArgumentNotValidException.class,ConstraintViolationException.class,HandlerMethodValidationException.class
    }
    )

    public ResponseEntity<?> invalido(Exception ex) {
        return respuesta(400,ex.getMessage());
    }
    @ExceptionHandler(IllegalStateException.class)

    public ResponseEntity<?> conflicto(IllegalStateException ex) {
        return respuesta(409,ex.getMessage());
    }
    @ExceptionHandler
    ( {
        DependenciaIncompletaException.class,RestClientException.class
    }
    )

    public ResponseEntity<?> dependencia(Exception ex) {
        return respuesta(503,ex.getMessage());
    }

    private ResponseEntity<?> respuesta(int codigo,String mensaje) {
        return ResponseEntity.status(codigo).body(Map.of("mensaje",mensaje==null?"Solicitud no valida":mensaje));
    }
}
