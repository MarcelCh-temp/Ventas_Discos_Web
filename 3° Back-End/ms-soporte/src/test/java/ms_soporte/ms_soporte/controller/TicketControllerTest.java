package ms_soporte.ms_soporte.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ms_soporte.ms_soporte.model.EstadoTicket;
import ms_soporte.ms_soporte.model.Ticket;
import ms_soporte.ms_soporte.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
public class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void crearTicket_EndpointPost_Devuelve201() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setIdUsuario(1L);
        ticket.setAsunto("Consulta");
        ticket.setDescripcion("Detalle de prueba");
        ticket.setEstado(EstadoTicket.ABIERTA);

        when(ticketService.crearTicket(any(Ticket.class))).thenReturn(ticket);

        mockMvc.perform(post("/api/soporte/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticket)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.asunto").value("Consulta"));
    }

    @Test
    void listarTodos_EndpointGet_DevuelveLista() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setIdUsuario(1L);

        when(ticketService.obtenerTodos()).thenReturn(List.of(ticket));

        mockMvc.perform(get("/api/soporte/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}