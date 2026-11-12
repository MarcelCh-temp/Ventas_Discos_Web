package ms_ordenes.ms_ordenes;

import ms_ordenes.ms_ordenes.security.Servicios;
import com.jayway.jsonpath.JsonPath;
import ms_ordenes.ms_ordenes.client.AccountsClient;
import ms_ordenes.ms_ordenes.client.CatalogClient;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.Cotizacion;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.Disponibilidad;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.ItemSolicitado;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.LineaCotizada;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.RespuestaOperacion;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.ResultadoOperacion;
import ms_ordenes.ms_ordenes.client.NotificacionesClient;
import ms_ordenes.ms_ordenes.exception.AccesoDenegadoException;
import ms_ordenes.ms_ordenes.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lote 3 · Pruebas de punta a punta de Orders (HTTP -> Controller -> Service -> H2). CatalogClient y
 * NotificacionesClient se simulan (@MockitoBean): no se levantan otros MS.
 * SIN @Transactional a propósito: así los avisos "después del commit" se ejecutan de verdad. Cada test usa un
 * Cliente distinto (userId propio) para no mezclar datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrdenesITTest {

    private static final AtomicLong USUARIOS = new AtomicLong(5000);
    private static final String RETIRADO = "Este producto ya no se encuentra disponible.";

    /** Catálogo simulado: precio, descuento de promoción, stock y si está activo. */
    private record Prod(String nombre, String precio, String descuento, int stock, boolean activo) {
    }

    private static final Map<Long, Prod> CATALOGO = Map.of(
            1L, new Prod("Disco Rock", "10000", "0", 5, true),
            2L, new Prod("Juego Aventura", "5000", "500", 100, true),
            3L, new Prod("Disco Retirado", "8000", "0", 10, false),
            4L, new Prod("Disco Agotado", "7000", "0", 0, true));

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CatalogClient catalog;
    @MockitoBean
    private NotificacionesClient notificaciones;
    /** Lote 5 (HU-78): simulado. Por defecto no lanza nada = cuenta ACTIVA (las pruebas anteriores no cambian). */
    @MockitoBean
    private AccountsClient cuentas;

    @BeforeEach
    void catalogoSimulado() {
        when(catalog.cotizar(anyList())).thenAnswer(inv -> cotizar(inv.getArgument(0)));
        when(catalog.disponibilidad(anyList())).thenAnswer(inv -> disponibilidad(inv.getArgument(0)));
        when(catalog.aplicarOperacion(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.APLICADA, null));
    }

    private static Cotizacion cotizar(List<ItemSolicitado> items) {
        List<LineaCotizada> lineas = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        boolean todos = true;
        for (ItemSolicitado i : items) {
            Prod p = CATALOGO.get(i.productId());
            if (p == null || !p.activo()) {
                todos = false;
                lineas.add(new LineaCotizada(i.productId(), p == null ? null : p.nombre(), i.cantidad(), null, null, null,
                        BigDecimal.ZERO, null, false, p == null ? "Producto no encontrado" : RETIRADO));
                continue;
            }
            BigDecimal precio = new BigDecimal(p.precio());
            BigDecimal desc = new BigDecimal(p.descuento());
            BigDecimal fin = precio.subtract(desc);
            BigDecimal cant = BigDecimal.valueOf(i.cantidad());
            subtotal = subtotal.add(precio.multiply(cant));
            descuento = descuento.add(desc.multiply(cant));
            lineas.add(new LineaCotizada(i.productId(), p.nombre(), i.cantidad(), precio, desc, fin, fin.multiply(cant),
                    null, true, null));
        }
        return new Cotizacion(lineas, subtotal, descuento, subtotal.subtract(descuento), todos);
    }

    private static List<Disponibilidad> disponibilidad(List<ItemSolicitado> items) {
        return items.stream().map(i -> {
            Prod p = CATALOGO.get(i.productId());
            String d = p.stock() == 0 ? "AGOTADO" : p.stock() <= 5 ? "STOCK_CRITICO" : "DISPONIBLE";
            boolean alcanza = i.cantidad() <= p.stock();
            return new Disponibilidad(i.productId(), i.cantidad(), d, alcanza, p.activo() && alcanza);
        }).toList();
    }

    /** Disponibilidad simulada SIN stock para todo (para confirmar una falta de stock real). */
    private static List<Disponibilidad> sinStock(List<ItemSolicitado> items) {
        return items.stream().map(i -> new Disponibilidad(i.productId(), i.cantidad(), "AGOTADO", false, false)).toList();
    }

    /** Las primeras "normales" consultas responden con el catálogo; las siguientes, sin stock. */
    private void stockSeAgotaDespuesDe(int normales) {
        AtomicLong llamadas = new AtomicLong();
        when(catalog.disponibilidad(anyList())).thenAnswer(inv -> llamadas.incrementAndGet() <= normales
                ? disponibilidad(inv.getArgument(0)) : sinStock(inv.getArgument(0)));
    }

    // ===================== Ayudas =====================

    private static long nuevoCliente() {
        return USUARIOS.incrementAndGet();
    }

    private ResultActions como(String token, MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.header("Authorization", token));
    }

    private ResultActions json(String token, MockHttpServletRequestBuilder req, String body) throws Exception {
        return como(token, req.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions agregar(long uid, long productId, int cantidad) throws Exception {
        return json(TestTokens.cliente(uid), post("/api/v1/cart/items"),
                "{\"productId\":" + productId + ",\"cantidad\":" + cantidad + "}");
    }

    private static String entrega(String metodo) {
        return metodo.equals("RETIRO_TIENDA")
                ? "{\"metodoEntrega\":\"RETIRO_TIENDA\",\"nombreReceptor\":\"Ana Pérez\",\"telefono\":\"+56912345678\"}"
                : "{\"metodoEntrega\":\"DESPACHO_DOMICILIO\",\"nombreReceptor\":\"Ana Pérez\",\"telefono\":\"+56912345678\","
                + "\"direccion\":\"Av. Siempre Viva 123\",\"comuna\":\"Santiago\",\"referencia\":\"\"}";
    }

    private ResultActions confirmar(long uid, String total, String resultado, String metodo) throws Exception {
        String res = resultado == null ? "" : ",\"resultadoSimulado\":\"" + resultado + "\"";
        return json(TestTokens.cliente(uid), post("/api/v1/checkout/confirm"),
                "{\"datosEntrega\":" + entrega(metodo) + ",\"metodoPago\":\"WEBPAY\"" + res + ",\"totalEsperado\":" + total + "}");
    }

    /** Compra aprobada de 2 unidades del producto 1 ($20.000). Devuelve el id del pedido. */
    private long pedidoConfirmado(long uid, String metodo) throws Exception {
        agregar(uid, 1, 2).andExpect(status().isOk());
        String r = confirmar(uid, "20000", null, metodo).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(r, "$.pedido.id")).longValue();
    }

    private ResultActions cambiarEstado(long id, String estado, String motivo) throws Exception {
        String m = motivo == null ? "" : ",\"motivo\":\"" + motivo + "\"";
        return json(TestTokens.admin(), patch("/api/v1/admin/orders/" + id + "/status"), "{\"estado\":\"" + estado + "\"" + m + "}");
    }

    // ===================== Seguridad =====================

    @Test
    void sinToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/orders/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/orders")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(post("/api/v1/checkout/confirm").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteEnRutasDeAdministrador_403() throws Exception {
        String cliente = TestTokens.cliente(nuevoCliente());
        como(cliente, get("/api/v1/admin/orders")).andExpect(status().isForbidden());
        json(cliente, patch("/api/v1/admin/orders/1/status"), "{\"estado\":\"CONFIRMADO\"}").andExpect(status().isForbidden());
        json(cliente, post("/api/v1/admin/orders/1/dispatch"), "{\"transportista\":\"X\"}").andExpect(status().isForbidden());
    }

    /** T-09: un pedido ajeno se responde como inexistente; el dueño y el Administrador sí lo ven. */
    @Test
    void pedidoAjeno_404_yAccesoAutorizado() throws Exception {
        long dueno = nuevoCliente();
        long id = pedidoConfirmado(dueno, "DESPACHO_DOMICILIO");
        String otro = TestTokens.cliente(nuevoCliente());
        como(otro, get("/api/v1/orders/me/" + id)).andExpect(status().isNotFound());
        como(otro, get("/api/v1/orders/me/" + id + "/tracking")).andExpect(status().isNotFound());
        como(TestTokens.cliente(dueno), get("/api/v1/orders/me/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(dueno));
        como(TestTokens.admin(), get("/api/v1/admin/orders/" + id)).andExpect(status().isOk());
    }

    /** Lote 3: las rutas antiguas sin token ya no existen. */
    @Test
    void rutasAntiguasEliminadas() throws Exception {
        mockMvc.perform(get("/api/ordenes")).andExpect(status().isNotFound());
        // Sin controlador: Spring responde 404 o 405 según el método; lo importante es que ya no se crea ni cambia nada
        mockMvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idUsuario\":1,\"total\":1}")).andExpect(status().is4xxClientError());
        mockMvc.perform(patch("/api/ordenes/1/estado?estado=CANCELADO")).andExpect(status().is4xxClientError());
    }

    // ===================== Métodos publicados =====================

    @Test
    void metodosDeEntregaYPagoSonPublicos() throws Exception {
        mockMvc.perform(get("/api/v1/checkout/delivery-methods")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("RETIRO_TIENDA"))
                .andExpect(jsonPath("$[1].camposObligatorios", hasItem("direccion")));
        mockMvc.perform(get("/api/v1/checkout/payment-methods")).andExpect(status().isOk())
                .andExpect(jsonPath("$.simulacionAcademica").value(true))
                .andExpect(jsonPath("$.metodos.length()").value(2))
                .andExpect(jsonPath("$.resultadoPorDefecto").value("APROBADO"));
    }

    // ===================== Carrito =====================

    @Test
    void carritoRespetaStockYEstadoComercial() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 1, 2).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(20000));
        agregar(uid, 1, 4).andExpect(status().isConflict()); // 6 > stock 5
        agregar(uid, 3, 1).andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(RETIRADO));
        agregar(uid, 4, 1).andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("El producto está agotado"));
        agregar(uid, 99, 1).andExpect(status().isNotFound());
        agregar(uid, 2, 2).andExpect(status().isOk())
                .andExpect(jsonPath("$.descuento").value(1000))
                .andExpect(jsonPath("$.total").value(29000));
        como(TestTokens.cliente(uid), get("/api/v1/cart")).andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadProductos").value(2))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andExpect(jsonPath("$.listoParaComprar").value(true));
    }

    @Test
    void cantidadCeroNoEliminaYQuitarEsAparte() throws Exception {
        long uid = nuevoCliente();
        String token = TestTokens.cliente(uid);
        agregar(uid, 2, 1).andExpect(status().isOk());
        json(token, patch("/api/v1/cart/items/2"), "{\"cantidad\":0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidad").exists());
        json(token, patch("/api/v1/cart/items/2"), "{\"cantidad\":1.5}").andExpect(status().isBadRequest());
        json(token, patch("/api/v1/cart/items/2"), "{\"cantidad\":3}").andExpect(status().isOk())
                .andExpect(jsonPath("$.unidades").value(3));
        como(token, delete("/api/v1/cart/items/2")).andExpect(status().isOk()).andExpect(jsonPath("$.vacio").value(true));
        como(token, delete("/api/v1/cart/items/2")).andExpect(status().isNotFound());
        agregar(uid, 2, 1);
        como(token, delete("/api/v1/cart")).andExpect(status().isOk()).andExpect(jsonPath("$.vacio").value(true));
    }

    @Test
    void conCatalogCaidoSeVeElCarritoPeroNoSeAgrega() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 2, 1).andExpect(status().isOk());
        when(catalog.cotizar(anyList())).thenThrow(new ServicioNoDisponibleException("caído"));
        como(TestTokens.cliente(uid), get("/api/v1/cart")).andExpect(status().isOk())
                .andExpect(jsonPath("$.preciosActualizados").value(false))
                .andExpect(jsonPath("$.listoParaComprar").value(false));
        agregar(uid, 2, 1).andExpect(status().isServiceUnavailable());
    }

    // ===================== Checkout =====================

    @Test
    void datosDeEntregaSegunElMetodo_400() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 2, 1);
        String token = TestTokens.cliente(uid);
        json(token, post("/api/v1/checkout/validate"),
                "{\"datosEntrega\":{\"metodoEntrega\":\"RETIRO_TIENDA\",\"nombreReceptor\":\"Ana\"}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['datosEntrega.telefono']").exists());
        json(token, post("/api/v1/checkout/validate"),
                "{\"datosEntrega\":{\"metodoEntrega\":\"DESPACHO_DOMICILIO\",\"nombreReceptor\":\"Ana\",\"telefono\":\"912345678\"}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['datosEntrega.direccion']").exists())
                .andExpect(jsonPath("$.errores['datosEntrega.comuna']").exists());
        json(token, post("/api/v1/checkout/validate"),
                "{\"datosEntrega\":{\"metodoEntrega\":\"RETIRO_TIENDA\",\"nombreReceptor\":\"Ana\",\"telefono\":\"abc\"}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['datosEntrega.telefono']").exists());
        // Retiro sin dirección es válido (RF-COM-05)
        json(token, post("/api/v1/checkout/validate"), "{\"datosEntrega\":" + entrega("RETIRO_TIENDA") + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listo").value(true))
                .andExpect(jsonPath("$.total").value(4500))
                .andExpect(jsonPath("$.datosEntrega.direccion").doesNotExist());
    }

    @Test
    void validarConCarritoVacioNoPermiteAvanzar() throws Exception {
        json(TestTokens.cliente(nuevoCliente()), post("/api/v1/checkout/validate"), "{\"datosEntrega\":" + entrega("RETIRO_TIENDA") + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listo").value(false))
                .andExpect(jsonPath("$.problemas.carrito").exists());
    }

    @Test
    void compraAprobada_201_conTotalDelBackendYDescuentoIdempotente() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 1, 2);
        String r = confirmar(uid, "20000", "APROBADO", "DESPACHO_DOMICILIO").andExpect(status().isCreated())
                .andExpect(jsonPath("$.pago.simulacionAcademica").value(true))
                .andExpect(jsonPath("$.pedido.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.pedido.total").value(20000))
                .andExpect(jsonPath("$.pedido.items[0].precioUnitario").value(10000))
                .andExpect(jsonPath("$.pedido.datosEntrega.comuna").value("Santiago"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(r, "$.pedido.id")).longValue();
        String numero = JsonPath.read(r, "$.pedido.numeroPedido");
        org.junit.jupiter.api.Assertions.assertEquals(String.format("PED-%06d", id), numero);
        verify(catalog).aplicarOperacion(eq("ORDERS-PEDIDO-" + id + "-VENTA"), anyString(), eq("SALIDA"), anyList());
        verify(notificaciones).notificar(eq("ORDERS-PEDIDO-" + id + "-CREADO"), eq(uid), eq("PEDIDO_CREADO"),
                anyString(), anyString(), eq(id));
        como(TestTokens.cliente(uid), get("/api/v1/cart")).andExpect(jsonPath("$.vacio").value(true));
    }

    /** RN-14: pago simulado rechazado -> 402, sin pedido, sin descontar stock y con el carrito intacto. */
    @Test
    void pagoRechazado_402_sinPedidoNiStock() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 1, 1);
        confirmar(uid, "10000", "RECHAZADO", "RETIRO_TIENDA").andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.mensaje").exists());
        verify(catalog, never()).aplicarOperacion(anyString(), anyString(), anyString(), anyList());
        como(TestTokens.cliente(uid), get("/api/v1/orders/me")).andExpect(jsonPath("$.totalElementos").value(0));
        como(TestTokens.cliente(uid), get("/api/v1/cart")).andExpect(jsonPath("$.unidades").value(1));
    }

    /** RN-13: si el total cambió respecto del resumen, no se confirma. El total del frontend nunca se usa. */
    @Test
    void totalDistinto_409_sinPedido() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 1, 1);
        confirmar(uid, "1", null, "RETIRO_TIENDA").andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.totalEsperado").exists());
        como(TestTokens.cliente(uid), get("/api/v1/orders/me")).andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void catalogCaidoAlConfirmar_503_sinPedido() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 1, 1);
        when(catalog.cotizar(anyList())).thenThrow(new ServicioNoDisponibleException("caído"));
        confirmar(uid, "10000", null, "RETIRO_TIENDA").andExpect(status().isServiceUnavailable());
        como(TestTokens.cliente(uid), get("/api/v1/orders/me")).andExpect(jsonPath("$.totalElementos").value(0));
    }

    /**
     * Sin stock en el último momento: Catalog responde 409 y la disponibilidad CONFIRMA la falta de stock.
     * El pedido queda CANCELADO (sin stock que reponer) y el carrito vuelve.
     */
    @Test
    void sinStockAlDescontar_409_pedidoCanceladoYCarritoRestaurado() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 1, 2);
        stockSeAgotaDespuesDe(1); // la consulta del checkout ve stock; la verificación posterior al 409, no
        when(catalog.aplicarOperacion(anyString(), anyString(), eq("SALIDA"), anyList()))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.CONFLICTO, "Stock insuficiente para el producto 1", 409));
        confirmar(uid, "20000", null, "RETIRO_TIENDA").andExpect(status().isConflict());
        como(TestTokens.cliente(uid), get("/api/v1/orders/me")).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].estado").value("CANCELADO"));
        como(TestTokens.cliente(uid), get("/api/v1/cart")).andExpect(jsonPath("$.unidades").value(2));
        verify(catalog, never()).aplicarOperacion(anyString(), anyString(), eq("ENTRADA"), anyList());
    }

    /** Catalog no confirmó el descuento: 202 PENDIENTE; el Administrador confirma reintentando la MISMA operación. */
    @Test
    void descuentoNoConfirmado_202_yAdminConfirmaConLaMismaOperacion() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 1, 1);
        when(catalog.aplicarOperacion(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.NO_CONFIRMADA, "caído"))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.APLICADA, null));
        String r = confirmar(uid, "10000", null, "RETIRO_TIENDA").andExpect(status().isAccepted())
                .andExpect(jsonPath("$.pedido.estado").value("PENDIENTE"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(r, "$.pedido.id")).longValue();
        cambiarEstado(id, "CONFIRMADO", null).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADO"));
        verify(catalog, times(2)).aplicarOperacion(eq("ORDERS-PEDIDO-" + id + "-VENTA"), anyString(), eq("SALIDA"), anyList());
    }

    /** Doble clic: la segunda confirmación encuentra el carrito vacío y no crea otro pedido. */
    @Test
    void dobleConfirmacionNoCreaDosPedidos() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 2, 1);
        confirmar(uid, "4500", null, "RETIRO_TIENDA").andExpect(status().isCreated());
        confirmar(uid, "4500", null, "RETIRO_TIENDA").andExpect(status().isConflict());
        como(TestTokens.cliente(uid), get("/api/v1/orders/me")).andExpect(jsonPath("$.totalElementos").value(1));
    }

    // ===================== Administrador y seguimiento =====================

    @Test
    void flujoCompletoConDespachoYSeguimiento() throws Exception {
        long uid = nuevoCliente();
        long id = pedidoConfirmado(uid, "DESPACHO_DOMICILIO");
        cambiarEstado(id, "PREPARANDO", null).andExpect(status().isOk());
        json(TestTokens.admin(), post("/api/v1/admin/orders/" + id + "/dispatch"), "{\"transportista\":\"Chilexpress\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESPACHADO"))
                .andExpect(jsonPath("$.despacho.transportista").value("Chilexpress"));
        String numero = String.format("PED-%06d", id);
        como(TestTokens.cliente(uid), get("/api/v1/orders/me/" + numero + "/tracking")).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tu pedido va en camino"))
                .andExpect(jsonPath("$.pasos[3].actual").value(true))
                .andExpect(jsonPath("$.historial[0].usuarioResponsableId").doesNotExist());
        cambiarEstado(id, "ENTREGADO", null).andExpect(status().isOk());
        cambiarEstado(id, "PREPARANDO", null).andExpect(status().isConflict());
        como(TestTokens.admin(), get("/api/v1/admin/orders/" + id)).andExpect(jsonPath("$.historial.length()").value(5))
                .andExpect(jsonPath("$.historial[2].usuarioResponsableId").value(1));
        verify(notificaciones).notificar(eq("ORDERS-PEDIDO-" + id + "-DESPACHADO"), eq(uid), eq("PEDIDO_ACTUALIZADO"),
                anyString(), eq("Tu pedido va en camino (" + numero + ")"), eq(id));
    }

    /** T-07: cancelar dos veces -> CANCELADO terminal y stock repuesto UNA vez. */
    @Test
    void cancelacionAdministrativaReponeStockUnaVez() throws Exception {
        long id = pedidoConfirmado(nuevoCliente(), "RETIRO_TIENDA");
        cambiarEstado(id, "CANCELADO", null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.motivo").exists());
        cambiarEstado(id, "CANCELADO", "Cliente sin respuesta").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
        cambiarEstado(id, "CANCELADO", "Otra vez").andExpect(status().isConflict());
        cambiarEstado(id, "DESPACHADO", null).andExpect(status().isConflict()); // T-08
        verify(catalog, times(1)).aplicarOperacion(eq("ORDERS-PEDIDO-" + id + "-CANCELACION"), anyString(), eq("ENTRADA"), anyList());
    }

    /** PENDIENTE: no se sabe si el descuento se aplicó. Se reenvía la MISMA venta; solo si Catalog la acepta se repone. */
    @Test
    void cancelarPendienteSoloReponeSiElDescuentoSeAplico() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 2, 1);
        when(catalog.aplicarOperacion(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.NO_CONFIRMADA, "caído"));
        String r = confirmar(uid, "4500", null, "RETIRO_TIENDA").andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        long sinDescuento = ((Number) JsonPath.read(r, "$.pedido.id")).longValue();
        when(catalog.aplicarOperacion(anyString(), anyString(), eq("SALIDA"), anyList()))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.CONFLICTO, "Stock insuficiente", 409));
        when(catalog.disponibilidad(anyList())).thenAnswer(inv -> sinStock(inv.getArgument(0)));
        cambiarEstado(sinDescuento, "CANCELADO", "Sin stock").andExpect(status().isOk());
        when(catalog.disponibilidad(anyList())).thenAnswer(inv -> disponibilidad(inv.getArgument(0)));
        verify(catalog, never()).aplicarOperacion(anyString(), anyString(), eq("ENTRADA"), anyList());

        long uid2 = nuevoCliente();
        agregar(uid2, 2, 1);
        when(catalog.aplicarOperacion(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.NO_CONFIRMADA, "caído"))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.APLICADA, null));
        String r2 = confirmar(uid2, "4500", null, "RETIRO_TIENDA").andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        long conDescuento = ((Number) JsonPath.read(r2, "$.pedido.id")).longValue();
        cambiarEstado(conDescuento, "CANCELADO", "Cliente desistió").andExpect(status().isOk());
        verify(catalog, times(1)).aplicarOperacion(eq("ORDERS-PEDIDO-" + conDescuento + "-CANCELACION"), anyString(),
                eq("ENTRADA"), anyList());
    }

    @Test
    void despachadoNoSeCancela() throws Exception {
        long id = pedidoConfirmado(nuevoCliente(), "DESPACHO_DOMICILIO");
        cambiarEstado(id, "PREPARANDO", null);
        cambiarEstado(id, "DESPACHADO", null).andExpect(status().isOk());
        cambiarEstado(id, "CANCELADO", "Tarde").andExpect(status().isConflict());
        verify(catalog, never()).aplicarOperacion(anyString(), anyString(), eq("ENTRADA"), anyList());
    }

    @Test
    void correccionDeEntregaSoloAntesDelDespacho() throws Exception {
        long id = pedidoConfirmado(nuevoCliente(), "DESPACHO_DOMICILIO");
        json(TestTokens.admin(), patch("/api/v1/admin/orders/" + id + "/delivery-data"), "{\"telefono\":\"+56987654321\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datosEntrega.telefono").value("+56987654321"))
                .andExpect(jsonPath("$.historial[2].descripcion").value("Campos corregidos: telefono"));
        json(TestTokens.admin(), patch("/api/v1/admin/orders/" + id + "/delivery-data"), "{\"direccion\":\"\"}")
                .andExpect(status().isBadRequest()); // no puede quedar vacío un obligatorio
        cambiarEstado(id, "PREPARANDO", null);
        cambiarEstado(id, "DESPACHADO", null);
        json(TestTokens.admin(), patch("/api/v1/admin/orders/" + id + "/delivery-data"), "{\"telefono\":\"+56911111111\"}")
                .andExpect(status().isConflict());

        long retiro = pedidoConfirmado(nuevoCliente(), "RETIRO_TIENDA");
        json(TestTokens.admin(), patch("/api/v1/admin/orders/" + retiro + "/delivery-data"), "{\"direccion\":\"Calle 12345\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.direccion").exists());
    }

    @Test
    void listadosConBusquedaYFiltros() throws Exception {
        long uid = nuevoCliente();
        long id = pedidoConfirmado(uid, "RETIRO_TIENDA");
        String numero = String.format("PED-%06d", id);
        como(TestTokens.admin(), get("/api/v1/admin/orders?q=" + numero)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].userId").value(uid));
        como(TestTokens.admin(), get("/api/v1/admin/orders?userId=" + uid + "&estado=CANCELADO"))
                .andExpect(jsonPath("$.totalElementos").value(0));
        como(TestTokens.cliente(uid), get("/api/v1/orders/me?q=" + numero + "&estado=CONFIRMADO"))
                .andExpect(jsonPath("$.totalElementos").value(1));
        como(TestTokens.cliente(nuevoCliente()), get("/api/v1/orders/me?q=" + numero))
                .andExpect(jsonPath("$.totalElementos").value(0)); // RF-MIP-04: solo propios
        como(TestTokens.admin(), get("/api/v1/admin/orders?desde=2026-05-02&hasta=2026-05-01")).andExpect(status().isBadRequest());
        como(TestTokens.admin(), get("/api/v1/admin/orders?size=500")).andExpect(status().isBadRequest());
    }

    @Test
    void seguimientoDeCanceladoNoSeMuestraAvanzando() throws Exception {
        long uid = nuevoCliente();
        long id = pedidoConfirmado(uid, "RETIRO_TIENDA");
        cambiarEstado(id, "CANCELADO", "Sin stock real");
        como(TestTokens.cliente(uid), get("/api/v1/orders/me/" + id + "/tracking")).andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelado").value(true))
                .andExpect(jsonPath("$.mensaje").value("Tu pedido fue cancelado"))
                .andExpect(jsonPath("$.pasos[1].completado").value(true))
                .andExpect(jsonPath("$.pasos[1].actual").value(false))
                .andExpect(jsonPath("$.pasos[2].completado").value(false));
    }

    // ===================== Contrato interno (PostSale, Lote 4) =====================

    @Test
    void internoConsultaYCancelacionIdempotente() throws Exception {
        long uid = nuevoCliente();
        long id = pedidoConfirmado(uid, "RETIRO_TIENDA");
        mockMvc.perform(get("/api/v1/internal/orders/" + id).header("Authorization", TokensServicioPrueba.bearer(Servicios.POSTSALE))).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(uid))
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].cantidad").value(2));
        String body = "{\"solicitudId\":7,\"motivo\":\"Cancelación aprobada\"}";
        mockMvc.perform(post("/api/v1/internal/orders/" + id + "/cancel").header("Authorization", TokensServicioPrueba.bearer(Servicios.POSTSALE)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.yaCancelado").value(false))
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
        mockMvc.perform(post("/api/v1/internal/orders/" + id + "/cancel").header("Authorization", TokensServicioPrueba.bearer(Servicios.POSTSALE)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.yaCancelado").value(true));
        verify(catalog, times(1)).aplicarOperacion(eq("ORDERS-PEDIDO-" + id + "-CANCELACION"), anyString(), eq("ENTRADA"), anyList());
        mockMvc.perform(get("/api/v1/internal/orders/999999").header("Authorization", TokensServicioPrueba.bearer(Servicios.POSTSALE))).andExpect(status().isNotFound());
    }

    @Test
    void internoNoCancelaDespachado_409() throws Exception {
        long id = pedidoConfirmado(nuevoCliente(), "DESPACHO_DOMICILIO");
        cambiarEstado(id, "PREPARANDO", null);
        cambiarEstado(id, "DESPACHADO", null);
        mockMvc.perform(post("/api/v1/internal/orders/" + id + "/cancel").header("Authorization", TokensServicioPrueba.bearer(Servicios.POSTSALE)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"solicitudId\":8,\"motivo\":\"Tarde\"}")).andExpect(status().isConflict());
        verify(notificaciones, never()).notificar(eq("ORDERS-PEDIDO-" + id + "-CANCELADO"), anyLong(), anyString(),
                anyString(), anyString(), anyLong());
    }

    // ===================== Revisión Lote 3: errores de Catalog que NO prueban falta de stock =====================

    private long pendientePor(RespuestaOperacion respuesta) throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 2, 1);
        when(catalog.aplicarOperacion(anyString(), anyString(), eq("SALIDA"), anyList())).thenReturn(respuesta);
        String r = confirmar(uid, "4500", null, "RETIRO_TIENDA").andExpect(status().isAccepted())
                .andExpect(jsonPath("$.pedido.estado").value("PENDIENTE"))
                .andReturn().getResponse().getContentAsString();
        como(TestTokens.cliente(uid), get("/api/v1/cart")).andExpect(jsonPath("$.vacio").value(true));
        return ((Number) JsonPath.read(r, "$.pedido.id")).longValue();
    }

    /** 409 pero la disponibilidad dice que SÍ hay stock: no se cancela; queda PENDIENTE para revisión. */
    @Test
    void conflictoSinFaltaDeStockConfirmadaNoCancela() throws Exception {
        long id = pendientePor(new RespuestaOperacion(ResultadoOperacion.CONFLICTO, "Stock insuficiente para el producto 2", 409));
        como(TestTokens.admin(), get("/api/v1/admin/orders/" + id)).andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.historial[1].motivo").value("Descuento de stock por confirmar"));
    }

    /** operacionId ya registrada con otro contenido: aunque falte stock, NO se interpreta como falta de stock. */
    @Test
    void operacionIdRepetidaNoSeInterpretaComoFaltaDeStock() throws Exception {
        when(catalog.disponibilidad(anyList())).thenAnswer(inv -> disponibilidad(inv.getArgument(0)));
        stockSeAgotaDespuesDe(2); // carrito + checkout normales; después, sin stock
        pendientePor(new RespuestaOperacion(ResultadoOperacion.CONFLICTO,
                "La operación ORDERS-PEDIDO-1-VENTA ya fue registrada con otros productos, cantidades o motivo", 409));
    }

    /** 400 / 404 de Catalog (datos inválidos u otro error): tampoco cancela la compra. */
    @Test
    void datosInvalidosEnCatalogNoCancelan() throws Exception {
        pendientePor(new RespuestaOperacion(ResultadoOperacion.RECHAZADA, "operacionId no válido", 400));
    }

    /** Confirmar un PENDIENTE cuando Catalog responde un conflicto que no es falta de stock: 409 y sigue PENDIENTE. */
    @Test
    void confirmarPendienteConConflictoNoCambiaElPedido() throws Exception {
        long id = pendientePor(new RespuestaOperacion(ResultadoOperacion.NO_CONFIRMADA, "caído"));
        when(catalog.aplicarOperacion(anyString(), anyString(), eq("SALIDA"), anyList()))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.CONFLICTO,
                        "La operación ORDERS-PEDIDO-" + id + "-VENTA ya fue registrada con otros productos, cantidades o motivo", 409));
        cambiarEstado(id, "CONFIRMADO", null).andExpect(status().isConflict());
        cambiarEstado(id, "CANCELADO", "Revisión").andExpect(status().isConflict()); // no se sabe si se descontó
        como(TestTokens.admin(), get("/api/v1/admin/orders/" + id)).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        verify(catalog, never()).aplicarOperacion(anyString(), anyString(), eq("ENTRADA"), anyList());
    }

    // ===================== Revisión Lote 3: cancelación con la reposición sin confirmar =====================

    /**
     * Catalog no confirma la reposición: el pedido queda CANCELADO (ya no avanza) con la reposición PENDIENTE.
     * Repetir la cancelación reintenta solo la reposición con la MISMA operación; después ya no se puede repetir.
     */
    @Test
    void reposicionNoConfirmadaQuedaPendienteYSeReintenta() throws Exception {
        long id = pedidoConfirmado(nuevoCliente(), "RETIRO_TIENDA");
        when(catalog.aplicarOperacion(anyString(), anyString(), eq("ENTRADA"), anyList()))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.NO_CONFIRMADA, "caído"))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.APLICADA, null, 200));
        cambiarEstado(id, "CANCELADO", "Cliente desistió").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.reposicionStockPendiente").value(true));
        cambiarEstado(id, "PREPARANDO", null).andExpect(status().isConflict()); // cancelado: no avanza
        cambiarEstado(id, "CANCELADO", null).andExpect(status().isOk())   // reintento (sin motivo nuevo)
                .andExpect(jsonPath("$.reposicionStockPendiente").value(false));
        cambiarEstado(id, "CANCELADO", "Otra vez").andExpect(status().isConflict());
        verify(catalog, times(2)).aplicarOperacion(eq("ORDERS-PEDIDO-" + id + "-CANCELACION"), anyString(), eq("ENTRADA"), anyList());
    }

    /** Lo mismo desde PostSale: el reintento completa la reposición pendiente y responde yaCancelado=true. */
    @Test
    void postVentaReintentaLaReposicionPendiente() throws Exception {
        long id = pedidoConfirmado(nuevoCliente(), "RETIRO_TIENDA");
        when(catalog.aplicarOperacion(anyString(), anyString(), eq("ENTRADA"), anyList()))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.NO_CONFIRMADA, "caído"))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.APLICADA, null, 200));
        String body = "{\"solicitudId\":9,\"motivo\":\"Cancelación aprobada\"}";
        mockMvc.perform(post("/api/v1/internal/orders/" + id + "/cancel").header("Authorization", TokensServicioPrueba.bearer(Servicios.POSTSALE)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.yaCancelado").value(false))
                .andExpect(jsonPath("$.reposicionStockPendiente").value(true));
        mockMvc.perform(post("/api/v1/internal/orders/" + id + "/cancel").header("Authorization", TokensServicioPrueba.bearer(Servicios.POSTSALE)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.yaCancelado").value(true))
                .andExpect(jsonPath("$.reposicionStockPendiente").value(false));
        mockMvc.perform(post("/api/v1/internal/orders/" + id + "/cancel").header("Authorization", TokensServicioPrueba.bearer(Servicios.POSTSALE)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.yaCancelado").value(true));
        verify(catalog, times(2)).aplicarOperacion(eq("ORDERS-PEDIDO-" + id + "-CANCELACION"), anyString(), eq("ENTRADA"), anyList());
    }

    // ===================== Lote 5 · Integraciones de HU-78 =====================

    /** HU-78 (ERS 13.8): Accounts pregunta por pedidos activos. Activos = PENDIENTE, CONFIRMADO, PREPARANDO, DESPACHADO. */
    @Test
    void internoPedidosActivosParaAccounts() throws Exception {
        long sinPedidos = nuevoCliente();
        mockMvc.perform(get("/api/v1/internal/orders/users/" + sinPedidos + "/active-exists").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS))).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(sinPedidos)).andExpect(jsonPath("$.existe").value(false));
        long uid = nuevoCliente();
        long id = pedidoConfirmado(uid, "DESPACHO_DOMICILIO");
        mockMvc.perform(get("/api/v1/internal/orders/users/" + uid + "/active-exists").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)))
                .andExpect(jsonPath("$.existe").value(true));
        cambiarEstado(id, "PREPARANDO", null).andExpect(status().isOk());
        json(TestTokens.admin(), post("/api/v1/admin/orders/" + id + "/dispatch"), "{\"transportista\":\"Chilexpress\"}")
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/internal/orders/users/" + uid + "/active-exists").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)))
                .andExpect(jsonPath("$.existe").value(true));           // DESPACHADO sigue activo (RN-36)
        cambiarEstado(id, "ENTREGADO", null).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/internal/orders/users/" + uid + "/active-exists").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)))
                .andExpect(jsonPath("$.existe").value(false));          // ENTREGADO ya no bloquea
        // La consulta no toca el pedido: el historial se conserva
        como(TestTokens.admin(), get("/api/v1/admin/orders/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));
    }

    /** RN-37 / RN-44 / T-06: cuenta EN_ELIMINACION, ELIMINADA o no habilitada -> 403, sin pedido ni descuento. */
    @Test
    void compraRechazadaSiLaCuentaNoPuedeOperar() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 1, 1).andExpect(status().isOk());
        doThrow(new AccesoDenegadoException("Tu cuenta no puede realizar nuevas operaciones")).when(cuentas)
                .exigirCuentaActiva(uid);
        confirmar(uid, "10000", null, "RETIRO_TIENDA").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        // Cierre (RN-44): con la cuenta no ACTIVA tampoco se puede consultar
        como(TestTokens.cliente(uid), get("/api/v1/orders/me")).andExpect(status().isForbidden());
        // Para comprobar que no quedó nada, la cuenta vuelve a estar ACTIVA (simulado)
        doNothing().when(cuentas).exigirCuentaActiva(uid);
        como(TestTokens.cliente(uid), get("/api/v1/orders/me")).andExpect(jsonPath("$.totalElementos").value(0));
        como(TestTokens.cliente(uid), get("/api/v1/cart")).andExpect(jsonPath("$.unidades").value(1));
        verify(catalog, never()).aplicarOperacion(anyString(), anyString(), anyString(), anyList());
    }

    /** Accounts no responde: no se puede confirmar ACTIVA -> 503, sin pedido (nunca se asume ACTIVA). */
    @Test
    void compraRechazadaSiAccountsNoResponde() throws Exception {
        long uid = nuevoCliente();
        agregar(uid, 1, 1).andExpect(status().isOk());
        doThrow(new ServicioNoDisponibleException("No fue posible confirmar el estado de tu cuenta")).when(cuentas)
                .exigirCuentaActiva(uid);
        confirmar(uid, "10000", null, "RETIRO_TIENDA").andExpect(status().isServiceUnavailable());
        // Cierre (RN-44): sin respuesta de Accounts tampoco se atienden consultas (nunca se asume ACTIVA)
        como(TestTokens.cliente(uid), get("/api/v1/orders/me")).andExpect(status().isServiceUnavailable());
        doNothing().when(cuentas).exigirCuentaActiva(uid);
        como(TestTokens.cliente(uid), get("/api/v1/orders/me")).andExpect(jsonPath("$.totalElementos").value(0));
        verify(catalog, never()).aplicarOperacion(anyString(), anyString(), anyString(), anyList());
    }

    /** Cuenta ACTIVA: la compra sigue funcionando y se consulta a Accounts con el usuario del token. */
    @Test
    void compraConCuentaActivaConsultaAccountsConElUsuarioDelToken() throws Exception {
        long uid = nuevoCliente();
        pedidoConfirmado(uid, "RETIRO_TIENDA");
        // Cierre (RN-44): se consulta en cada petición del cliente (y no se repite dentro de una misma petición)
        verify(cuentas, atLeastOnce()).exigirCuentaActiva(uid);
    }

    /** Cierre (RF-SOP-01): ms-support puede consultar el dueño de un pedido; otros servicios no autorizados no. */
    @Test
    void internoConsultaDePedidoAutorizadaParaSupport() throws Exception {
        long uid = nuevoCliente();
        long id = pedidoConfirmado(uid, "RETIRO_TIENDA");
        mockMvc.perform(get("/api/v1/internal/orders/" + id).header("Authorization", TokensServicioPrueba.bearer(Servicios.SUPPORT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(uid));
        mockMvc.perform(get("/api/v1/internal/orders/" + id).header("Authorization", TokensServicioPrueba.bearer(Servicios.CATALOG)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/internal/orders/" + id).header("Authorization", TestTokens.cliente(uid)))
                .andExpect(status().isUnauthorized());
    }
}
