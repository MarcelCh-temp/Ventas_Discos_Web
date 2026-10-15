package com.ventasdiscos.mscatalog;

import com.jayway.jsonpath.JsonPath;
import com.ventasdiscos.mscatalog.client.InventarioDto;
import com.ventasdiscos.mscatalog.client.InventoryClient;
import com.ventasdiscos.mscatalog.exception.ServicioExternoNoDisponibleException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de punta a punta de Catalog (HTTP -> Controller -> Service -> H2).
 * InventoryClient se reemplaza por un mock (@MockitoBean): así se prueba tanto
 * Inventory respondiendo como Inventory caído, sin levantar el otro MS.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogoITTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryClient inventoryClient;

    private ResultActions admin(MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.header("Authorization", TestTokens.admin()));
    }

    private ResultActions json(MockHttpServletRequestBuilder req, String body) throws Exception {
        return admin(req.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long crearProducto(String nombre, String categoria, String plataforma) throws Exception {
        String body = """
                {"nombre":"%s","precioBase":19990,"categoria":"%s","genero":"Rock","formato":"CD"%s}
                """.formatted(nombre, categoria, plataforma == null ? "" : ",\"plataforma\":\"" + plataforma + "\"");
        String resp = json(post("/api/v1/admin/products"), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resp, "$.id")).longValue();
    }

    private long crearProducto(String nombre) throws Exception {
        return crearProducto(nombre, "MUSICA", null);
    }

    // ===================== Seguridad =====================

    @Test
    void sinToken_401_yClienteEnAdmin_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", TestTokens.cliente(5L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"precioBase\":1000,\"categoria\":\"MUSICA\",\"genero\":\"Rock\",\"formato\":\"CD\"}"))
                .andExpect(status().isForbidden());
    }

    // ===================== EP-02 Administración =====================

    @Test
    void crearEditarYDetalleAdmin() throws Exception {
        long id = crearProducto("Juego aventura", "VIDEOJUEGOS", "PS5");
        when(inventoryClient.disponibilidad(anyCollection()))
                .thenReturn(Map.of(id, new InventarioDto(id, 12, 5, "DISPONIBLE")));

        json(put("/api/v1/admin/products/" + id), """
                {"nombre":"Juego aventura GOTY","precioBase":24990,"categoria":"VIDEOJUEGOS",
                 "genero":"Aventura","formato":"Físico","plataforma":"PS5","imagenUrl":"https://img.cl/a.jpg"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juego aventura GOTY"))
                .andExpect(jsonPath("$.stockActual").value(12));

        admin(get("/api/v1/admin/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidad").value("DISPONIBLE"));
    }

    @Test
    void datosInvalidos_400() throws Exception {
        json(post("/api/v1/admin/products"), "{\"nombre\":\"X\",\"precioBase\":0,\"categoria\":\"MUSICA\",\"genero\":\"R\",\"formato\":\"CD\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precioBase").exists());
        json(post("/api/v1/admin/products"), "{\"nombre\":\"X\",\"precioBase\":10,\"categoria\":\"VIDEOJUEGOS\",\"genero\":\"R\",\"formato\":\"CD\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.plataforma").exists());
        admin(get("/api/v1/admin/products/abc")).andExpect(status().isBadRequest());
        admin(get("/api/v1/admin/products/999999")).andExpect(status().isNotFound());
    }

    @Test
    void retirarYRepetir_409() throws Exception {
        long id = crearProducto("Disco a retirar");
        json(post("/api/v1/admin/products/retire"), "{\"ids\":[" + id + ",999999]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retirados[0]").value(id))
                .andExpect(jsonPath("$.noEncontrados[0]").value(999999));
        json(post("/api/v1/admin/products/retire"), "{\"ids\":[" + id + "]}")
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/products/" + id))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.mensaje").value("Este producto ya no se encuentra disponible."));
    }

    @Test
    void catalogoAdminConTodosLosFiltros() throws Exception {
        long id = crearProducto("Filtro Admin", "VIDEOJUEGOS", "Switch");
        admin(get("/api/v1/admin/products?q=filtro admin&categoria=VIDEOJUEGOS&genero=rock&formato=cd"
                + "&plataforma=switch&estado=ACTIVO&precioMin=1000&precioMax=50000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(id));
        admin(get("/api/v1/admin/products?q=" + id))
                .andExpect(jsonPath("$.contenido[0].id").value(id));
    }

    // ===================== EP-03 Exploración =====================

    @Test
    void catalogoPublicoFuncionaConInventoryCaidoPeroNoPermiteComprar() throws Exception {
        long id = crearProducto("Disco degradación");
        when(inventoryClient.disponibilidad(anyCollection()))
                .thenThrow(new ServicioExternoNoDisponibleException("caído"));
        mockMvc.perform(get("/api/v1/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidad").value("NO_CONFIRMADA"))
                .andExpect(jsonPath("$.comprable").value(false));
    }

    @Test
    void catalogoPublicoFiltraPorDisponibilidad() throws Exception {
        long conStock = crearProducto("Filtro disponible");
        long sinStock = crearProducto("Filtro agotado");
        when(inventoryClient.productIdsPorDisponibilidad("DISPONIBLE")).thenReturn(List.of(conStock));
        when(inventoryClient.productIdsPorDisponibilidad("STOCK_CRITICO")).thenReturn(List.of());
        when(inventoryClient.disponibilidad(anyCollection())).thenReturn(Map.of(
                conStock, new InventarioDto(conStock, 20, 5, "DISPONIBLE"),
                sinStock, new InventarioDto(sinStock, 0, 5, "AGOTADO")));

        mockMvc.perform(get("/api/v1/products?q=filtro&disponibilidad=DISPONIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].comprable").value(true));
        mockMvc.perform(get("/api/v1/products?q=filtro&disponibilidad=AGOTADO"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(sinStock))
                .andExpect(jsonPath("$.contenido[0].comprable").value(false));
        mockMvc.perform(get("/api/v1/products?disponibilidad=STOCK_CRITICO"))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void filtroPorDisponibilidadConInventoryCaido_503() throws Exception {
        when(inventoryClient.productIdsPorDisponibilidad("DISPONIBLE"))
                .thenThrow(new ServicioExternoNoDisponibleException("No fue posible consultar el inventario"));
        mockMvc.perform(get("/api/v1/products?disponibilidad=DISPONIBLE"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void rangoDePrecioIncoherente_400_yRutaInexistente_404() throws Exception {
        mockMvc.perform(get("/api/v1/products?precioMin=5000&precioMax=1000"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/products?size=0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/products/999999")).andExpect(status().isNotFound());
    }

    // ===================== EP-09 Promociones =====================

    @Test
    void cicloDeVidaDePromocion() throws Exception {
        long id = crearProducto("Disco promo");
        LocalDate hoy = LocalDate.now();
        String body = """
                {"nombre":"Semana del disco","tipoDescuento":"PORCENTAJE","valor":10,
                 "fechaInicio":"%s","fechaFin":"%s","productIds":[%d]}
                """.formatted(hoy, hoy.plusDays(10), id);
        String resp = json(post("/api/v1/admin/promotions"), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andReturn().getResponse().getContentAsString();
        long promoId = ((Number) JsonPath.read(resp, "$.id")).longValue();

        admin(get("/api/v1/admin/promotions/" + promoId)).andExpect(status().isOk());
        json(put("/api/v1/admin/promotions/" + promoId), body.replace("\"valor\":10", "\"valor\":20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(20));

        // HU-52: la oferta se ve en el catálogo y en /offers
        mockMvc.perform(get("/api/v1/products/" + id))
                .andExpect(jsonPath("$.precioOferta").value(15992));
        mockMvc.perform(get("/api/v1/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Semana del disco"));

        // Orders: el precio del carrito ya trae la promoción
        mockMvc.perform(post("/api/v1/internal/catalog/pricing").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + id + ",\"cantidad\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas[0].promocionId").value(promoId))
                .andExpect(jsonPath("$.todosComprables").value(true));

        admin(patch("/api/v1/admin/promotions/" + promoId + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESACTIVADA"));
        admin(patch("/api/v1/admin/promotions/" + promoId + "/deactivate"))
                .andExpect(status().isConflict());
    }

    @Test
    void listarPromocionesConCadaFiltro() throws Exception {
        long id = crearProducto("Disco listado promo");
        LocalDate hoy = LocalDate.now();
        json(post("/api/v1/admin/promotions"), """
                {"nombre":"Promo música","tipoDescuento":"MONTO","valor":1000,"montoMinimoCompra":30000,
                 "fechaInicio":"%s","fechaFin":"%s","categoria":"MUSICA"}
                """.formatted(hoy.plusDays(1), hoy.plusDays(5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"));

        for (String estado : List.of("PROGRAMADA", "ACTIVA", "FINALIZADA", "DESACTIVADA")) {
            admin(get("/api/v1/admin/promotions?estado=" + estado)).andExpect(status().isOk());
        }
        admin(get("/api/v1/admin/promotions?estado=PROGRAMADA&q=música&desde=" + hoy + "&hasta=" + hoy.plusDays(30)
                + "&productId=" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        admin(get("/api/v1/admin/promotions?desde=" + hoy.plusDays(5) + "&hasta=" + hoy))
                .andExpect(status().isBadRequest());
    }

    // ===================== EP-11 Favoritos =====================

    @Test
    void favoritosSinDuplicadosYSoloPropios() throws Exception {
        long id = crearProducto("Disco favorito");
        mockMvc.perform(post("/api/v1/favorites/" + id).header("Authorization", TestTokens.cliente(7L)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/favorites/" + id).header("Authorization", TestTokens.cliente(7L)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/favorites").header("Authorization", TestTokens.cliente(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        // Otro cliente no ve ni puede borrar el favorito del cliente 7
        mockMvc.perform(get("/api/v1/favorites").header("Authorization", TestTokens.cliente(8L)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(delete("/api/v1/favorites/" + id).header("Authorization", TestTokens.cliente(8L)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/favorites/" + id).header("Authorization", TestTokens.cliente(7L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void promocionConMontoMayorAlPrecio_400() throws Exception {
        long id = crearProducto("Disco promo mala");
        json(post("/api/v1/admin/promotions"), """
                {"nombre":"Mala","tipoDescuento":"MONTO","valor":999999,
                 "fechaInicio":"2026-01-01","fechaFin":"2099-12-31","productIds":[%d]}
                """.formatted(id))
                .andExpect(status().isBadRequest());
    }
}
