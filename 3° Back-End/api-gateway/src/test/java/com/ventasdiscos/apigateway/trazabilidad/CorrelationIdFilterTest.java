package com.ventasdiscos.apigateway.trazabilidad;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lote 6 · DOC-06: el Gateway conserva un X-Correlation-Id válido, genera uno si falta y limpia el MDC. */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    private record Visto(String header, String mdc, long nombres) {
    }

    private Visto ejecutar(MockHttpServletRequest req, MockHttpServletResponse res) throws Exception {
        AtomicReference<Visto> visto = new AtomicReference<>();
        filtro.doFilter(req, res, (r, s) -> {
            HttpServletRequest h = (HttpServletRequest) r;
            long nombres = Collections.list(h.getHeaderNames()).stream()
                    .filter(CorrelationId.HEADER::equalsIgnoreCase).count();
            visto.set(new Visto(h.getHeader(CorrelationId.HEADER), MDC.get(CorrelationId.MDC_KEY), nombres));
        });
        return visto.get();
    }

    @Test
    void conservaElIdentificadorValido() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/products");
        req.addHeader(CorrelationId.HEADER, "front-12345678");
        MockHttpServletResponse res = new MockHttpServletResponse();
        Visto v = ejecutar(req, res);
        assertEquals("front-12345678", v.header());
        assertEquals("front-12345678", v.mdc());
        assertEquals("front-12345678", res.getHeader(CorrelationId.HEADER));
        assertNull(MDC.get(CorrelationId.MDC_KEY), "el MDC se limpia al terminar la petición");
    }

    @Test
    void generaUnoNuevoSiFaltaOEsInvalido() throws Exception {
        MockHttpServletResponse sinHeader = new MockHttpServletResponse();
        Visto v1 = ejecutar(new MockHttpServletRequest("GET", "/api/v1/products"), sinHeader);
        assertTrue(CorrelationId.esValido(v1.header()));
        assertEquals(1, v1.nombres(), "el MS recibe la cabecera aunque el navegador no la mandó");

        MockHttpServletRequest malo = new MockHttpServletRequest("GET", "/api/v1/products");
        malo.addHeader(CorrelationId.HEADER, "x\r\nlinea-falsa-en-el-log");
        MockHttpServletResponse res = new MockHttpServletResponse();
        Visto v2 = ejecutar(malo, res);
        assertNotEquals("x\r\nlinea-falsa-en-el-log", v2.header());
        assertTrue(CorrelationId.esValido(v2.header()));
        assertEquals(1, v2.nombres(), "el valor inválido se reemplaza, no se duplica");
        assertEquals(v2.header(), res.getHeader(CorrelationId.HEADER));
        assertNull(MDC.get(CorrelationId.MDC_KEY));
    }

    @Test
    void elMdcSeLimpiaAunqueFalleLaPeticion() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/products");
        try {
            filtro.doFilter(req, new MockHttpServletResponse(), (r, s) -> {
                throw new IllegalStateException("falla");
            });
        } catch (Exception ignorada) {
            // se espera
        }
        assertNull(MDC.get(CorrelationId.MDC_KEY));
    }

    @Test
    void formatoPermitido() {
        assertTrue(CorrelationId.esValido("123e4567-e89b-12d3-a456-426614174000"));
        assertEquals(false, CorrelationId.esValido("corto"));
        assertEquals(false, CorrelationId.esValido("a".repeat(65)));
        assertEquals(false, CorrelationId.esValido("con espacios 123"));
        assertEquals(false, CorrelationId.esValido(null));
    }
}
