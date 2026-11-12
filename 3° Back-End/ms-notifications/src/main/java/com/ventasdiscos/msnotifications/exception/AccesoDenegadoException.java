package com.ventasdiscos.msnotifications.exception;

/** Lote 6 (DOC-05): 403, el servicio que llama está autenticado pero no autorizado para la ruta interna. */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
