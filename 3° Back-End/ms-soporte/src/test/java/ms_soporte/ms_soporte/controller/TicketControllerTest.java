package ms_soporte.ms_soporte.controller;

import ms_soporte.ms_soporte.client.AccountsClient;
import ms_soporte.ms_soporte.dto.CrearTicketRequest;
import ms_soporte.ms_soporte.dto.TicketDetalleResponse;
import ms_soporte.ms_soporte.dto.TicketResumenResponse;
import ms_soporte.ms_soporte.model.EstadoTicket;
import ms_soporte.ms_soporte.security.AuthUser;
import ms_soporte.ms_soporte.security.JwtService;
import ms_soporte.ms_soporte.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lote 1: capa Controller aislada (el Service se simula). Se adapta el test original del equipo
 * a las rutas del ERS y al usuario obtenido desde el token.
 */
@WebMvcTest(controllers = {TicketController.class, AdminTicketController.class})
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private JwtService jwtService;

    // Cierre (RN-44): el interceptor de cuenta ACTIVA usa AccountsClient; simulado = ACTIVA
    @MockitoBean
    private AccountsClient accountsClient;

    private TicketDetalleResponse detalle;

    @BeforeEach
    void setUp() {
        when(jwtService.validar("cliente-100")).thenReturn(new AuthUser(100L, "CLIENTE"));
        when(jwtService.validar("admin-1")).thenReturn(new AuthUser(1L, "ADMINISTRADOR"));
        detalle = new TicketDetalleResponse(1L, "SOP-000001", 100L, null, "Problema con pedido", "GENERAL",
                EstadoTicket.ABIERTA, LocalDateTime.now(), null, List.of(), List.of());
    }

    @Test
    void crearTicket_EndpointPost_Devuelve201() throws Exception {
        when(ticketService.crearTicket(any(CrearTicketRequest.class), eq(100L))).thenReturn(detalle);

        mockMvc.perform(post("/api/v1/support/requests").header("Authorization", "Bearer cliente-100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asunto\":\"Problema con pedido\",\"categoria\":\"GENERAL\",\"mensaje\":\"No llega\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.asunto").value("Problema con pedido"));
    }

    @Test
    void crearTicket_SinAsunto_Devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/support/requests").header("Authorization", "Bearer cliente-100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoria\":\"GENERAL\",\"mensaje\":\"No llega\"}"))
                .andExpect(status().isBadRequest())
                // Lote 1.1: formato ApiError; el campo con problema va dentro de "errores"
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.asunto").exists());
    }

    @Test
    void listarPropios_UsaUsuarioDelToken() throws Exception {
        when(ticketService.listarPropios(100L)).thenReturn(List.of(new TicketResumenResponse(1L, "SOP-000001", 100L,
                null, "Problema", "GENERAL", EstadoTicket.ABIERTA, LocalDateTime.now(), null)));

        mockMvc.perform(get("/api/v1/support/requests/me").header("Authorization", "Bearer cliente-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUsuario").value(100));
    }

    @Test
    void admin_ConCliente403_ConAdmin200() throws Exception {
        when(ticketService.obtenerParaAdministrador(1L)).thenReturn(detalle);

        mockMvc.perform(get("/api/v1/admin/support/requests/1").header("Authorization", "Bearer cliente-100"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/support/requests/1").header("Authorization", "Bearer admin-1"))
                .andExpect(status().isOk());
    }
}
