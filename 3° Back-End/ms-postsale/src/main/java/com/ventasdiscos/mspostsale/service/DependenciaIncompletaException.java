package com.ventasdiscos.mspostsale.service;

public class DependenciaIncompletaException extends RuntimeException {

    public DependenciaIncompletaException(String mensaje) {
        super(mensaje);
    }
}
