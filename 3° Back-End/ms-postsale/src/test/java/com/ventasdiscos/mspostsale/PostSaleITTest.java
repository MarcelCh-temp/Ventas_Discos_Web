package com.ventasdiscos.mspostsale;

import com.jayway.jsonpath.JsonPath;
import com.ventasdiscos.mspostsale.security.Servicios;
import com.ventasdiscos.mspostsale.client.AccountsClient;
import com.ventasdiscos.mspostsale.client.InventoryClient;
import com.ventasdiscos.mspostsale.client.NotificationsClient;
import com.ventasdiscos.mspostsale.client.OrdersClient;
import com.ventasdiscos.mspostsale.client.OrdersDtos.CancelacionOrders;
import com.ventasdiscos.mspostsale.client.OrdersDtos.ItemPedido;
import com.ventasdiscos.mspostsale.client.OrdersDtos.PedidoOrders;
import com.ventasdiscos.mspostsale.exception.AccesoDenegadoException;
import com.ventasdiscos.mspostsale.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mspostsale.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mspostsale.exception.ServicioNoDisponibleException;
import com.ventasdiscos.mspostsale.service.DevolucionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lote 4 · Pruebas de punta a punta de PostSale (HTTP -> Controller -> Service -> H2). OrdersClient, InventoryClient
 * (Catalog) y NotificationsClient se simulan (@MockitoBean): no se levantan otros MS. Orders se simula con un mapa de
 * pedidos que imita el contrato interno del Lote 3 (cancelar es idempotente y responde 409 si ya fue despachado).
 * SIN @Transactional a propósito: los avisos "después del commit" se ejecutan de verdad. Cada test usa Clientes y
 * pedidos propios para no mezclar datos.
 * IMPLEMENTADO — SIN EJECUTAR: estas pruebas están escritas, no se han ejecutado en este entorno.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PostSaleITTest {

    private static final AtomicLong USUARIOS = new AtomicLong(7000);
    private static final AtomicLong PEDIDOS_SEQ = new AtomicLong(90000);
    private static final String ADMIN = TestTokens.admin();           // userId 1
    private static final String ADMIN_2 = TestTokens.bearer(2L, "ADMINISTRADOR");

    /** "Orders" simulado: pedidoId -> pedido. */
    private static final Map<Long, PedidoOrders> PEDIDOS = new ConcurrentHashMap<>();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DevolucionService devolucionService;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoBean
    private OrdersClient orders;
    @MockitoBean
    private InventoryClient inventory;
    @MockitoBean
    private NotificationsClient notificaciones;
    /** Lote 5 (HU-78): simulado. Por defecto no lanza nada = cuenta ACTIVA (las pruebas anteriores no cambian). */
    @MockitoBean
    private AccountsClient cuentas;

    @BeforeEach
    void ordersYCatalogSimulados() {
        when(orders.obtener(anyLong())).thenAnswer(inv -> pedido(inv.getArgument(0)));
        when(orders.cancelar(anyLong(), anyLong(), anyString())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            PedidoOrders p = pedido(id);
            if (p.estado().equals("DESPACHADO") || p.estado().equals("ENTREGADO")) {
                throw new OperacionNoPermitidaException("Un pedido " + p.estado() + " no se puede cancelar");
            }
            boolean ya = p.estado().equals("CANCELADO");
            PEDIDOS.put(id, conEstado(p, "CANCELADO"));
            return new CancelacionOrders(id, p.numeroPedido(), "CANCELADO", ya, false);
        });
        when(inventory.reponer(anyString(), anyString(), anyLong(), anyInt()))
                .thenReturn(new InventoryClient.Respuesta(InventoryClient.Resultado.APLICADA, 201, null));
    }

    // ===================== Apoyo =====================

    private static PedidoOrders pedido(Long id) {
        PedidoOrders p = PEDIDOS.get(id);
        if (p == null) {
            throw new RecursoNoEncontradoException("Pedido no encontrado");
        }
        return p;
    }

    private static PedidoOrders conEstado(PedidoOrders p, String estado) {
        return new PedidoOrders(p.id(), p.numeroPedido(), p.userId(), estado, p.fechaCompra(), p.total(), p.items());
    }

    /** Pedido nuevo en "Orders": productos 2 (2 unidades) y 5 (1 unidad). */
    private static long nuevoPedido(long userId, String estado) {
        long id = PEDIDOS_SEQ.incrementAndGet();
        PEDIDOS.put(id, new PedidoOrders(id, String.format("PED-%06d", id), userId, estado, LocalDateTime.now(),
                new BigDecimal("19000"), List.of(new ItemPedido(2L, "Juego Aventura", 2), new ItemPedido(5L, "Disco Jazz", 1))));
        return id;
    }

    private static void cambiarEstado(long pedidoId, String estado) {
        PEDIDOS.put(pedidoId, conEstado(PEDIDOS.get(pedidoId), estado));
    }

    private static long nuevoCliente() {
        return USUARIOS.incrementAndGet();
    }

    private ResultActions enviar(String url, String token, String json) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            req.header("Authorization", token);
        }
        return mockMvc.perform(req);
    }

    private ResultActions consultar(String url, String token) throws Exception {
        var req = get(url);
        if (token != null) {
            req.header("Authorization", token);
        }
        return mockMvc.perform(req);
    }

    private static long id(ResultActions r, String ruta) throws Exception {
        Number n = JsonPath.read(r.andReturn().getResponse().getContentAsString(), ruta);
        return n.longValue();
    }

    private long crearCancelacion(long userId, long pedidoId) throws Exception {
        return id(enviar("/api/v1/post-sale/cancellations", TestTokens.cliente(userId),
                "{\"pedidoId\":" + pedidoId + ",\"motivoSolicitud\":\"Me equivoqué de disco\"}")
                .andExpect(status().isCreated()), "$.id");
    }

    private long crearDevolucion(long userId, long pedidoId, String items) throws Exception {
        return id(enviar("/api/v1/post-sale/returns", TestTokens.cliente(userId),
                "{\"pedidoId\":" + pedidoId + ",\"motivoSolicitud\":\"Llegó rayado\",\"items\":" + items + "}")
                .andExpect(status().isCreated()), "$.id");
    }

    private ResultActions resolver(String tipo, long id, String token, boolean aprobar) throws Exception {
        return enviar("/api/v1/admin/post-sale/" + tipo + "/" + id + "/resolve", token,
                "{\"aprobar\":" + aprobar + ",\"motivo\":\"Revisado por el equipo\"}");
    }

    private long devolucionAprobada(long userId, long pedidoId) throws Exception {
        long id = crearDevolucion(userId, pedidoId,
                "[{\"productoId\":2,\"cantidadSolicitada\":2},{\"productoId\":5,\"cantidadSolicitada\":1}]");
        resolver("returns", id, ADMIN, true).andExpect(status().isOk());
        return id;
    }

    /** ids de los ItemDevolucion en el orden del detalle (producto 2 y luego 5). */
    private List<Long> itemsDe(long solicitudId) throws Exception {
        String json = consultar("/api/v1/admin/post-sale/requests?id=" + solicitudId, ADMIN)
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(json, "$.contenido[0].items[*].id");
        List<Long> r = new ArrayList<>();
        ids.forEach(n -> r.add(n.longValue()));
        return r;
    }

    // ===================== Seguridad y formato =====================

    @Test
    void sinTokenResponde401ConApiError() throws Exception {
        consultar("/api/v1/post-sale/requests/me", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/post-sale/requests/me"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void tokenInvalidoResponde401() throws Exception {
        consultar("/api/v1/post-sale/requests/me", "Bearer no-es-un-jwt").andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoPuedeUsarRutasDeAdministrador() throws Exception {
        String cliente = TestTokens.cliente(nuevoCliente());
        consultar("/api/v1/admin/post-sale/requests", cliente).andExpect(status().isForbidden());
        resolver("cancellations", 1, cliente, true).andExpect(status().isForbidden());
        resolver("returns", 1, cliente, true).andExpect(status().isForbidden());
        enviar("/api/v1/admin/post-sale/returns/1/reception", cliente,
                "{\"items\":[{\"itemDevolucionId\":1,\"cantidadRecibida\":1,\"condicion\":\"BUENA\"}]}")
                .andExpect(status().isForbidden());
    }

    @Test
    void lasRutasAntiguasSinApiV1YaNoExisten() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "CONFIRMADO");
        enviar("/post-sale/cancellations", null,
                "{\"pedidoId\":" + pedido + ",\"usuarioId\":" + user + ",\"motivoSolicitud\":\"x\"}")
                .andExpect(status().is4xxClientError());
        consultar("/post-sale/requests/me?usuarioId=" + user, null).andExpect(status().isNotFound());
        consultar("/admin/post-sale/requests", null).andExpect(status().isNotFound());
    }

    @Test
    void elUsuarioSaleDelTokenAunqueElCuerpoTraigaOtro() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "CONFIRMADO");
        enviar("/api/v1/post-sale/cancellations", TestTokens.cliente(user),
                "{\"pedidoId\":" + pedido + ",\"usuarioId\":999999,\"motivoSolicitud\":\"Ya no lo quiero\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(user));
    }

    // ===================== Cancelación (HU-68, HU-74) =====================

    @Test
    void solicitarCancelacionDeUnPedidoPropioConfirmado() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "CONFIRMADO");
        ResultActions r = enviar("/api/v1/post-sale/cancellations", TestTokens.cliente(user),
                "{\"pedidoId\":" + pedido + ",\"motivoSolicitud\":\"Me equivoqué de disco\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("CANCELACION"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.pedidoId").value(pedido))
                .andExpect(jsonPath("$.motivoSolicitud").value("Me equivoqué de disco"))
                .andExpect(jsonPath("$.usuarioResolucionId").value(nullValue()));
        long id = id(r, "$.id");
        verify(notificaciones).enviar(eq("cancelacion-" + id + "-creada"), eq(user), eq("CANCELACION_CREADA"),
                anyString(), anyString(), eq(id));
    }

    @Test
    void cancelacionPermitidaSoloAntesDelDespacho() throws Exception {
        long user = nuevoCliente();
        String token = TestTokens.cliente(user);
        for (String estado : List.of("PENDIENTE", "PREPARANDO")) {
            crearCancelacion(user, nuevoPedido(user, estado));
        }
        for (String estado : List.of("DESPACHADO", "ENTREGADO", "CANCELADO")) {
            enviar("/api/v1/post-sale/cancellations", token,
                    "{\"pedidoId\":" + nuevoPedido(user, estado) + ",\"motivoSolicitud\":\"x\"}")
                    .andExpect(status().isConflict());
        }
    }

    @Test
    void pedidoAjenoOInexistenteResponde404() throws Exception {
        long dueno = nuevoCliente();
        long otro = nuevoCliente();
        long pedido = nuevoPedido(dueno, "CONFIRMADO");
        enviar("/api/v1/post-sale/cancellations", TestTokens.cliente(otro),
                "{\"pedidoId\":" + pedido + ",\"motivoSolicitud\":\"x\"}").andExpect(status().isNotFound());
        enviar("/api/v1/post-sale/cancellations", TestTokens.cliente(otro),
                "{\"pedidoId\":123456789,\"motivoSolicitud\":\"x\"}").andExpect(status().isNotFound());
    }

    @Test
    void cancelacionSinMotivoResponde400() throws Exception {
        long user = nuevoCliente();
        enviar("/api/v1/post-sale/cancellations", TestTokens.cliente(user),
                "{\"pedidoId\":" + nuevoPedido(user, "CONFIRMADO") + ",\"motivoSolicitud\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivoSolicitud").exists());
    }

    @Test
    void noSePermiteUnaSegundaCancelacionPendiente() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "CONFIRMADO");
        crearCancelacion(user, pedido);
        enviar("/api/v1/post-sale/cancellations", TestTokens.cliente(user),
                "{\"pedidoId\":" + pedido + ",\"motivoSolicitud\":\"Otra vez\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void ordersCaidoAlSolicitarResponde503YNoGuardaNada() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "CONFIRMADO");
        doThrow(new ServicioNoDisponibleException("Orders no responde")).when(orders).obtener(pedido);
        enviar("/api/v1/post-sale/cancellations", TestTokens.cliente(user),
                "{\"pedidoId\":" + pedido + ",\"motivoSolicitud\":\"x\"}").andExpect(status().isServiceUnavailable());
        consultar("/api/v1/post-sale/requests/me", TestTokens.cliente(user))
                .andExpect(jsonPath("$.contenido", hasSize(0)));
    }

    @Test
    void aprobarCancelacionLlamaAOrdersUnaVezYGuardaAlResponsable() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "PREPARANDO");
        long id = crearCancelacion(user, pedido);
        resolver("cancellations", id, ADMIN, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitud.estado").value("APROBADA"))
                .andExpect(jsonPath("$.solicitud.usuarioResolucionId").value(1))
                .andExpect(jsonPath("$.solicitud.motivoResolucion").value("Revisado por el equipo"))
                .andExpect(jsonPath("$.solicitud.fechaResolucion").exists())
                .andExpect(jsonPath("$.estadoPedido").value("CANCELADO"))
                .andExpect(jsonPath("$.reposicionStockPendiente").value(false));
        verify(orders, times(1)).cancelar(eq(pedido), eq(id), anyString());
        // La reposición de una cancelación la hace Orders: PostSale no llama a Catalog (sin reposición duplicada)
        verify(inventory, never()).reponer(anyString(), anyString(), anyLong(), anyInt());
        verify(notificaciones).enviar(eq("cancelacion-" + id + "-resuelta"), eq(user), eq("CANCELACION_RESUELTA"),
                anyString(), anyString(), eq(id));
        // El Cliente ve la resolución, pero no al Administrador responsable
        consultar("/api/v1/post-sale/requests/" + id, TestTokens.cliente(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.usuarioResolucionId").value(nullValue()));
    }

    @Test
    void repetirLaAprobacionNoCambiaAlResponsableNiLaFecha() throws Exception {
        long user = nuevoCliente();
        long id = crearCancelacion(user, nuevoPedido(user, "CONFIRMADO"));
        resolver("cancellations", id, ADMIN, true).andExpect(status().isOk());
        String guardada = consultar("/api/v1/admin/post-sale/requests?id=" + id, ADMIN).andReturn().getResponse()
                .getContentAsString();
        String fecha = JsonPath.read(guardada, "$.contenido[0].fechaResolucion");
        resolver("cancellations", id, ADMIN_2, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitud.estado").value("APROBADA"))
                .andExpect(jsonPath("$.solicitud.usuarioResolucionId").value(1))
                .andExpect(jsonPath("$.solicitud.fechaResolucion").value(fecha));
        // Rechazar algo ya aprobado no se permite
        resolver("cancellations", id, ADMIN, false).andExpect(status().isConflict());
        verify(notificaciones, times(1)).enviar(eq("cancelacion-" + id + "-resuelta"), anyLong(), anyString(),
                anyString(), anyString(), anyLong());
    }

    @Test
    void siElPedidoYaFueDespachadoNoSeApruebaYQuedaPendiente() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "PREPARANDO");
        long id = crearCancelacion(user, pedido);
        cambiarEstado(pedido, "DESPACHADO");    // RF-POS-07: la solicitud se creó cuando aún era cancelable
        resolver("cancellations", id, ADMIN, true).andExpect(status().isConflict());
        consultar("/api/v1/post-sale/requests/" + id, TestTokens.cliente(user))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        resolver("cancellations", id, ADMIN, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitud.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.estadoPedido").value("DESPACHADO"))
                .andExpect(jsonPath("$.reposicionStockPendiente").value(nullValue()));
    }

    @Test
    void ordersCaidoAlAprobarResponde503YSePuedeReintentar() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "CONFIRMADO");
        long id = crearCancelacion(user, pedido);
        doThrow(new ServicioNoDisponibleException("caído")).when(orders).cancelar(eq(pedido), eq(id), anyString());
        resolver("cancellations", id, ADMIN, true).andExpect(status().isServiceUnavailable());
        consultar("/api/v1/post-sale/requests/" + id, TestTokens.cliente(user))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        doReturn(new CancelacionOrders(pedido, "PED", "CANCELADO", true, false))
                .when(orders).cancelar(eq(pedido), eq(id), anyString());
        resolver("cancellations", id, ADMIN, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitud.estado").value("APROBADA"));
    }

    @Test
    void siOrdersYaCanceloElPedidoLaSolicitudNoPuedeRechazarse() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "CONFIRMADO");
        long id = crearCancelacion(user, pedido);
        cambiarEstado(pedido, "CANCELADO");     // p. ej. Orders canceló pero PostSale no alcanzó a guardar
        resolver("cancellations", id, ADMIN, false).andExpect(status().isConflict());
        resolver("cancellations", id, ADMIN, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitud.estado").value("APROBADA"));
    }

    @Test
    void dosAprobacionesSimultaneasQuedanConUnSoloResponsable() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "CONFIRMADO");
        long id = crearCancelacion(user, pedido);
        doAnswer(inv -> {
            Thread.sleep(300);
            return new CancelacionOrders(pedido, "PED", "CANCELADO", false, false);
        }).when(orders).cancelar(eq(pedido), eq(id), anyString());
        ExecutorService ex = Executors.newFixedThreadPool(2);
        try {
            List<Callable<String>> tareas = List.of(
                    () -> resolver("cancellations", id, ADMIN, true).andReturn().getResponse().getContentAsString(),
                    () -> resolver("cancellations", id, ADMIN_2, true).andReturn().getResponse().getContentAsString());
            List<Long> responsables = new ArrayList<>();
            for (Future<String> f : ex.invokeAll(tareas)) {
                Number n = JsonPath.read(f.get(), "$.solicitud.usuarioResolucionId");
                responsables.add(n.longValue());
            }
            assertEquals(responsables.get(0), responsables.get(1));
        } finally {
            ex.shutdown();
        }
        verify(notificaciones, times(1)).enviar(eq("cancelacion-" + id + "-resuelta"), anyLong(), anyString(),
                anyString(), anyString(), anyLong());
    }

    @Test
    void unaDevolucionNoSeResuelveComoCancelacion() throws Exception {
        long user = nuevoCliente();
        long dev = crearDevolucion(user, nuevoPedido(user, "ENTREGADO"), "[{\"productoId\":2,\"cantidadSolicitada\":1}]");
        resolver("cancellations", dev, ADMIN, true).andExpect(status().isNotFound());
        resolver("cancellations", 987654321, ADMIN, true).andExpect(status().isNotFound());
    }

    // ===================== Devolución (HU-69, HU-75) =====================

    @Test
    void solicitarDevolucionDeProductosComprados() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "ENTREGADO");
        enviar("/api/v1/post-sale/returns", TestTokens.cliente(user),
                "{\"pedidoId\":" + pedido + ",\"motivoSolicitud\":\"Llegó rayado\",\"items\":[{\"productoId\":2,\"cantidadSolicitada\":2}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("DEVOLUCION"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productoId").value(2))
                .andExpect(jsonPath("$.items[0].cantidadSolicitada").value(2))
                .andExpect(jsonPath("$.items[0].cantidadRecibida").value(0));
    }

    @Test
    void devolucionSoloDePedidosEntregados() throws Exception {
        long user = nuevoCliente();
        enviar("/api/v1/post-sale/returns", TestTokens.cliente(user),
                "{\"pedidoId\":" + nuevoPedido(user, "DESPACHADO") + ",\"motivoSolicitud\":\"x\",\"items\":[{\"productoId\":2,\"cantidadSolicitada\":1}]}")
                .andExpect(status().isConflict());
    }

    @Test
    void devolucionValidaProductosYCantidades() throws Exception {
        long user = nuevoCliente();
        String token = TestTokens.cliente(user);
        long pedido = nuevoPedido(user, "ENTREGADO");
        String base = "{\"pedidoId\":" + pedido + ",\"motivoSolicitud\":\"x\",\"items\":";
        // producto no comprado
        enviar("/api/v1/post-sale/returns", token, base + "[{\"productoId\":99,\"cantidadSolicitada\":1}]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores['items[0].productoId']").exists());
        // más de lo comprado
        enviar("/api/v1/post-sale/returns", token, base + "[{\"productoId\":5,\"cantidadSolicitada\":2}]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores['items[0].cantidadSolicitada']").exists());
        // decimal, texto, cero, negativo, vacío, sin productos y producto repetido
        for (String items : List.of("[{\"productoId\":2,\"cantidadSolicitada\":1.5}]",
                "[{\"productoId\":2,\"cantidadSolicitada\":\"dos\"}]",
                "[{\"productoId\":2,\"cantidadSolicitada\":0}]",
                "[{\"productoId\":2,\"cantidadSolicitada\":-1}]",
                "[{\"productoId\":2}]",
                "[]",
                "[{\"productoId\":2,\"cantidadSolicitada\":1},{\"productoId\":2,\"cantidadSolicitada\":1}]")) {
            enviar("/api/v1/post-sale/returns", token, base + items + "}").andExpect(status().isBadRequest());
        }
        consultar("/api/v1/post-sale/requests/me", token).andExpect(jsonPath("$.contenido", hasSize(0)));
    }

    @Test
    void noSeDuplicaUnaDevolucionPendienteDelMismoProducto() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "ENTREGADO");
        crearDevolucion(user, pedido, "[{\"productoId\":2,\"cantidadSolicitada\":1}]");
        enviar("/api/v1/post-sale/returns", TestTokens.cliente(user),
                "{\"pedidoId\":" + pedido + ",\"motivoSolicitud\":\"x\",\"items\":[{\"productoId\":2,\"cantidadSolicitada\":1}]}")
                .andExpect(status().isConflict());
        // Otro producto del mismo pedido sí se puede pedir
        crearDevolucion(user, pedido, "[{\"productoId\":5,\"cantidadSolicitada\":1}]");
    }

    @Test
    void loAprobadoMasLoNuevoNoSuperaLoComprado() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "ENTREGADO");
        long primera = crearDevolucion(user, pedido, "[{\"productoId\":2,\"cantidadSolicitada\":2}]");
        resolver("returns", primera, ADMIN, true).andExpect(status().isOk());
        enviar("/api/v1/post-sale/returns", TestTokens.cliente(user),
                "{\"pedidoId\":" + pedido + ",\"motivoSolicitud\":\"x\",\"items\":[{\"productoId\":2,\"cantidadSolicitada\":1}]}")
                .andExpect(status().isConflict());
    }

    @Test
    void aprobarYRechazarDevolucionGuardanResponsableYAvisan() throws Exception {
        long user = nuevoCliente();
        long a = crearDevolucion(user, nuevoPedido(user, "ENTREGADO"), "[{\"productoId\":2,\"cantidadSolicitada\":1}]");
        long b = crearDevolucion(user, nuevoPedido(user, "ENTREGADO"), "[{\"productoId\":2,\"cantidadSolicitada\":1}]");
        resolver("returns", a, ADMIN, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA")).andExpect(jsonPath("$.usuarioResolucionId").value(1));
        resolver("returns", b, ADMIN_2, false).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA")).andExpect(jsonPath("$.usuarioResolucionId").value(2));
        resolver("returns", b, ADMIN, true).andExpect(status().isConflict());
        resolver("returns", a, ADMIN_2, true).andExpect(status().isOk()).andExpect(jsonPath("$.usuarioResolucionId").value(1));
        verify(notificaciones).enviar(eq("devolucion-" + a + "-resuelta"), eq(user), eq("DEVOLUCION_RESUELTA"),
                anyString(), anyString(), eq(a));
    }

    @Test
    void alAprobarSeRevalidaElPedidoEnOrders() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "ENTREGADO");
        long id = crearDevolucion(user, pedido, "[{\"productoId\":2,\"cantidadSolicitada\":1}]");
        PEDIDOS.put(pedido, new PedidoOrders(pedido, "PED", user, "ENTREGADO", LocalDateTime.now(), BigDecimal.TEN,
                List.of(new ItemPedido(5L, "Disco Jazz", 1))));          // el producto 2 ya no figura en el pedido
        resolver("returns", id, ADMIN, true).andExpect(status().isConflict());
        doThrow(new ServicioNoDisponibleException("caído")).when(orders).obtener(pedido);
        resolver("returns", id, ADMIN, true).andExpect(status().isServiceUnavailable());
        consultar("/api/v1/post-sale/requests/" + id, TestTokens.cliente(user)).andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    // ===================== Recepción y reposición (HU-76) =====================

    @Test
    void recepcionSoloDeDevolucionesAprobadas() throws Exception {
        long user = nuevoCliente();
        long id = crearDevolucion(user, nuevoPedido(user, "ENTREGADO"), "[{\"productoId\":2,\"cantidadSolicitada\":1}]");
        long item = itemsDe(id).get(0);
        enviar("/api/v1/admin/post-sale/returns/" + id + "/reception", ADMIN,
                "{\"items\":[{\"itemDevolucionId\":" + item + ",\"cantidadRecibida\":1,\"condicion\":\"BUENA\"}]}")
                .andExpect(status().isConflict());
    }

    @Test
    void recepcionReponeSoloLoAptoUnaVezYGuardaAlResponsable() throws Exception {
        long user = nuevoCliente();
        long id = devolucionAprobada(user, nuevoPedido(user, "ENTREGADO"));
        List<Long> items = itemsDe(id);
        ResultActions r = enviar("/api/v1/admin/post-sale/returns/" + id + "/reception", ADMIN_2,
                "{\"observaciones\":\"Caja abierta\",\"items\":[{\"itemDevolucionId\":" + items.get(0)
                        + ",\"cantidadRecibida\":2,\"condicion\":\"BUENA\"},{\"itemDevolucionId\":" + items.get(1)
                        + ",\"cantidadRecibida\":1,\"condicion\":\"DANADA\"}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recepciones", hasSize(1)))
                .andExpect(jsonPath("$.recepciones[0].usuarioRecepcionId").value(2))
                .andExpect(jsonPath("$.recepciones[0].observaciones").value("Caja abierta"))
                .andExpect(jsonPath("$.recepciones[0].fechaRecepcion").exists())
                .andExpect(jsonPath("$.recepciones[0].items[0].estadoReposicion").value("CONFIRMADA"))
                .andExpect(jsonPath("$.recepciones[0].items[1].operacionId").value(nullValue()))
                .andExpect(jsonPath("$.items[0].cantidadRecibida").value(2));
        String operacion = JsonPath.read(r.andReturn().getResponse().getContentAsString(),
                "$.recepciones[0].items[0].operacionId");
        assertTrue(operacion.startsWith("POSTSALE-RECEPCION-"));
        // Solo la unidad BUENA se repone (RN-25), con el prefijo de DOC-08 y una sola vez
        verify(inventory, times(1)).reponer(eq(operacion), anyString(), eq(2L), eq(2));
        verify(inventory, times(1)).reponer(anyString(), anyString(), anyLong(), anyInt());
        // El Cliente ve la recepción, pero no al responsable ni la operación interna
        consultar("/api/v1/post-sale/requests/" + id, TestTokens.cliente(user))
                .andExpect(jsonPath("$.recepciones[0].usuarioRecepcionId").value(nullValue()))
                .andExpect(jsonPath("$.recepciones[0].items[0].operacionId").value(nullValue()));
    }

    @Test
    void recepcionNoSuperaLoAutorizadoNiRepiteProductos() throws Exception {
        long user = nuevoCliente();
        long id = devolucionAprobada(user, nuevoPedido(user, "ENTREGADO"));
        long item = itemsDe(id).get(0);       // producto 2, 2 unidades autorizadas
        String url = "/api/v1/admin/post-sale/returns/" + id + "/reception";
        enviar(url, ADMIN, "{\"items\":[{\"itemDevolucionId\":" + item + ",\"cantidadRecibida\":3,\"condicion\":\"BUENA\"}]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores['items[0].cantidadRecibida']").exists());
        enviar(url, ADMIN, "{\"items\":[{\"itemDevolucionId\":" + item + ",\"cantidadRecibida\":1,\"condicion\":\"BUENA\"},"
                + "{\"itemDevolucionId\":" + item + ",\"cantidadRecibida\":1,\"condicion\":\"BUENA\"}]}")
                .andExpect(status().isBadRequest());
        enviar(url, ADMIN, "{\"items\":[{\"itemDevolucionId\":987654,\"cantidadRecibida\":1,\"condicion\":\"BUENA\"}]}")
                .andExpect(status().isBadRequest());
        enviar(url, ADMIN, "{\"items\":[{\"itemDevolucionId\":" + item + ",\"cantidadRecibida\":1.5,\"condicion\":\"BUENA\"}]}")
                .andExpect(status().isBadRequest());
        enviar(url, ADMIN, "{\"items\":[{\"itemDevolucionId\":" + item + ",\"cantidadRecibida\":1}]}")
                .andExpect(status().isBadRequest());
        // Recepciones parciales: 1 + 1 llega al total; una tercera ya no
        enviar(url, ADMIN, "{\"items\":[{\"itemDevolucionId\":" + item + ",\"cantidadRecibida\":1,\"condicion\":\"BUENA\"}]}")
                .andExpect(status().isCreated());
        enviar(url, ADMIN, "{\"items\":[{\"itemDevolucionId\":" + item + ",\"cantidadRecibida\":1,\"condicion\":\"DANADA\"}]}")
                .andExpect(status().isCreated());
        enviar(url, ADMIN, "{\"items\":[{\"itemDevolucionId\":" + item + ",\"cantidadRecibida\":1,\"condicion\":\"BUENA\"}]}")
                .andExpect(status().isBadRequest());
        verify(inventory, times(1)).reponer(anyString(), anyString(), anyLong(), anyInt());
    }

    @Test
    void catalogCaidoDejaLaReposicionPendienteYElReintentoUsaLaMismaOperacion() throws Exception {
        long user = nuevoCliente();
        long id = devolucionAprobada(user, nuevoPedido(user, "ENTREGADO"));
        long item = itemsDe(id).get(0);
        when(inventory.reponer(anyString(), anyString(), anyLong(), anyInt()))
                .thenReturn(new InventoryClient.Respuesta(InventoryClient.Resultado.NO_CONFIRMADA, 0, null));
        String json = enviar("/api/v1/admin/post-sale/returns/" + id + "/reception", ADMIN,
                "{\"items\":[{\"itemDevolucionId\":" + item + ",\"cantidadRecibida\":2,\"condicion\":\"BUENA\"}]}")
                .andExpect(status().isCreated())   // la recepción queda registrada aunque Catalog no responda
                .andExpect(jsonPath("$.recepciones[0].items[0].estadoReposicion").value("PENDIENTE"))
                .andReturn().getResponse().getContentAsString();
        String operacion = JsonPath.read(json, "$.recepciones[0].items[0].operacionId");

        when(inventory.reponer(anyString(), anyString(), anyLong(), anyInt()))
                .thenReturn(new InventoryClient.Respuesta(InventoryClient.Resultado.APLICADA, 200, null));
        assertTrue(devolucionService.reintentarReposicionesPendientes() >= 1);
        verify(inventory, times(2)).reponer(eq(operacion), anyString(), eq(2L), eq(2));
        consultar("/api/v1/admin/post-sale/requests?id=" + id, ADMIN)
                .andExpect(jsonPath("$.contenido[0].recepciones[0].items[0].estadoReposicion").value("CONFIRMADA"));
        // Ya confirmada: otro reintento no la vuelve a enviar
        devolucionService.reintentarReposicionesPendientes();
        verify(inventory, times(2)).reponer(eq(operacion), anyString(), anyLong(), anyInt());
    }

    @Test
    void catalogRechazaLaReposicionQuedaFallidaYNoSeReintenta() throws Exception {
        long user = nuevoCliente();
        long id = devolucionAprobada(user, nuevoPedido(user, "ENTREGADO"));
        long item = itemsDe(id).get(1);
        when(inventory.reponer(anyString(), anyString(), anyLong(), anyInt()))
                .thenReturn(new InventoryClient.Respuesta(InventoryClient.Resultado.RECHAZADA, 404, "Producto no existe"));
        String json = enviar("/api/v1/admin/post-sale/returns/" + id + "/reception", ADMIN,
                "{\"items\":[{\"itemDevolucionId\":" + item + ",\"cantidadRecibida\":1,\"condicion\":\"BUENA\"}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recepciones[0].items[0].estadoReposicion").value("FALLIDA"))
                .andReturn().getResponse().getContentAsString();
        String operacion = JsonPath.read(json, "$.recepciones[0].items[0].operacionId");
        devolucionService.reintentarReposicionesPendientes();
        verify(inventory, times(1)).reponer(eq(operacion), anyString(), anyLong(), anyInt());
    }

    // ===================== Reembolso (HU-77) =====================

    @Test
    void reembolsoDeUnaCancelacionAprobadaQuedaRegistradoUnaSolaVez() throws Exception {
        long user = nuevoCliente();
        long cancelacion = crearCancelacion(user, nuevoPedido(user, "CONFIRMADO"));
        String token = TestTokens.cliente(user);
        String cuerpo = "{\"solicitudOrigenId\":" + cancelacion + "}";
        enviar("/api/v1/post-sale/refund-requests", token, cuerpo).andExpect(status().isConflict()); // aún PENDIENTE
        resolver("cancellations", cancelacion, ADMIN, true).andExpect(status().isOk());
        long id = id(enviar("/api/v1/post-sale/refund-requests", token, cuerpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("REEMBOLSO"))
                .andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.solicitudOrigenId").value(cancelacion)), "$.id");
        enviar("/api/v1/post-sale/refund-requests", token, cuerpo).andExpect(status().isConflict());
        verify(notificaciones).enviar(eq("reembolso-" + id + "-registrado"), eq(user), eq("REEMBOLSO_REGISTRADO"),
                anyString(), anyString(), eq(id));
        // Un reembolso no se aprueba ni se rechaza (RN-41) y no puede ser origen de otro
        resolver("cancellations", id, ADMIN, true).andExpect(status().isNotFound());
        enviar("/api/v1/post-sale/refund-requests", token, "{\"solicitudOrigenId\":" + id + "}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void reembolsoDeUnaSolicitudAjenaRechazadaOInexistente() throws Exception {
        long dueno = nuevoCliente();
        long otro = nuevoCliente();
        long dev = devolucionAprobada(dueno, nuevoPedido(dueno, "ENTREGADO"));
        enviar("/api/v1/post-sale/refund-requests", TestTokens.cliente(otro), "{\"solicitudOrigenId\":" + dev + "}")
                .andExpect(status().isNotFound());
        enviar("/api/v1/post-sale/refund-requests", TestTokens.cliente(otro), "{\"solicitudOrigenId\":987654321}")
                .andExpect(status().isNotFound());
        long rechazada = crearDevolucion(dueno, nuevoPedido(dueno, "ENTREGADO"), "[{\"productoId\":5,\"cantidadSolicitada\":1}]");
        resolver("returns", rechazada, ADMIN, false).andExpect(status().isOk());
        enviar("/api/v1/post-sale/refund-requests", TestTokens.cliente(dueno), "{\"solicitudOrigenId\":" + rechazada + "}")
                .andExpect(status().isConflict());
        enviar("/api/v1/post-sale/refund-requests", TestTokens.cliente(dueno), "{\"solicitudOrigenId\":" + dev + "}")
                .andExpect(status().isCreated());
    }

    @Test
    void dosReembolsosSimultaneosCreanUnoSolo() throws Exception {
        long user = nuevoCliente();
        long cancelacion = crearCancelacion(user, nuevoPedido(user, "CONFIRMADO"));
        resolver("cancellations", cancelacion, ADMIN, true).andExpect(status().isOk());
        String token = TestTokens.cliente(user);
        ExecutorService ex = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> tarea = () -> enviar("/api/v1/post-sale/refund-requests", token,
                    "{\"solicitudOrigenId\":" + cancelacion + "}").andReturn().getResponse().getStatus();
            List<Integer> codigos = new ArrayList<>();
            for (Future<Integer> f : ex.invokeAll(List.of(tarea, tarea))) {
                codigos.add(f.get());
            }
            assertEquals(1, codigos.stream().filter(c -> c == 201).count(), "códigos: " + codigos);
            assertEquals(1, codigos.stream().filter(c -> c == 409).count(), "códigos: " + codigos);
        } finally {
            ex.shutdown();
        }
    }

    // ===================== Consultas (HU-70 a HU-73) =====================

    @Test
    void elClienteSoloVeSusSolicitudes() throws Exception {
        long user = nuevoCliente();
        long otro = nuevoCliente();
        long mia = crearCancelacion(user, nuevoPedido(user, "CONFIRMADO"));
        long ajena = crearCancelacion(otro, nuevoPedido(otro, "CONFIRMADO"));
        consultar("/api/v1/post-sale/requests/me", TestTokens.cliente(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].id").value(mia))
                .andExpect(jsonPath("$.totalElementos").value(1));
        consultar("/api/v1/post-sale/requests/" + ajena, TestTokens.cliente(user)).andExpect(status().isNotFound());
        consultar("/api/v1/post-sale/requests/me", TestTokens.cliente(nuevoCliente()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido", hasSize(0)));
    }

    @Test
    void elAdministradorBuscaYFiltra() throws Exception {
        long user = nuevoCliente();
        long pedido = nuevoPedido(user, "CONFIRMADO");
        long cancelacion = crearCancelacion(user, pedido);
        resolver("cancellations", cancelacion, ADMIN, true).andExpect(status().isOk());
        long reembolso = id(enviar("/api/v1/post-sale/refund-requests", TestTokens.cliente(user),
                "{\"solicitudOrigenId\":" + cancelacion + "}").andExpect(status().isCreated()), "$.id");
        consultar("/api/v1/admin/post-sale/requests?usuarioId=" + user, ADMIN)
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.contenido[0].id").value(reembolso));      // más reciente primero
        consultar("/api/v1/admin/post-sale/requests?usuarioId=" + user + "&tipo=REEMBOLSO", ADMIN)
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].solicitudOrigenId").value(cancelacion));
        consultar("/api/v1/admin/post-sale/requests?pedidoId=" + pedido + "&estado=APROBADA", ADMIN)
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].usuarioResolucionId").value(1));
        consultar("/api/v1/admin/post-sale/requests?id=" + cancelacion, ADMIN).andExpect(jsonPath("$.contenido", hasSize(1)));
        String hoy = java.time.LocalDate.now().toString();
        consultar("/api/v1/admin/post-sale/requests?usuarioId=" + user + "&desde=" + hoy + "&hasta=" + hoy, ADMIN)
                .andExpect(jsonPath("$.contenido", hasSize(2)));
        consultar("/api/v1/admin/post-sale/requests?usuarioId=" + user + "&desde=2000-01-01&hasta=2000-01-02", ADMIN)
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido", hasSize(0)));
        consultar("/api/v1/admin/post-sale/requests?desde=2026-02-01&hasta=2026-01-01", ADMIN).andExpect(status().isBadRequest());
        consultar("/api/v1/admin/post-sale/requests?size=101", ADMIN).andExpect(status().isBadRequest());
        consultar("/api/v1/admin/post-sale/requests?tipo=OTRO", ADMIN).andExpect(status().isBadRequest());
    }

    @Test
    void lasFilasAntiguasSinMigrarNoSeMuestranNiSeGestionan() throws Exception {
        long user = nuevoCliente();
        // Simula una fila de la herencia anterior: solo las columnas de la tabla base, sin tipo ni estado
        jdbc.update("insert into solicitudes_postventa (pedido_id, usuario_id, fecha_solicitud) values (?, ?, ?)",
                424242L, user, LocalDateTime.now());
        Long antigua = jdbc.queryForObject("select max(id) from solicitudes_postventa where usuario_id = ?", Long.class, user);
        consultar("/api/v1/post-sale/requests/me", TestTokens.cliente(user)).andExpect(jsonPath("$.contenido", hasSize(0)));
        consultar("/api/v1/post-sale/requests/" + antigua, TestTokens.cliente(user)).andExpect(status().isNotFound());
        resolver("cancellations", antigua, ADMIN, true).andExpect(status().isNotFound());
        enviar("/api/v1/post-sale/refund-requests", TestTokens.cliente(user), "{\"solicitudOrigenId\":" + antigua + "}")
                .andExpect(status().isNotFound());
    }

    @Test
    void siNotificationsFallaLaSolicitudQuedaGuardada() throws Exception {
        long user = nuevoCliente();
        doThrow(new RuntimeException("Notifications caído")).when(notificaciones)
                .enviar(anyString(), anyLong(), anyString(), anyString(), anyString(), anyLong());
        long id = crearCancelacion(user, nuevoPedido(user, "CONFIRMADO"));
        consultar("/api/v1/post-sale/requests/" + id, TestTokens.cliente(user)).andExpect(status().isOk());
    }

    // ===================== Lote 5 · Integraciones de HU-78 =====================

    /** HU-78 (ERS 13.8): Accounts pregunta si hay solicitudes PENDIENTE. Resueltas o REGISTRADA no bloquean. */
    @Test
    void internoSolicitudesPendientesParaAccounts() throws Exception {
        long user = nuevoCliente();
        consultar("/api/v1/internal/post-sale/users/" + user + "/pending-exists", TokensServicioPrueba.bearer(Servicios.ACCOUNTS))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(user)).andExpect(jsonPath("$.existe").value(false));
        long id = crearCancelacion(user, nuevoPedido(user, "CONFIRMADO"));
        consultar("/api/v1/internal/post-sale/users/" + user + "/pending-exists", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)).andExpect(jsonPath("$.existe").value(true));
        resolver("cancellations", id, ADMIN, true).andExpect(status().isOk());
        enviar("/api/v1/post-sale/refund-requests", TestTokens.cliente(user), "{\"solicitudOrigenId\":" + id + "}")
                .andExpect(status().isCreated());
        consultar("/api/v1/internal/post-sale/users/" + user + "/pending-exists", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)).andExpect(jsonPath("$.existe").value(false));
    }

    /** Una fila antigua sin migrar no se puede saber si está pendiente: por precaución cuenta como pendiente. */
    @Test
    void internoFilasSinMigrarSeTratanComoPendientes() throws Exception {
        long user = nuevoCliente();
        jdbc.update("insert into solicitudes_postventa (pedido_id, usuario_id, fecha_solicitud) values (?, ?, ?)",
                434343L, user, LocalDateTime.now());
        consultar("/api/v1/internal/post-sale/users/" + user + "/pending-exists", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)).andExpect(jsonPath("$.existe").value(true));
    }

    /** RN-37 / RN-44 / T-06: cuenta que no puede operar -> 403 y no se crea ninguna solicitud. */
    @Test
    void cuentaQueNoPuedeOperarNoCreaSolicitudes() throws Exception {
        long user = nuevoCliente();
        long pedidoConfirmado = nuevoPedido(user, "CONFIRMADO");
        long aprobada = crearCancelacion(user, pedidoConfirmado);
        resolver("cancellations", aprobada, ADMIN, true).andExpect(status().isOk());
        long entregado = nuevoPedido(user, "ENTREGADO");
        doThrow(new AccesoDenegadoException("Tu cuenta no puede realizar nuevas operaciones")).when(cuentas)
                .exigirCuentaActiva(user);
        String token = TestTokens.cliente(user);
        enviar("/api/v1/post-sale/cancellations", token,
                "{\"pedidoId\":" + nuevoPedido(user, "CONFIRMADO") + ",\"motivoSolicitud\":\"x\"}").andExpect(status().isForbidden());
        enviar("/api/v1/post-sale/returns", token, "{\"pedidoId\":" + entregado
                + ",\"motivoSolicitud\":\"x\",\"items\":[{\"productoId\":2,\"cantidadSolicitada\":1}]}").andExpect(status().isForbidden());
        enviar("/api/v1/post-sale/refund-requests", token, "{\"solicitudOrigenId\":" + aprobada + "}")
                .andExpect(status().isForbidden());
        // Cierre (RN-44): con la cuenta no ACTIVA tampoco se consulta
        consultar("/api/v1/post-sale/requests/me", token).andExpect(status().isForbidden());
        doNothing().when(cuentas).exigirCuentaActiva(user);
        consultar("/api/v1/post-sale/requests/me", token).andExpect(jsonPath("$.totalElementos").value(1));   // solo la anterior
    }

    /** Accounts no responde -> 503 y no se crea la solicitud (nunca se asume ACTIVA). */
    @Test
    void accountsCaidoNoCreaSolicitudes() throws Exception {
        long user = nuevoCliente();
        doThrow(new ServicioNoDisponibleException("No fue posible confirmar el estado de tu cuenta")).when(cuentas)
                .exigirCuentaActiva(user);
        enviar("/api/v1/post-sale/cancellations", TestTokens.cliente(user),
                "{\"pedidoId\":" + nuevoPedido(user, "CONFIRMADO") + ",\"motivoSolicitud\":\"x\"}")
                .andExpect(status().isServiceUnavailable());
        consultar("/api/v1/post-sale/requests/me", TestTokens.cliente(user)).andExpect(status().isServiceUnavailable());
        doNothing().when(cuentas).exigirCuentaActiva(user);
        consultar("/api/v1/post-sale/requests/me", TestTokens.cliente(user)).andExpect(jsonPath("$.contenido", hasSize(0)));
        verify(notificaciones, never()).enviar(anyString(), eq(user), anyString(), anyString(), anyString(), anyLong());
    }
}
