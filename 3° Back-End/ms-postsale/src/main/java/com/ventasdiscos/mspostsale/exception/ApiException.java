package com.ventasdiscos.mspostsale.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Lote 4 (copia de ms-ordenes): base de los errores de negocio (mismo criterio que ms-catalog). El Service lanza la excepción con su
 * código HTTP y, si corresponde, el detalle por campo; GlobalExceptionHandler la convierte en ApiError.
 */
public abstract class ApiException extends RuntimeException {

    // Atributo status: dato o dependencia utilizada por esta clase.
    private final HttpStatus status;
    // Atributo errores: dato o dependencia utilizada por esta clase.
    private final transient Map<String, String> errores;

    // Ejecuta api exception como parte de las responsabilidades de esta clase.
    protected ApiException(HttpStatus status, String mensaje) {
        this(status, mensaje, null);
    }

    // Ejecuta api exception como parte de las responsabilidades de esta clase.
    protected ApiException(HttpStatus status, String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.status = status;
        this.errores = errores;
    }

    // Ejecuta get status como parte de las responsabilidades de esta clase.
    public HttpStatus getStatus() {
        return status;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getErrores (consultar errores).
    public Map<String, String> getErrores() {
        return errores;
    }
}
