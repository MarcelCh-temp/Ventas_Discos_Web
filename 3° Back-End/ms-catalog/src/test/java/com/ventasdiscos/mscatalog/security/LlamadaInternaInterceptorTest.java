package com.ventasdiscos.mscatalog.security;

import com.nimbusds.jwt.SignedJWT;
import com.ventasdiscos.mscatalog.trazabilidad.CorrelationId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lote 6: cada llamada saliente lleva JWT de servicio (solo a rutas internas) y X-Correlation-Id. */
class LlamadaInternaInterceptorTest {

    private final ServicioJwtService servicioJwt = new ServicioJwtService(
            "secreto-de-servicio-de-prueba-de-32-o-mas", "secreto-de-usuarios-de-prueba-de-32-o-mas");
    private final LlamadaInternaInterceptor interceptor = new LlamadaInternaInterceptor(servicioJwt);
    private final ClientHttpRequestExecution ok = (req, body) -> new MockClientHttpResponse(new byte[0], HttpStatus.OK);

    @AfterEach
    void limpiar() {
        MDC.remove(CorrelationId.MDC_KEY);
    }

    @Test
    void rutaInternaLlevaTokenNuevoParaElDestinoYElCorrelationIdActual() throws Exception {
        MDC.put(CorrelationId.MDC_KEY, "peticion-12345678");
        MockClientHttpRequest req = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("http://127.0.0.1:8081/api/v1/internal/accounts/5/operation-status"));
        interceptor.intercept(req, new byte[0], ok);
        String auth = req.getHeaders().getFirst("Authorization");
        assertTrue(auth != null && auth.startsWith("Bearer "));
        SignedJWT jwt = SignedJWT.parse(auth.substring(7));
        assertEquals(Servicios.PROPIO, jwt.getJWTClaimsSet().getIssuer());
        assertEquals(List.of(Servicios.ACCOUNTS), jwt.getJWTClaimsSet().getAudience());
        assertEquals("peticion-12345678", req.getHeaders().getFirst(CorrelationId.HEADER));
    }

    @Test
    void rutaNoInternaNoLlevaTokenDeServicio() throws Exception {
        MockClientHttpRequest req = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://127.0.0.1:8082/api/v1/products"));
        interceptor.intercept(req, new byte[0], ok);
        assertNull(req.getHeaders().getFirst("Authorization"));
        // Sin petición en curso (tarea programada): igual viaja un correlationId válido
        assertTrue(CorrelationId.esValido(req.getHeaders().getFirst(CorrelationId.HEADER)));
    }

    @Test
    void rutaInternaSinDuenoConocidoNoSeEnvia() {
        AtomicBoolean enviada = new AtomicBoolean(false);
        MockClientHttpRequest req = new MockClientHttpRequest(HttpMethod.POST, URI.create("http://127.0.0.1:8083/api/v1/internal/otra/cosa"));
        assertThrows(IOException.class, () -> interceptor.intercept(req, new byte[0], (r, b) -> {
            enviada.set(true);
            return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        }));
        assertFalse(enviada.get());
    }
}
