package com.ventasdiscos.msnotifications.exception;

/** Lote 1: 401, falta el token o es inválido/expiró. */
public class NoAutenticadoException extends RuntimeException {

    // Ejecuta no autenticado exception como parte de las responsabilidades de esta clase.
    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
