package com.ventasdiscos.msaccounts.model;

/**
 * Lote 5 · UML 01 (HU-78, RF-CTA-08, RN-37, RN-44). ACTIVA = puede operar. EN_ELIMINACION = el Cliente confirmó la
 * eliminación con su contraseña y no puede iniciar sesión ni hacer nuevas operaciones mientras se verifica y depura.
 * ELIMINADA = proceso completo; datos personales anonimizados (final).
 */
public enum EstadoCuenta {
    ACTIVA,
    EN_ELIMINACION,
    ELIMINADA
}
