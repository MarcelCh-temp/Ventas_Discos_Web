package com.ventasdiscos.msaccounts.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Rama del módulo 11 donde el DV resulta 0. */
class RutValidatorDvCeroTest {

    @Test
    void dvCero() {
        // 11 - (suma % 11) = 11 -> DV '0'
        assertEquals('0', RutValidator.calcularDv("10000004"));
    }
}
