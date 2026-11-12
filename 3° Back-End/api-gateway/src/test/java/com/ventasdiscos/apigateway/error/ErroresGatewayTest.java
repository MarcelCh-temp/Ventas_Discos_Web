package com.ventasdiscos.apigateway.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lote 6 · DOC-06: formato ApiError del Gateway y estado HTTP ante fallas de reenvío. */
class ErroresGatewayTest {

    @Test
    void apiErrorConTodosLosCamposYTextoEscapado() {
        String json = ApiErrorJson.json(HttpStatus.UNAUTHORIZED, "Token \"inválido\"\n", "/api/v1/orders/me", "abc-12345678");
        assertTrue(json.contains("\"status\":401"));
        assertTrue(json.contains("\"error\":\"Unauthorized\""));
        assertTrue(json.contains("\"mensaje\":\"Token \\\"inválido\\\"\\n\""));
        assertTrue(json.contains("\"path\":\"/api/v1/orders/me\""));
        assertTrue(json.contains("\"errores\":null"));
        assertTrue(json.contains("\"correlationId\":\"abc-12345678\""));
        assertTrue(json.contains("\"timestamp\":\""));
        assertEquals("null", ApiErrorJson.texto(null));
        assertEquals("\"\\u0001\"", ApiErrorJson.texto("\u0001"));
    }

    @Test
    void clasificaLaCausaDelFallo() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                FalloReenvioFilter.clasificar(new ResourceAccessException("x", new ConnectException("refused"))));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT,
                FalloReenvioFilter.clasificar(new ResourceAccessException("x", new SocketTimeoutException("read"))));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT,
                FalloReenvioFilter.clasificar(new ResourceAccessException("x", new HttpTimeoutException("request timed out"))));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                FalloReenvioFilter.clasificar(new ResourceAccessException("x", new HttpConnectTimeoutException("connect"))));
        assertEquals(HttpStatus.BAD_GATEWAY, FalloReenvioFilter.clasificar(new IOException("otra")));
        assertEquals(HttpStatus.BAD_GATEWAY, FalloReenvioFilter.clasificar(new IllegalStateException("x")));
    }
}
