package com.ventasdiscos.msnotifications;

import com.ventasdiscos.msnotifications.client.AccountsClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ventasdiscos.msnotifications.security.Servicios;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lote 1.1: flujo completo con BD H2 real (Controller -> Service -> Repository -> JPA).
 * Comprueba, desde el lado del receptor, el contrato Soporte -> Notifications del Lote 1:
 * HU-53 (se genera el aviso), HU-54 (el cliente ve solo los suyos), RN-31 (marcar como leída no borra)
 * y RN-33 / prueba T-21 (un evento repetido no duplica el aviso).
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificacionesITTest {

    // Cierre (RN-44): las peticiones del cliente consultan a Accounts; aquí se simula ACTIVA
    @MockitoBean
    private AccountsClient cuentas;

    @Autowired
    private MockMvc mockMvc;

    /** Mismo JSON que envía ms-soporte (NotificacionesClient) al crear una solicitud. */
    private static String eventoSoporte(String eventoId, long usuarioId, String tipo) {
        return "{\"eventoId\":\"" + eventoId + "\",\"origen\":\"SUPPORT\",\"usuarioId\":" + usuarioId
                + ",\"titulo\":\"Solicitud de soporte creada\",\"mensaje\":\"Tu solicitud SOP-000012 fue registrada.\","
                + "\"tipo\":\"" + tipo + "\",\"referenciaId\":12}";
    }

    @Test
    void eventoRepetido_NoDuplicaElAviso_YElClienteLoVe() throws Exception {
        String evento = eventoSoporte("SOPORTE-12-CREADO", 10, "SOPORTE_CREADO");

        mockMvc.perform(post("/api/v1/internal/notifications/events").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                        .contentType(MediaType.APPLICATION_JSON).content(evento))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.processed").value(true));
        // reintento del emisor con el mismo eventoId: no se crea otra notificación
        mockMvc.perform(post("/api/v1/internal/notifications/events").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                        .contentType(MediaType.APPLICATION_JSON).content(evento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(false));

        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", TestTokens.cliente(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("SOPORTE_CREADO"))
                .andExpect(jsonPath("$[0].leida").value(false));
        // HU-54: otro cliente no ve ese aviso
        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", TestTokens.cliente(11)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void marcarTodasComoLeidas_NoBorraLasNotificaciones() throws Exception {
        mockMvc.perform(post("/api/v1/internal/notifications/events").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventoSoporte("SOPORTE-20-CERRADO", 20, "SOPORTE_CERRADO")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/notifications/me/unread-count").header("Authorization", TestTokens.cliente(20)))
                .andExpect(jsonPath("$.unreadCount").value(1));
        mockMvc.perform(patch("/api/v1/notifications/read-all").header("Authorization", TestTokens.cliente(20)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(1));
        mockMvc.perform(get("/api/v1/notifications/me/unread-count").header("Authorization", TestTokens.cliente(20)))
                .andExpect(jsonPath("$.unreadCount").value(0));
        // RN-31: sigue ahí, ahora como leída
        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", TestTokens.cliente(20)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].leida").value(true));
    }

    // HU-54: marcar como leída una notificación ajena responde 404 (no revela que existe) y no la cambia
    @Test
    void marcarLeida_SoloLaPropia() throws Exception {
        mockMvc.perform(post("/api/v1/internal/notifications/events").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventoSoporte("SOPORTE-40-MSG-1", 40, "SOPORTE_RESPONDIDO")))
                .andExpect(status().isCreated());
        String json = mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", TestTokens.cliente(40)))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$[0].id")).longValue();

        mockMvc.perform(patch("/api/v1/notifications/" + id + "/read").header("Authorization", TestTokens.cliente(41)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/notifications/me/unread-count").header("Authorization", TestTokens.cliente(40)))
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(patch("/api/v1/notifications/" + id + "/read").header("Authorization", TestTokens.cliente(40)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/notifications/me/unread-count").header("Authorization", TestTokens.cliente(40)))
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void sinToken_401_YRutaInexistente_404() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/me/unread-count"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401)) // Lote 1.1: formato ApiError
                .andExpect(jsonPath("$.mensaje").exists());
        // Lote 1.1: antes esto respondía 500 (manejador general)
        mockMvc.perform(get("/api/v1/notifications/ruta-que-no-existe/otra").header("Authorization", TestTokens.cliente(42)))
                .andExpect(status().isNotFound());
    }

    // HU-55 / RF-NOT-03: filtrar las notificaciones propias por tipo y período; período inválido -> 400
    @Test
    void filtrarPorTipoYPeriodo_SoloPropias() throws Exception {
        for (String[] e : new String[][]{{"SOPORTE-50-CREADO", "SOPORTE_CREADO"}, {"SOPORTE-50-CERRADO", "SOPORTE_CERRADO"}}) {
            mockMvc.perform(post("/api/v1/internal/notifications/events").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                            .contentType(MediaType.APPLICATION_JSON).content(eventoSoporte(e[0], 50, e[1])))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/notifications/me?type=SOPORTE_CERRADO").header("Authorization", TestTokens.cliente(50)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("SOPORTE_CERRADO"));
        // período que no contiene nada -> lista vacía (el frontend informa "sin resultados")
        mockMvc.perform(get("/api/v1/notifications/me?from=2000-01-01T00:00:00&to=2000-01-02T00:00:00")
                        .header("Authorization", TestTokens.cliente(50)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        // fecha inicial mayor que la final -> 400 con el dato a corregir (RNF-04)
        mockMvc.perform(get("/api/v1/notifications/me?from=2030-01-02T00:00:00&to=2030-01-01T00:00:00")
                        .header("Authorization", TestTokens.cliente(50)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400)) // Lote 1.1: formato ApiError
                .andExpect(jsonPath("$.mensaje").value("La fecha inicial no puede superar la fecha final"));
    }

    @Test
    void eventoConTipoInexistente_400() throws Exception {
        mockMvc.perform(post("/api/v1/internal/notifications/events").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventoSoporte("SOPORTE-30-X", 30, "SOPORTE_INVENTADO")))
                .andExpect(status().isBadRequest());
    }

    // ===================== Lote 5 · Integración de HU-78 =====================

    /** HU-78 / RN-43 (ERS 13.8): depuración interna idempotente; solo borra las notificaciones de ese usuario. */
    @Test
    void depuracionDeNotificacionesPersonales_EsIdempotente() throws Exception {
        for (String id : new String[]{"SOPORTE-501-CREADO", "SOPORTE-502-CREADO"}) {
            mockMvc.perform(post("/api/v1/internal/notifications/events").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                            .contentType(MediaType.APPLICATION_JSON).content(eventoSoporte(id, 501, "SOPORTE_CREADO")))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/v1/internal/notifications/events").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                        .contentType(MediaType.APPLICATION_JSON).content(eventoSoporte("SOPORTE-503-CREADO", 502, "SOPORTE_CREADO")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/internal/notifications/users/501/purge").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(501))
                .andExpect(jsonPath("$.eliminados").value(2));
        // Repetir no duplica efectos
        mockMvc.perform(post("/api/v1/internal/notifications/users/501/purge").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eliminados").value(0));

        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", TestTokens.cliente(501)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", TestTokens.cliente(502)))
                .andExpect(jsonPath("$.length()").value(1));
        // El registro de eventos procesados se conserva: un reintento del mismo evento no vuelve a crear el aviso
        mockMvc.perform(post("/api/v1/internal/notifications/events").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS))
                        .contentType(MediaType.APPLICATION_JSON).content(eventoSoporte("SOPORTE-501-CREADO", 501, "SOPORTE_CREADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(false));
    }
}
