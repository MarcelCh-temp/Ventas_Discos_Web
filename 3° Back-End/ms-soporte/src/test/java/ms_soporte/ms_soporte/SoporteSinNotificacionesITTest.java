package ms_soporte.ms_soporte;

import ms_soporte.ms_soporte.client.AccountsClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lote 1.1 (observación A, D14, ERS 11.6 "una caída de Notifications no debe impedir guardar respuestas
 * o cerrar correctamente una solicitud").
 *
 * Aquí NotificacionesClient NO se simula: es el cliente real, apuntando a http://127.0.0.1:1
 * (src/test/resources/application.properties: 127.0.0.1:1), donde no hay nada escuchando. Es decir, Notifications está caído.
 * Se comprueba que crear, responder y cerrar funcionan y que todo queda guardado en la BD.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SoporteSinNotificacionesITTest {

    @Autowired
    private MockMvc mockMvc;

    /** Lote 5 (HU-78): solo Accounts se simula (cuenta ACTIVA). Notifications sigue siendo el cliente real caído. */
    @MockitoBean
    private AccountsClient cuentas;

    @Test
    void notificationsCaido_LaSolicitudSeGuardaRespondeYCierraIgual() throws Exception {
        MvcResult creado = mockMvc.perform(post("/api/v1/support/requests").header("Authorization", TestTokens.cliente(80))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asunto\":\"Sin avisos\",\"categoria\":\"general\",\"mensaje\":\"Hola\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String json = creado.getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();

        mockMvc.perform(post("/api/v1/admin/support/requests/" + id + "/messages").header("Authorization", TestTokens.admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contenido\":\"Respuesta\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/admin/support/requests/" + id + "/close").header("Authorization", TestTokens.admin()))
                .andExpect(status().isOk());

        // todo quedó confirmado en la BD aunque ningún aviso se entregó
        mockMvc.perform(get("/api/v1/support/requests/" + id).header("Authorization", TestTokens.cliente(80)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.mensajes.length()").value(2))
                .andExpect(jsonPath("$.historial.length()").value(2));
    }
}
