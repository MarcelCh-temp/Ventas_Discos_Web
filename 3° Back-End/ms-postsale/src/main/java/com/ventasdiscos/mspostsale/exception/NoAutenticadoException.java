package com.ventasdiscos.mspostsale.exception;

import org.springframework.http.HttpStatus;

/** Lote 4 (igual que ms-ordenes): falta el token o no es válido -> 401. */
public class NoAutenticadoException extends ApiException {
    // Ejecuta no autenticado exception como parte de las responsabilidades de esta clase.
    public NoAutenticadoException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
