package com.ventasdiscos.mscatalog;

import com.ventasdiscos.mscatalog.security.Servicios;
import com.ventasdiscos.mscatalog.client.AccountsClient;
import com.ventasdiscos.mscatalog.exception.AccesoDenegadoException;
import com.ventasdiscos.mscatalog.exception.ServicioExternoNoDisponibleException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de punta a punta de Catalog (HTTP -> Controller -> Service -> H2).
 * Lote 2: el inventario vive en este MS, así que el stock se carga con las rutas reales de
 * /api/v1/admin/inventory (antes se simulaba InventoryClient con @MockitoBean).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogoITTest {

    @Autowired
    private MockMvc mockMvc;

    /** Lote 5 (HU-78): simulado. Por defecto no lanza nada = cuenta ACTIVA (las pruebas anteriores no cambian). */
    @MockitoBean
    private AccountsClient cuentas;

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

    /** Lote 2: carga stock con la ruta real de inventario (responsable = Admin del token). */
    private void entrada(long productId, int cantidad) throws Exception {
        json(post("/api/v1/admin/inventory/" + productId + "/movements"),
                "{\"tipo\":\"ENTRADA\",\"cantidad\":" + cantidad + "}")
                .andExpect(status().isCreated());
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
        entrada(id, 12);

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

    /** Lote 2 (UML 03): el producto nace con su inventario en 0 -> Agotado y no comprable (RN-09). */
    @Test
    void productoNuevoNaceAgotadoYNoEsComprable() throws Exception {
        long id = crearProducto("Disco recién creado");
        mockMvc.perform(get("/api/v1/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidad").value("AGOTADO"))
                .andExpect(jsonPath("$.comprable").value(false));
        admin(get("/api/v1/admin/inventory/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(0))
                .andExpect(jsonPath("$.nombre").value("Disco recién creado"));
    }

    @Test
    void catalogoPublicoFiltraPorDisponibilidad() throws Exception {
        long conStock = crearProducto("Filtro disponible");
        long sinStock = crearProducto("Filtro agotado");
        entrada(conStock, 20);

        mockMvc.perform(get("/api/v1/products?q=filtro&disponibilidad=DISPONIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].comprable").value(true));
        mockMvc.perform(get("/api/v1/products?q=filtro&disponibilidad=AGOTADO"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(sinStock))
                .andExpect(jsonPath("$.contenido[0].comprable").value(false));
        mockMvc.perform(get("/api/v1/products?q=filtro&disponibilidad=STOCK_CRITICO"))
                .andExpect(jsonPath("$.totalElementos").value(0));
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
        mockMvc.perform(post("/api/v1/internal/catalog/pricing").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS)).contentType(MediaType.APPLICATION_JSON)
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

    // ===================== Lote 5 · Integraciones de HU-78 =====================

    /** RN-37 / RN-44 / T-06: con la cuenta que no puede operar (403) o Accounts caído (503) no se guarda el favorito. */
    @Test
    void favoritoRechazadoSiLaCuentaNoPuedeOperarOAccountsNoResponde() throws Exception {
        long id = crearProducto("Disco bloqueado");
        doThrow(new AccesoDenegadoException("Tu cuenta no puede realizar nuevas operaciones")).when(cuentas).exigirCuentaActiva(71L);
        mockMvc.perform(post("/api/v1/favorites/" + id).header("Authorization", TestTokens.cliente(71L)))
                .andExpect(status().isForbidden());
        doThrow(new ServicioExternoNoDisponibleException("No fue posible confirmar el estado de tu cuenta")).when(cuentas)
                .exigirCuentaActiva(72L);
        mockMvc.perform(post("/api/v1/favorites/" + id).header("Authorization", TestTokens.cliente(72L)))
                .andExpect(status().isServiceUnavailable());
        // Cierre (RN-44): mientras la cuenta no esté ACTIVA (o Accounts no responda) tampoco se consulta
        mockMvc.perform(get("/api/v1/favorites").header("Authorization", TestTokens.cliente(71L))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/favorites").header("Authorization", TestTokens.cliente(72L)))
                .andExpect(status().isServiceUnavailable());
        for (long u : new long[]{71L, 72L}) {
            doNothing().when(cuentas).exigirCuentaActiva(u);
            mockMvc.perform(get("/api/v1/favorites").header("Authorization", TestTokens.cliente(u)))
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    /** HU-78 / RN-43 (ERS 13.8): depuración interna idempotente; solo borra los favoritos de ese usuario. */
    @Test
    void depuracionInternaDeFavoritosEsIdempotente() throws Exception {
        long a = crearProducto("Disco depurado A");
        long b = crearProducto("Disco depurado B");
        mockMvc.perform(post("/api/v1/favorites/" + a).header("Authorization", TestTokens.cliente(73L))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/favorites/" + b).header("Authorization", TestTokens.cliente(73L))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/favorites/" + a).header("Authorization", TestTokens.cliente(74L))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/internal/catalog/users/73/favorites/purge").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(73)).andExpect(jsonPath("$.eliminados").value(2));
        mockMvc.perform(post("/api/v1/internal/catalog/users/73/favorites/purge").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.eliminados").value(0));
        mockMvc.perform(get("/api/v1/favorites").header("Authorization", TestTokens.cliente(73L))).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/favorites").header("Authorization", TestTokens.cliente(74L))).andExpect(jsonPath("$.length()").value(1));
        // Los productos no se tocan
        mockMvc.perform(get("/api/v1/products/" + a)).andExpect(status().isOk());
    }
}
