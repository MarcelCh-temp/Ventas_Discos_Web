package com.ventasdiscos.msaccounts.model;

/**
 * Lote 5 · UML 01 (HU-78, RF-CTA-08, RN-37, RN-44). ACTIVA = puede operar. EN_ELIMINACION = el Cliente confirmó la
 * eliminación con su contraseña y no puede iniciar sesión ni hacer nuevas operaciones mientras se verifica y depura.
 * ELIMINADA = proceso completo; datos personales anonimizados (final).
 */
public enum EstadoCuenta {
    // Opción ACTIVA: representa el estado o tipo «activa» permitido en este dominio.
    ACTIVA,
    // Opción EN_ELIMINACION: representa el estado o tipo «en eliminacion» permitido en este dominio.
    EN_ELIMINACION,
    // Opción ELIMINADA: representa el estado o tipo «eliminada» permitido en este dominio.
    ELIMINADA
}
