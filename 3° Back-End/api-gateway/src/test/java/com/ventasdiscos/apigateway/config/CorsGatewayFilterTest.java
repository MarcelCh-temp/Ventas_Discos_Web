package com.ventasdiscos.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lote 6 · Decisión 2/4: CORS con orígenes exactos y sin comodines. */
class CorsGatewayFilterTest {

    @Test
    void usaSoloLosOrigenesConfigurados() {
        CorsConfiguration c = CorsGatewayFilter.fuente(new String[]{" http://localhost:5500 ", "http://127.0.0.1:5500", ""})
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/products"));
        assertEquals(List.of("http://localhost:5500", "http://127.0.0.1:5500"), c.getAllowedOrigins());
        assertTrue(c.getAllowedHeaders().containsAll(List.of("Authorization", "X-Correlation-Id")));
        assertTrue(c.getExposedHeaders().contains("X-Correlation-Id"));
        assertTrue(c.getAllowedMethods().containsAll(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")));
        assertFalse(Boolean.TRUE.equals(c.getAllowCredentials()));
        assertEquals("http://localhost:5500", c.checkOrigin("http://localhost:5500"));
        assertEquals(null, c.checkOrigin("http://localhost:3000"));
        assertEquals(null, c.checkOrigin("http://sitio-cualquiera.com"));
    }

    @Test
    void comodinesOListaVacia_noArranca() {
        assertThrows(IllegalStateException.class, () -> CorsGatewayFilter.fuente(new String[]{"*"}));
        assertThrows(IllegalStateException.class, () -> CorsGatewayFilter.fuente(new String[]{"http://localhost:*"}));
        assertThrows(IllegalStateException.class, () -> CorsGatewayFilter.fuente(new String[]{" "}));
        assertThrows(IllegalStateException.class, () -> CorsGatewayFilter.fuente(null));
    }
}
