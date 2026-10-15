package com.ventasdiscos.msaccounts.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HU-01: el RUT debe ser válido y tener DV correcto. */
class RutValidatorTest {

    @Test
    void aceptaRutValidoConYSinPuntos() {
        assertTrue(RutValidator.esValido("12.345.678-5"));
        assertTrue(RutValidator.esValido("12345678-5"));
        assertTrue(RutValidator.esValido("11111111-1"));
    }

    @Test
    void aceptaDvK() {
        assertTrue(RutValidator.esValido("10000013-K"));
        assertTrue(RutValidator.esValido("10000013-k"));
    }

    @Test
    void rechazaDvIncorrectoYFormatoInvalido() {
        assertFalse(RutValidator.esValido("12345678-9"));
        assertFalse(RutValidator.esValido("abc"));
        assertFalse(RutValidator.esValido(""));
        assertFalse(RutValidator.esValido(null));
    }

    @Test
    void normalizaFormato() {
        assertEquals("12345678-5", RutValidator.normalizar("12.345.678-5"));
        assertEquals("10000013-K", RutValidator.normalizar("10000013k"));
        assertNull(RutValidator.normalizar("12-3"));
    }
}
