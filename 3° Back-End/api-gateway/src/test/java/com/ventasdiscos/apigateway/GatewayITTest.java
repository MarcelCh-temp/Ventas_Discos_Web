package com.ventasdiscos.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lote 6: Gateway completo con MockMvc. Los microservicios NO se levantan: sus URLs apuntan a 127.0.0.1:1, así que
 * una petición que PASA la seguridad del Gateway termina en un 5xx de reenvío (demuestra que se reenvió), y una que
 * no la pasa responde 401/403/404 sin llegar a ningún MS.
 * (Escrita en el Lote 6; sin ejecutar.)
 */
@SpringBootTest(properties = {
        "security.jwt.secret=" + TokensPrueba.SECRETO_USUARIOS,
        "gateway.cors.allowed-origins=http://localhost:5500,http://127.0.0.1:5500",
        "spring.cloud.gateway.server.webmvc.routes[0].uri=http://127.0.0.1:1",
        "spring.cloud.gateway.server.webmvc.routes[1].uri=http://127.0.0.1:1",
        "spring.cloud.gateway.server.webmvc.routes[2].uri=http://127.0.0.1:1",
        "spring.cloud.gateway.server.webmvc.routes[3].uri=http://127.0.0.1:1",
        "spring.cloud.gateway.server.webmvc.routes[4].uri=http://127.0.0.1:1",
        "spring.cloud.gateway.server.webmvc.routes[5].uri=http://127.0.0.1:1"})
@AutoConfigureMockMvc
class GatewayITTest {

    private static final String FRONT = "http://localhost:5500";

    @Autowired
    private MockMvc mockMvc;

    // ===================== Rutas internas: nunca se exponen =====================

    @Test
    void rutasInternasResponden404ConApiErrorAunqueTraiganToken() throws Exception {
        mockMvc.perform(get("/api/v1/internal/accounts/5/operation-status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/internal/accounts/5/operation-status"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        mockMvc.perform(post("/api/v1/internal/inventory/operations")
                        .header("Authorization", TokensPrueba.servicio("ms-orders", "ms-catalog")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/internal/notifications/events").header("Authorization", TokensPrueba.admin()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/INTERNAL/orders/1").header("Authorization", TokensPrueba.admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rutasCodificadasORarasSeRechazanSinReenviar() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/%73tats").header("Authorization", TokensPrueba.cliente(7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rutaQueNoPerteneceANingunMicroservicio_404() throws Exception {
        mockMvc.perform(get("/api/ordenes")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(get("/api/v1/inventario")).andExpect(status().isNotFound());
    }

    // ===================== JWT de usuario en el Gateway =====================

    @Test
    void rutaDeClienteSinTokenOConTokenInvalido_401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("Debes iniciar sesión para acceder a este recurso"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        mockMvc.perform(get("/api/v1/cart").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/cart").header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtDeServicioEnRutaDeCliente_401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", TokensPrueba.servicio("ms-orders", "ms-notifications")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaAdministrativaConCliente_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders").header("Authorization", TokensPrueba.cliente(7)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/api/v1/accounts/stats").header("Authorization", TokensPrueba.cliente(7)))
                .andExpect(status().isForbidden());
        // Cierre (decisión 3/4): la consulta agrupada de clientes es solo para administradores
        mockMvc.perform(get("/api/v1/admin/accounts").param("ids", "1,2").header("Authorization", TokensPrueba.cliente(7)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/accounts").param("ids", "1,2")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/support/requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void peticionesQuePasanLaSeguridadSeReenvian() throws Exception {
        // El MS no está levantado: el 5xx (y no 401/403/404) prueba que el Gateway dejó pasar y reenvió.
        mockMvc.perform(get("/api/v1/admin/orders").header("Authorization", TokensPrueba.admin()))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        mockMvc.perform(get("/api/v1/orders/me").header("Authorization", TokensPrueba.cliente(7)))
                .andExpect(status().is5xxServerError());
        mockMvc.perform(get("/api/v1/admin/accounts").param("ids", "1,2").header("Authorization", TokensPrueba.admin()))
                .andExpect(status().is5xxServerError());
        // Públicas: sin token, e incluso con un token vencido guardado en el navegador
        mockMvc.perform(get("/api/v1/products")).andExpect(status().is5xxServerError());
        mockMvc.perform(post("/api/v1/auth/login").header("Authorization", "Bearer vencido")
                        .contentType("application/json").content("{}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void microservicioCaido_503ConApiError() throws Exception {
        mockMvc.perform(get("/api/v1/offers"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.path").value("/api/v1/offers"));
    }

    // ===================== DOC-06: correlationId =====================

    @Test
    void correlationIdSeConservaOSeGenera() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me").header("X-Correlation-Id", "front-abc-12345"))
                .andExpect(header().string("X-Correlation-Id", "front-abc-12345"))
                .andExpect(jsonPath("$.correlationId").value("front-abc-12345"));
        MvcResult r = mockMvc.perform(get("/api/v1/orders/me").header("X-Correlation-Id", "inválido con espacios"))
                .andReturn();
        String generado = r.getResponse().getHeader("X-Correlation-Id");
        assertTrue(generado != null && generado.matches("[A-Za-z0-9-]{8,64}"));
        assertTrue(r.getResponse().getContentAsString().contains(generado));
    }

    // ===================== Decisión 2/4: CORS solo en el Gateway =====================

    @Test
    void preflightDeOrigenPermitidoSinToken() throws Exception {
        mockMvc.perform(options("/api/v1/orders/me")
                        .header("Origin", FRONT)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization, X-Correlation-Id"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Access-Control-Allow-Origin", hasSize(1)))
                .andExpect(header().string("Access-Control-Allow-Origin", FRONT))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("X-Correlation-Id")));
        mockMvc.perform(options("/api/v1/admin/products/1")
                        .header("Origin", "http://127.0.0.1:5500")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5500"));
    }

    @Test
    void preflightDeOrigenNoPermitido_403SinCabecerasCors() throws Exception {
        MvcResult r = mockMvc.perform(options("/api/v1/products")
                        .header("Origin", "http://sitio-malicioso.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andReturn();
        assertEquals(null, r.getResponse().getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void erroresDelGatewayLlevanCorsParaQueElFrontendLeaElApiError() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me").header("Origin", FRONT))
                .andExpect(status().isUnauthorized())
                .andExpect(header().stringValues("Access-Control-Allow-Origin", hasSize(1)))
                .andExpect(header().string("Access-Control-Allow-Origin", FRONT))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("X-Correlation-Id")));
    }
}
