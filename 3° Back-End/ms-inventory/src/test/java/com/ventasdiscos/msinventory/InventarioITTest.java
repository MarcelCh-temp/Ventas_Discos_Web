package com.ventasdiscos.msinventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de punta a punta de EP-07: seguridad, validación de enteros, stock e idempotencia. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventarioITTest {

    @Autowired
    private MockMvc mockMvc;

    private void entrada(long productId, int cantidad) throws Exception {
        mockMvc.perform(post("/api/v1/inventory/" + productId + "/movements")
                        .header("Authorization", TestTokens.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENTRADA\",\"cantidad\":" + cantidad + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void sinToken_401_yComoCliente_403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/inventory").header("Authorization", TestTokens.cliente(5L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cantidadDecimal_400() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/1/movements")
                        .header("Authorization", TestTokens.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENTRADA\",\"cantidad\":2.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidad").exists());
    }

    @Test
    void cantidadTexto_400() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/1/movements")
                        .header("Authorization", TestTokens.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENTRADA\",\"cantidad\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void entradaYSalidaMayorAlStock_409() throws Exception {
        entrada(10L, 3);
        mockMvc.perform(post("/api/v1/inventory/10/movements")
                        .header("Authorization", TestTokens.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"SALIDA\",\"cantidad\":4}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/inventory/10").header("Authorization", TestTokens.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(3))
                .andExpect(jsonPath("$.disponibilidad").value("STOCK_CRITICO"));
    }

    @Test
    void operacionInternaRepetidaEsIdempotente() throws Exception {
        entrada(20L, 10);
        String body = """
                {"operacionId":"PEDIDO-99-VENTA","motivo":"Venta pedido 99",
                 "items":[{"productId":20,"tipo":"SALIDA","cantidad":2}]}
                """;
        mockMvc.perform(post("/api/v1/internal/inventory/operations")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.yaAplicada").value(false));
        mockMvc.perform(post("/api/v1/internal/inventory/operations")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yaAplicada").value(true));
        mockMvc.perform(get("/api/v1/inventory/20").header("Authorization", TestTokens.admin()))
                .andExpect(jsonPath("$.stockActual").value(8));
    }

    @Test
    void disponibilidadPorLoteIncluyeProductosSinRegistro() throws Exception {
        mockMvc.perform(get("/api/v1/internal/inventory/availability?productIds=777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponibilidad").value("AGOTADO"));
    }

    private org.springframework.test.web.servlet.ResultActions conAdmin(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.header("Authorization", TestTokens.admin()));
    }

    @Test
    void ajusteUmbralYFiltrosDeListado() throws Exception {
        entrada(30L, 10);
        entrada(31L, 2);
        conAdmin(post("/api/v1/inventory/30/adjustments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nuevoStock\":8,\"motivo\":\"Conteo físico\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockAnterior").value(10))
                .andExpect(jsonPath("$.stockResultante").value(8));
        conAdmin(put("/api/v1/inventory/30/critical-level").contentType(MediaType.APPLICATION_JSON)
                .content("{\"umbralCritico\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidad").value("DISPONIBLE"));
        mockMvc.perform(post("/api/v1/internal/inventory/products/32")).andExpect(status().isCreated());

        conAdmin(get("/api/v1/inventory?productId=30")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        conAdmin(get("/api/v1/inventory?productIds=30,31,32&size=10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3));
        conAdmin(get("/api/v1/inventory?productIds=30,31,32&disponibilidad=DISPONIBLE"))
                .andExpect(jsonPath("$.contenido[0].productId").value(30));
        conAdmin(get("/api/v1/inventory?productIds=30,31,32&disponibilidad=STOCK_CRITICO"))
                .andExpect(jsonPath("$.contenido[0].productId").value(31));
        conAdmin(get("/api/v1/inventory?productIds=30,31,32&disponibilidad=AGOTADO"))
                .andExpect(jsonPath("$.contenido[0].productId").value(32));

        mockMvc.perform(get("/api/v1/internal/inventory/product-ids?disponibilidad=DISPONIBLE"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/internal/inventory/product-ids?disponibilidad=STOCK_CRITICO"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/internal/inventory/product-ids?disponibilidad=AGOTADO"))
                .andExpect(status().isOk());
    }

    @Test
    void historialConTodosLosFiltros() throws Exception {
        entrada(40L, 5);
        conAdmin(get("/api/v1/inventory/movements?productId=40&tipo=ENTRADA&desde=2000-01-01&hasta=2999-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].productId").value(40))
                .andExpect(jsonPath("$.contenido[0].tipo").value("ENTRADA"));
        conAdmin(get("/api/v1/inventory/movements?desde=2026-05-02&hasta=2026-05-01"))
                .andExpect(status().isBadRequest());
        conAdmin(get("/api/v1/inventory?size=500")).andExpect(status().isBadRequest());
    }

    @Test
    void masivoValidoEInvalido() throws Exception {
        entrada(50L, 1);
        conAdmin(post("/api/v1/inventory/bulk").contentType(MediaType.APPLICATION_JSON)
                .content("{\"operaciones\":[{\"productId\":50,\"tipo\":\"ENTRADA\",\"cantidad\":4},{\"productId\":51,\"tipo\":\"AJUSTE\",\"cantidad\":7,\"motivo\":\"Conteo\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
        conAdmin(post("/api/v1/inventory/bulk").contentType(MediaType.APPLICATION_JSON)
                .content("{\"operaciones\":[{\"productId\":50,\"tipo\":\"SALIDA\",\"cantidad\":99}]}"))
                .andExpect(status().isConflict());
        conAdmin(post("/api/v1/inventory/bulk").contentType(MediaType.APPLICATION_JSON)
                .content("{\"operaciones\":[]}"))
                .andExpect(status().isBadRequest());
        conAdmin(get("/api/v1/inventory/50")).andExpect(jsonPath("$.stockActual").value(5));
    }

    @Test
    void parametroConTipoInvalido_400() throws Exception {
        conAdmin(get("/api/v1/inventory/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void mismaOperacionConOtroContenido_409() throws Exception {
        entrada(60L, 10);
        mockMvc.perform(post("/api/v1/internal/inventory/operations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operacionId\":\"PEDIDO-77-VENTA\",\"motivo\":\"Venta\",\"items\":[{\"productId\":60,\"tipo\":\"SALIDA\",\"cantidad\":2}]}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/internal/inventory/operations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operacionId\":\"PEDIDO-77-VENTA\",\"motivo\":\"Venta\",\"items\":[{\"productId\":60,\"tipo\":\"SALIDA\",\"cantidad\":5}]}"))
                .andExpect(status().isConflict());
        conAdmin(get("/api/v1/inventory/60")).andExpect(jsonPath("$.stockActual").value(8));
    }
}
