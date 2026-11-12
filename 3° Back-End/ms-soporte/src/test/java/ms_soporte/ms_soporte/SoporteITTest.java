package ms_soporte.ms_soporte;

import ms_soporte.ms_soporte.security.Servicios;
import com.jayway.jsonpath.JsonPath;
import ms_soporte.ms_soporte.client.AccountsClient;
import ms_soporte.ms_soporte.client.OrdersClient;
import ms_soporte.ms_soporte.client.NotificacionesClient;
import ms_soporte.ms_soporte.exception.AccesoDenegadoException;
import ms_soporte.ms_soporte.exception.RecursoNoEncontradoException;
import ms_soporte.ms_soporte.exception.ServicioNoDisponibleException;
import ms_soporte.ms_soporte.dto.CrearTicketRequest;
import ms_soporte.ms_soporte.service.TicketService;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lote 1: flujo completo de Soporte con BD H2 real (Controller -> Service -> Repository -> JPA).
 * Prueba HU-59 a HU-67 y RN-30. Notifications se simula para no depender de otro MS.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SoporteITTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificacionesClient notificacionesClient;

    /** Lote 5 (HU-78): simulado. Por defecto no lanza nada = cuenta ACTIVA (las pruebas anteriores no cambian). */
    @MockitoBean
    private AccountsClient cuentas;

    /** Cierre (RF-SOP-01): simulado. Por defecto no lanza nada = pedido propio. */
    @MockitoBean
    private OrdersClient pedidos;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final String CUERPO_VALIDO =
            "{\"asunto\":\"No llega mi pedido\",\"categoria\":\"pedido\",\"mensaje\":\"Hola, mi pedido no llega\"}";

    private long crearTicket(String token) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/support/requests").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andReturn();
        String json = r.getResponse().getContentAsString();
        // Lote 1.1: se lee con JsonPath (Jackson 3 puede ordenar los campos; "id" no siempre va primero)
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void crear_SinToken_401() throws Exception {
        mockMvc.perform(post("/api/v1/support/requests")
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crear_MensajeVacio_400ConElCampo() throws Exception {
        mockMvc.perform(post("/api/v1/support/requests").header("Authorization", TestTokens.cliente(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asunto\":\"Hola\",\"categoria\":\"GENERAL\",\"mensaje\":\"  \"}"))
                .andExpect(status().isBadRequest())
                // Lote 1.1: formato ApiError; el campo con problema va dentro de "errores"
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.mensaje").value("El mensaje no puede estar vacío"));
    }

    @Test
    void crear_DuenoDesdeElToken_EstadoAbierta_PrimerMensajeEHistorial() throws Exception {
        mockMvc.perform(post("/api/v1/support/requests").header("Authorization", TestTokens.cliente(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        // aunque el cliente intente mandar otro dueño o estado, se ignora
                        .content("{\"asunto\":\"Consulta\",\"categoria\":\"general\",\"mensaje\":\"Necesito ayuda\","
                                + "\"idUsuario\":99,\"estado\":\"CERRADA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(10))
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.categoria").value("GENERAL"))
                .andExpect(jsonPath("$.numeroTicket").value(startsWith("SOP-")))
                .andExpect(jsonPath("$.mensajes[0].autor").value("CLIENTE"))
                .andExpect(jsonPath("$.mensajes[0].autorId").value(10))
                .andExpect(jsonPath("$.historial[0].estadoNuevo").value("ABIERTA"));
        verify(notificacionesClient).notificar(anyString(), eq(10L), eq("SOPORTE_CREADO"), anyString(), anyString(), anyLong());
    }

    @Test
    void cliente_SoloVeSusSolicitudes_LaAjenaEs404() throws Exception {
        long id = crearTicket(TestTokens.cliente(20));

        mockMvc.perform(get("/api/v1/support/requests/" + id).header("Authorization", TestTokens.cliente(20)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/support/requests/" + id).header("Authorization", TestTokens.cliente(21)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/support/requests/me").header("Authorization", TestTokens.cliente(21)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rutasAdmin_ConTokenDeCliente_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/support/requests").header("Authorization", TestTokens.cliente(30)))
                .andExpect(status().isForbidden());
    }

    @Test
    void flujoCompleto_Responder_EnProceso_Cerrar_YCerradaNoAceptaMas() throws Exception {
        long id = crearTicket(TestTokens.cliente(40));

        // HU-65: el administrador responde -> aviso SOPORTE_RESPONDIDO al dueño
        mockMvc.perform(post("/api/v1/admin/support/requests/" + id + "/messages")
                        .header("Authorization", TestTokens.admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contenido\":\"Lo revisamos\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensajes[1].autor").value("ADMINISTRADOR"));
        verify(notificacionesClient).notificar(anyString(), eq(40L), eq("SOPORTE_RESPONDIDO"), anyString(), anyString(), anyLong());

        // HU-66: CERRADA no se permite por cambio de estado intermedio
        mockMvc.perform(patch("/api/v1/admin/support/requests/" + id + "/status?estado=CERRADA")
                        .header("Authorization", TestTokens.admin()))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/v1/admin/support/requests/" + id + "/status?estado=EN_PROCESO")
                        .header("Authorization", TestTokens.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"))
                .andExpect(jsonPath("$.historial[1].estadoAnterior").value("ABIERTA"));

        // HU-67: cierre con fecha; repetirlo -> 409
        mockMvc.perform(post("/api/v1/admin/support/requests/" + id + "/close").header("Authorization", TestTokens.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.fechaCierre").exists());
        mockMvc.perform(post("/api/v1/admin/support/requests/" + id + "/close").header("Authorization", TestTokens.admin()))
                .andExpect(status().isConflict());

        // RN-30: cerrada no acepta respuestas, ni del cliente ni del administrador
        mockMvc.perform(post("/api/v1/support/requests/" + id + "/messages").header("Authorization", TestTokens.cliente(40))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contenido\":\"Sigo con dudas\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/admin/support/requests/" + id + "/messages").header("Authorization", TestTokens.admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contenido\":\"Otra\"}"))
                .andExpect(status().isConflict());

        // HU-61: la conversación y el historial quedaron guardados en la BD
        mockMvc.perform(get("/api/v1/support/requests/" + id).header("Authorization", TestTokens.cliente(40)))
                .andExpect(jsonPath("$.mensajes.length()").value(2))
                .andExpect(jsonPath("$.historial.length()").value(3));
    }

    // Lote 1.1 (observación A) con transacción REAL de Spring y BD H2:
    // la operación se confirma -> se intenta enviar el aviso, una sola vez
    @Test
    void transaccionConfirmada_SeIntentaEnviarElAviso() {
        long id = ticketService.crearTicket(new CrearTicketRequest(null, "Confirmada", "general", "Hola"), 70L).id();

        verify(notificacionesClient, times(1)).notificar(eq("SOPORTE-" + id + "-CREADO"), eq(70L),
                eq("SOPORTE_CREADO"), anyString(), anyString(), eq(id));
        assertEquals(1, ticketService.listarPropios(70L).size());
    }

    // la transacción se revierte -> no queda la solicitud y NO se envía aviso
    @Test
    void transaccionRevertida_NoGuardaNiAvisa() {
        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            ticketService.crearTicket(new CrearTicketRequest(null, "Revertida", "general", "Hola"), 71L);
            estado.setRollbackOnly(); // simula un error al final de la operación
        });

        verify(notificacionesClient, never()).notificar(any(), any(), any(), any(), any(), any());
        assertEquals(0, ticketService.listarPropios(71L).size());
    }

    // RN-33: cada acción tiene un eventoId propio y fijo; Notifications usa ese id para no duplicar
    @Test
    void eventoId_UnoPorAccion_CadaRespuestaConSuPropioId() throws Exception {
        long id = crearTicket(TestTokens.cliente(72));
        for (String texto : List.of("Primera respuesta", "Segunda respuesta")) {
            mockMvc.perform(post("/api/v1/admin/support/requests/" + id + "/messages")
                            .header("Authorization", TestTokens.admin())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"contenido\":\"" + texto + "\"}"))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/v1/admin/support/requests/" + id + "/close").header("Authorization", TestTokens.admin()))
                .andExpect(status().isOk());

        ArgumentCaptor<String> eventoIds = ArgumentCaptor.forClass(String.class);
        verify(notificacionesClient, times(4)).notificar(eventoIds.capture(), eq(72L), anyString(), anyString(),
                anyString(), eq(id));
        List<String> ids = eventoIds.getAllValues();
        assertEquals("SOPORTE-" + id + "-CREADO", ids.get(0));
        assertTrue(ids.get(1).matches("SOPORTE-" + id + "-MSG-\\d+"), ids.get(1));
        assertTrue(ids.get(2).matches("SOPORTE-" + id + "-MSG-\\d+"), ids.get(2));
        assertTrue(!ids.get(1).equals(ids.get(2)), "cada respuesta debe tener su propio eventoId");
        assertEquals("SOPORTE-" + id + "-CERRADO", ids.get(3));
    }

    // Lote 1.1 (DOC-01, RNF-09): el responsable de cada cambio es el usuario del TOKEN.
    // Aunque el frontend envíe un "usuarioResponsableId", se ignora.
    @Test
    void historial_ResponsableEsElUsuarioDelToken_NuncaUnIdEnviadoPorElFrontend() throws Exception {
        long id = crearTicket(TestTokens.cliente(90));

        mockMvc.perform(patch("/api/v1/admin/support/requests/" + id + "/status?estado=EN_PROCESO&usuarioResponsableId=999")
                        .header("Authorization", TestTokens.bearer(7, "ADMINISTRADOR")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/support/requests/" + id + "/close")
                        .header("Authorization", TestTokens.bearer(8, "ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioResponsableId\":999}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/support/requests/" + id).header("Authorization", TestTokens.cliente(90)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historial.length()").value(3))
                .andExpect(jsonPath("$.historial[0].usuarioResponsableId").value(90)) // el Cliente creó
                .andExpect(jsonPath("$.historial[1].usuarioResponsableId").value(7))  // admin 7: EN_PROCESO
                .andExpect(jsonPath("$.historial[2].usuarioResponsableId").value(8)); // admin 8: cierre
    }

    // Lote 1.1 (HU-64 "buscar por identificador"): q numérico encuentra la solicitud por su id
    @Test
    void listadoAdmin_BuscaPorIdNumerico() throws Exception {
        long id = crearTicket(TestTokens.cliente(60));

        mockMvc.perform(get("/api/v1/admin/support/requests?q=" + id).header("Authorization", TestTokens.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.id == " + id + ")]").exists());
    }

    // Lote 1.1: falta el parámetro obligatorio -> 400 con el formato {"error": ...} (antes, respuesta genérica)
    @Test
    void cambiarEstado_SinParametroEstado_400() throws Exception {
        long id = crearTicket(TestTokens.cliente(61));

        mockMvc.perform(patch("/api/v1/admin/support/requests/" + id + "/status").header("Authorization", TestTokens.admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("Falta el parámetro obligatorio 'estado'"))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/support/requests/" + id + "/status"));
    }

    @Test
    void listadoAdmin_FiltraPorEstadoYTexto_YValidaFechas() throws Exception {
        crearTicket(TestTokens.cliente(50));

        mockMvc.perform(get("/api/v1/admin/support/requests?estado=ABIERTA&q=pedido&categoria=PEDIDO&tamano=5")
                        .header("Authorization", TestTokens.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(5))
                .andExpect(jsonPath("$.contenido[0].estado").value("ABIERTA"));
        mockMvc.perform(get("/api/v1/admin/support/requests?desde=2026-05-02&hasta=2026-05-01")
                        .header("Authorization", TestTokens.admin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/admin/support/requests?estado=INVENTADO")
                        .header("Authorization", TestTokens.admin()))
                .andExpect(status().isBadRequest());
    }

    // ===================== Lote 5 · Integraciones de HU-78 =====================

    /** HU-78 (ERS 13.8): Accounts pregunta si hay tickets ABIERTA o EN_PROCESO. CERRADA ya no bloquea. */
    @Test
    void internoTicketsAbiertosParaAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/internal/support/users/990001/open-exists").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(990001))
                .andExpect(jsonPath("$.existe").value(false));
        long id = crearTicket(TestTokens.cliente(990002));
        mockMvc.perform(get("/api/v1/internal/support/users/990002/open-exists").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS))).andExpect(jsonPath("$.existe").value(true));
        mockMvc.perform(patch("/api/v1/admin/support/requests/" + id + "/status?estado=EN_PROCESO")
                        .header("Authorization", TestTokens.admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/internal/support/users/990002/open-exists").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS))).andExpect(jsonPath("$.existe").value(true));
        mockMvc.perform(post("/api/v1/admin/support/requests/" + id + "/close").header("Authorization", TestTokens.admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/internal/support/users/990002/open-exists").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS))).andExpect(jsonPath("$.existe").value(false));
    }

    /** RN-37 / RN-44 / T-06: cuenta que no puede operar -> 403 y no se crea el ticket. */
    @Test
    void crearTicket_ConCuentaQueNoPuedeOperar_403_SinTicket() throws Exception {
        doThrow(new AccesoDenegadoException("Tu cuenta no puede realizar nuevas operaciones")).when(cuentas)
                .exigirCuentaActiva(990003L);
        mockMvc.perform(post("/api/v1/support/requests").header("Authorization", TestTokens.cliente(990003))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        // Cierre (RN-44): con la cuenta no ACTIVA tampoco se consulta
        mockMvc.perform(get("/api/v1/support/requests/me").header("Authorization", TestTokens.cliente(990003)))
                .andExpect(status().isForbidden());
        doNothing().when(cuentas).exigirCuentaActiva(990003L);
        mockMvc.perform(get("/api/v1/support/requests/me").header("Authorization", TestTokens.cliente(990003)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        verify(notificacionesClient, never()).notificar(anyString(), eq(990003L), anyString(), anyString(), anyString(), anyLong());
    }

    /** Accounts no responde -> 503 y no se crea el ticket (nunca se asume ACTIVA). */
    @Test
    void crearTicket_ConAccountsCaido_503_SinTicket() throws Exception {
        doThrow(new ServicioNoDisponibleException("No fue posible confirmar el estado de tu cuenta")).when(cuentas)
                .exigirCuentaActiva(990004L);
        mockMvc.perform(post("/api/v1/support/requests").header("Authorization", TestTokens.cliente(990004))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/v1/support/requests/me").header("Authorization", TestTokens.cliente(990004)))
                .andExpect(status().isServiceUnavailable());
        doNothing().when(cuentas).exigirCuentaActiva(990004L);
        mockMvc.perform(get("/api/v1/support/requests/me").header("Authorization", TestTokens.cliente(990004)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ===================== Cierre · RF-SOP-01 / HU-59: pedido asociado =====================

    private static String cuerpoConPedido(long orderId) {
        return "{\"orderId\":" + orderId + ",\"asunto\":\"Mi pedido\",\"categoria\":\"pedido\",\"mensaje\":\"No llega\"}";
    }

    @Test
    void crearTicket_ConPedidoPropio_201YSeConsultaOrdersConElUsuarioDelToken() throws Exception {
        mockMvc.perform(post("/api/v1/support/requests").header("Authorization", TestTokens.cliente(990010))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoConPedido(55)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(55));
        verify(pedidos).exigirPedidoPropio(55L, 990010L);
    }

    @Test
    void crearTicket_ConPedidoAjenoOInexistente_404SinTicket() throws Exception {
        doThrow(new RecursoNoEncontradoException("El pedido indicado no existe o no corresponde a tu cuenta"))
                .when(pedidos).exigirPedidoPropio(56L, 990011L);
        mockMvc.perform(post("/api/v1/support/requests").header("Authorization", TestTokens.cliente(990011))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoConPedido(56)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(get("/api/v1/support/requests/me").header("Authorization", TestTokens.cliente(990011)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void crearTicket_ConOrdersCaido_503SinTicket() throws Exception {
        doThrow(new ServicioNoDisponibleException("No fue posible verificar el pedido asociado"))
                .when(pedidos).exigirPedidoPropio(57L, 990012L);
        mockMvc.perform(post("/api/v1/support/requests").header("Authorization", TestTokens.cliente(990012))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoConPedido(57)))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/v1/support/requests/me").header("Authorization", TestTokens.cliente(990012)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void crearTicket_SinPedido_NoConsultaOrders() throws Exception {
        crearTicket(TestTokens.cliente(990013));
        verify(pedidos, never()).exigirPedidoPropio(anyLong(), anyLong());
    }
}
