package com.ventasdiscos.mscatalog;

import com.ventasdiscos.mscatalog.security.Servicios;
import com.jayway.jsonpath.JsonPath;
import com.ventasdiscos.mscatalog.client.AccountsClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de punta a punta de EP-07 dentro de Catalog (Lote 2): seguridad, validación de enteros,
 * stock nunca negativo, idempotencia, historial y rutas públicas de disponibilidad.
 * Portado de ms-inventory/InventarioITTest. Cambios: rutas /api/v1/admin/inventory, y cada producto
 * se crea primero en el catálogo (ya no se acepta stock para un id que no es producto).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventarioITTest {

    @Autowired
    private MockMvc mockMvc;

    // Cierre (RN-44): las peticiones con token de cliente consultan a Accounts; aquí se simula ACTIVA
    @MockitoBean
    private AccountsClient cuentas;

    private ResultActions conAdmin(MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.header("Authorization", TestTokens.admin()));
    }

    private ResultActions jsonAdmin(MockHttpServletRequestBuilder req, String body) throws Exception {
        return conAdmin(req.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método producto (producto).
    private long producto(String nombre, String categoria, String plataforma) throws Exception {
        String body = """
                {"nombre":"%s","precioBase":9990,"categoria":"%s","genero":"Rock","formato":"CD"%s}
                """.formatted(nombre, categoria, plataforma == null ? "" : ",\"plataforma\":\"" + plataforma + "\"");
        String resp = jsonAdmin(post("/api/v1/admin/products"), body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resp, "$.id")).longValue();
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método producto (producto).
    private long producto(String nombre) throws Exception {
        return producto(nombre, "MUSICA", null);
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método entrada (entrada).
    private void entrada(long productId, int cantidad) throws Exception {
        jsonAdmin(post("/api/v1/admin/inventory/" + productId + "/movements"),
                "{\"tipo\":\"ENTRADA\",\"cantidad\":" + cantidad + "}")
                .andExpect(status().isCreated());
    }

    // ===================== Seguridad y validación (tests originales) =====================

    @Test
    void sinToken_401_yComoCliente_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/inventory").header("Authorization", TestTokens.cliente(5L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/inventory/1/movements").header("Authorization", TestTokens.cliente(5L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"ENTRADA\",\"cantidad\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cantidadDecimal_400() throws Exception {
        jsonAdmin(post("/api/v1/admin/inventory/1/movements"), "{\"tipo\":\"ENTRADA\",\"cantidad\":2.5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidad").exists());
    }

    @Test
    void cantidadTexto_400() throws Exception {
        jsonAdmin(post("/api/v1/admin/inventory/1/movements"), "{\"tipo\":\"ENTRADA\",\"cantidad\":\"abc\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void parametroConTipoInvalido_400() throws Exception {
        conAdmin(get("/api/v1/admin/inventory/abc")).andExpect(status().isBadRequest());
    }

    // ===================== Stock (RN-10) =====================

    @Test
    void entradaYSalidaMayorAlStock_409() throws Exception {
        long id = producto("Inv salida");
        entrada(id, 3);
        jsonAdmin(post("/api/v1/admin/inventory/" + id + "/movements"), "{\"tipo\":\"SALIDA\",\"cantidad\":4}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").exists());
        conAdmin(get("/api/v1/admin/inventory/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(3))
                .andExpect(jsonPath("$.disponibilidad").value("STOCK_CRITICO"));
    }

    /** Lote 2: un id que no es producto del catálogo no puede tener stock. */
    @Test
    void movimientoDeProductoInexistente_404() throws Exception {
        jsonAdmin(post("/api/v1/admin/inventory/999999/movements"), "{\"tipo\":\"ENTRADA\",\"cantidad\":1}")
                .andExpect(status().isNotFound());
        conAdmin(get("/api/v1/admin/inventory/999999")).andExpect(status().isNotFound());
    }

    @Test
    void ajusteUmbralYFiltrosDeListado() throws Exception {
        long a = producto("Inv lista A");
        long b = producto("Inv lista B");
        long c = producto("Inv lista C");
        entrada(a, 10);
        entrada(b, 2);
        jsonAdmin(post("/api/v1/admin/inventory/" + a + "/adjustments"), "{\"nuevoStock\":8,\"motivo\":\"Conteo físico\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockAnterior").value(10))
                .andExpect(jsonPath("$.stockResultante").value(8))
                .andExpect(jsonPath("$.usuarioId").value(1));
        jsonAdmin(put("/api/v1/admin/inventory/" + a + "/critical-level"), "{\"umbralCritico\":3}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidad").value("DISPONIBLE"));

        String ids = a + "," + b + "," + c;
        conAdmin(get("/api/v1/admin/inventory?productId=" + a)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        conAdmin(get("/api/v1/admin/inventory?productIds=" + ids + "&size=10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3));
        conAdmin(get("/api/v1/admin/inventory?productIds=" + ids + "&disponibilidad=DISPONIBLE"))
                .andExpect(jsonPath("$.contenido[0].productId").value(a));
        conAdmin(get("/api/v1/admin/inventory?productIds=" + ids + "&disponibilidad=STOCK_CRITICO"))
                .andExpect(jsonPath("$.contenido[0].productId").value(b));
        conAdmin(get("/api/v1/admin/inventory?productIds=" + ids + "&disponibilidad=AGOTADO"))
                .andExpect(jsonPath("$.contenido[0].productId").value(c));
    }

    /** Lote 2 (RF-INV-02): buscar por nombre o identificador y filtrar por categoría. */
    @Test
    void listadoPorTextoYCategoria() throws Exception {
        long musica = producto("Inv busqueda texto");
        long juego = producto("Inv busqueda juego", "VIDEOJUEGOS", "PS5");
        conAdmin(get("/api/v1/admin/inventory?q=busqueda"))
                .andExpect(jsonPath("$.totalElementos").value(2));
        conAdmin(get("/api/v1/admin/inventory?q=busqueda&categoria=VIDEOJUEGOS"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].productId").value(juego))
                .andExpect(jsonPath("$.contenido[0].categoria").value("VIDEOJUEGOS"));
        conAdmin(get("/api/v1/admin/inventory?q=" + musica))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Inv busqueda texto"));
    }

    @Test
    void historialConTodosLosFiltros() throws Exception {
        long id = producto("Inv historial");
        entrada(id, 5);
        conAdmin(get("/api/v1/admin/inventory/movements?productId=" + id + "&tipo=ENTRADA&desde=2000-01-01&hasta=2999-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].productId").value(id))
                .andExpect(jsonPath("$.contenido[0].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.contenido[0].usuarioId").value(1));
        conAdmin(get("/api/v1/admin/inventory/movements?desde=2026-05-02&hasta=2026-05-01"))
                .andExpect(status().isBadRequest());
        conAdmin(get("/api/v1/admin/inventory?size=500")).andExpect(status().isBadRequest());
    }

    @Test
    void masivoValidoEInvalido() throws Exception {
        long a = producto("Inv masivo A");
        long b = producto("Inv masivo B");
        entrada(a, 1);
        jsonAdmin(post("/api/v1/admin/inventory/bulk"), "{\"operaciones\":[{\"productId\":" + a
                + ",\"tipo\":\"ENTRADA\",\"cantidad\":4},{\"productId\":" + b
                + ",\"tipo\":\"AJUSTE\",\"cantidad\":7,\"motivo\":\"Conteo\"}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
        jsonAdmin(post("/api/v1/admin/inventory/bulk"), "{\"operaciones\":[{\"productId\":" + a
                + ",\"tipo\":\"SALIDA\",\"cantidad\":99}]}")
                .andExpect(status().isConflict());
        jsonAdmin(post("/api/v1/admin/inventory/bulk"), "{\"operaciones\":[]}")
                .andExpect(status().isBadRequest());
        // Una fila con producto inexistente invalida todo el lote (todo o nada)
        jsonAdmin(post("/api/v1/admin/inventory/bulk"), "{\"operaciones\":[{\"productId\":" + a
                + ",\"tipo\":\"ENTRADA\",\"cantidad\":1},{\"productId\":999999,\"tipo\":\"ENTRADA\",\"cantidad\":1}]}")
                .andExpect(status().isNotFound());
        conAdmin(get("/api/v1/admin/inventory/" + a)).andExpect(jsonPath("$.stockActual").value(5));
    }

    // ===================== Ruta interna (Orders / PostSale) =====================

    @Test
    void operacionInternaRepetidaEsIdempotente() throws Exception {
        long id = producto("Inv interna");
        entrada(id, 10);
        String body = """
                {"operacionId":"ORDERS-PEDIDO-99-VENTA","motivo":"Venta pedido 99",
                 "items":[{"productId":%d,"tipo":"SALIDA","cantidad":2}]}
                """.formatted(id);
        mockMvc.perform(post("/api/v1/internal/inventory/operations").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.yaAplicada").value(false));
        mockMvc.perform(post("/api/v1/internal/inventory/operations").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yaAplicada").value(true));
        conAdmin(get("/api/v1/admin/inventory/" + id)).andExpect(jsonPath("$.stockActual").value(8));
    }

    @Test
    void mismaOperacionConOtroContenido_409() throws Exception {
        long id = producto("Inv interna conflicto");
        entrada(id, 10);
        mockMvc.perform(post("/api/v1/internal/inventory/operations").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operacionId\":\"ORDERS-PEDIDO-77-VENTA\",\"motivo\":\"Venta\",\"items\":[{\"productId\":"
                                + id + ",\"tipo\":\"SALIDA\",\"cantidad\":2}]}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/internal/inventory/operations").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operacionId\":\"ORDERS-PEDIDO-77-VENTA\",\"motivo\":\"Venta\",\"items\":[{\"productId\":"
                                + id + ",\"tipo\":\"SALIDA\",\"cantidad\":5}]}"))
                .andExpect(status().isConflict());
        conAdmin(get("/api/v1/admin/inventory/" + id)).andExpect(jsonPath("$.stockActual").value(8));
    }

    // ===================== Lote 2: disponibilidad pública (ERS 12) =====================

    @Test
    void disponibilidadPublicaSinTokenYSinStockExacto() throws Exception {
        long id = producto("Inv publico");
        entrada(id, 9);
        mockMvc.perform(get("/api/v1/inventory/" + id + "/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidad").value("DISPONIBLE"))
                .andExpect(jsonPath("$.permiteCompra").value(true))
                .andExpect(jsonPath("$.stockActual").doesNotExist());
        mockMvc.perform(get("/api/v1/inventory/999999/availability")).andExpect(status().isNotFound());
    }

    @Test
    void disponibilidadPublicaDeRetirado_410() throws Exception {
        long id = producto("Inv retirado");
        jsonAdmin(post("/api/v1/admin/products/retire"), "{\"ids\":[" + id + "]}").andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/inventory/" + id + "/availability"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.mensaje").value("Este producto ya no se encuentra disponible."));
    }

    @Test
    void consultaDeDisponibilidadEnLote() throws Exception {
        long a = producto("Inv carrito A");
        long b = producto("Inv carrito B");
        entrada(a, 3);
        mockMvc.perform(post("/api/v1/inventory/availability/query").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + a + ",\"cantidad\":2},{\"productId\":" + b
                                + ",\"cantidad\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].haySuficiente").value(true))
                .andExpect(jsonPath("$[0].permiteCompra").value(true))
                .andExpect(jsonPath("$[0].disponibilidad").value("STOCK_CRITICO"))
                .andExpect(jsonPath("$[1].disponibilidad").value("AGOTADO"))
                .andExpect(jsonPath("$[1].permiteCompra").value(false))
                .andExpect(jsonPath("$[0].stockActual").doesNotExist());
        // Solo consulta: el stock no cambia
        conAdmin(get("/api/v1/admin/inventory/" + a)).andExpect(jsonPath("$.stockActual").value(3));
        mockMvc.perform(post("/api/v1/inventory/availability/query").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/inventory/availability/query").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + a + ",\"cantidad\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    /** DOC-08: una operación interna sin prefijo ORDERS- o POSTSALE- se rechaza con 400 y ApiError. */
    @Test
    void operacionInternaSinPrefijoPermitido_400() throws Exception {
        long id = producto("Inv prefijo");
        entrada(id, 5);
        mockMvc.perform(post("/api/v1/internal/inventory/operations").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operacionId\":\"PEDIDO-5-VENTA\",\"motivo\":\"Venta\",\"items\":[{\"productId\":"
                                + id + ",\"tipo\":\"SALIDA\",\"cantidad\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.operacionId").exists());
        mockMvc.perform(post("/api/v1/internal/inventory/operations").header("Authorization", TokensServicioPrueba.bearer(Servicios.POSTSALE)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operacionId\":\"POSTSALE-RECEPCION-5\",\"motivo\":\"Reposicion\",\"items\":[{\"productId\":"
                                + id + ",\"tipo\":\"ENTRADA\",\"cantidad\":1}]}"))
                .andExpect(status().isCreated());
        conAdmin(get("/api/v1/admin/inventory/" + id)).andExpect(jsonPath("$.stockActual").value(6));
    }

    /** Lote 6 · DOC-08 + DOC-05: el prefijo del operacionId debe corresponder al servicio del JWT de servicio. */
    @Test
    void prefijoQueNoCorrespondeAlServicioAutenticado_403() throws Exception {
        long id = producto("Inv origen");
        entrada(id, 5);
        String venta = "{\"operacionId\":\"ORDERS-PEDIDO-55-VENTA\",\"motivo\":\"Venta\",\"items\":[{\"productId\":"
                + id + ",\"tipo\":\"SALIDA\",\"cantidad\":1}]}";
        String reposicion = "{\"operacionId\":\"POSTSALE-RECEPCION-55\",\"motivo\":\"Reposicion\",\"items\":[{\"productId\":"
                + id + ",\"tipo\":\"ENTRADA\",\"cantidad\":1}]}";
        // ORDERS- enviado por PostSale y POSTSALE- enviado por Orders: 403 y el stock no cambia
        mockMvc.perform(post("/api/v1/internal/inventory/operations")
                        .header("Authorization", TokensServicioPrueba.bearer(Servicios.POSTSALE))
                        .contentType(MediaType.APPLICATION_JSON).content(venta))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        mockMvc.perform(post("/api/v1/internal/inventory/operations")
                        .header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                        .contentType(MediaType.APPLICATION_JSON).content(reposicion))
                .andExpect(status().isForbidden());
        // Un servicio que no está autorizado para la ruta (Accounts) tampoco puede
        mockMvc.perform(post("/api/v1/internal/inventory/operations")
                        .header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS))
                        .contentType(MediaType.APPLICATION_JSON).content(venta))
                .andExpect(status().isForbidden());
        // Sin token o con JWT de usuario: 401
        mockMvc.perform(post("/api/v1/internal/inventory/operations")
                        .contentType(MediaType.APPLICATION_JSON).content(venta))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/internal/inventory/operations").header("Authorization", TestTokens.admin())
                        .contentType(MediaType.APPLICATION_JSON).content(venta))
                .andExpect(status().isUnauthorized());
        conAdmin(get("/api/v1/admin/inventory/" + id)).andExpect(jsonPath("$.stockActual").value(5));
        // Con el servicio que corresponde, sí se aplica
        mockMvc.perform(post("/api/v1/internal/inventory/operations")
                        .header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                        .contentType(MediaType.APPLICATION_JSON).content(venta))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/internal/inventory/operations")
                        .header("Authorization", TokensServicioPrueba.bearer(Servicios.POSTSALE))
                        .contentType(MediaType.APPLICATION_JSON).content(reposicion))
                .andExpect(status().isCreated());
        conAdmin(get("/api/v1/admin/inventory/" + id)).andExpect(jsonPath("$.stockActual").value(5));
    }
}
