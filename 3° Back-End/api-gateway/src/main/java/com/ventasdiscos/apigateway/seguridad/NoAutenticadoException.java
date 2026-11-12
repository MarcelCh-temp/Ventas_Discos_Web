package com.ventasdiscos.apigateway.seguridad;

/** Lote 6: el JWT de usuario falta, es inválido o expiró → 401. */
public class NoAutenticadoException extends RuntimeException {
    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
