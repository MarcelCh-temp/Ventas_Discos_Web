package com.ventasdiscos.msaccounts.trazabilidad;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lote 6 · DOC-06: el MS conserva o genera el correlationId y limpia el MDC al terminar cada petición. */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    private String ejecutar(MockHttpServletRequest req, MockHttpServletResponse res) throws Exception {
        AtomicReference<String> enMdc = new AtomicReference<>();
        filtro.doFilter(req, res, (r, s) -> {
            enMdc.set(MDC.get(CorrelationId.MDC_KEY));
            assertEquals(enMdc.get(), CorrelationId.de((HttpServletRequest) r));
        });
        return enMdc.get();
    }

    @Test
    void conservaElRecibidoDelGatewayOGeneraUno() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        req.addHeader(CorrelationId.HEADER, "gateway-12345678");
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertEquals("gateway-12345678", ejecutar(req, res));
        assertEquals("gateway-12345678", res.getHeader(CorrelationId.HEADER));
        assertNull(MDC.get(CorrelationId.MDC_KEY));

        MockHttpServletRequest malo = new MockHttpServletRequest("GET", "/x");
        malo.addHeader(CorrelationId.HEADER, "valor\ncon salto");
        String generado = ejecutar(malo, new MockHttpServletResponse());
        assertNotEquals("valor\ncon salto", generado);
        assertTrue(CorrelationId.esValido(generado));
        assertNull(MDC.get(CorrelationId.MDC_KEY));
    }

    @Test
    void cadaPeticionTieneSuPropioIdentificador() throws Exception {
        String a = ejecutar(new MockHttpServletRequest("GET", "/x"), new MockHttpServletResponse());
        String b = ejecutar(new MockHttpServletRequest("GET", "/x"), new MockHttpServletResponse());
        assertNotEquals(a, b);
    }

    @Test
    void elMdcSeLimpiaAunqueLaPeticionFalle() {
        try {
            filtro.doFilter(new MockHttpServletRequest("GET", "/x"), new MockHttpServletResponse(), (r, s) -> {
                throw new IllegalStateException("falla");
            });
        } catch (Exception ignorada) {
            // se espera
        }
        assertNull(MDC.get(CorrelationId.MDC_KEY));
    }
}
