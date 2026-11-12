package com.ventasdiscos.msaccounts.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lote 6 · DOC-05: servicios autorizados para invocar una ruta interna (autorización por servicio y ruta).
 * Se pone en cada método de los controllers internos. Una ruta interna SIN esta anotación se rechaza (403):
 * si alguien agrega una ruta nueva y olvida declarar quién la puede usar, queda cerrada por defecto.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ServiciosPermitidos {

    /** Identificadores de {@link Servicios} (ej. Servicios.ACCOUNTS). */
    String[] value();
}
