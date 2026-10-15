package ms_soporte.ms_soporte.service;

import ms_soporte.ms_soporte.model.EstadoTicket;
import ms_soporte.ms_soporte.model.Ticket;
import ms_soporte.ms_soporte.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

    private Ticket ticketPrueba;

    @BeforeEach
    void setUp() {
        ticketPrueba = new Ticket();
        ticketPrueba.setId(1L);
        ticketPrueba.setIdUsuario(1L);
        ticketPrueba.setAsunto("Consulta de envío");
        ticketPrueba.setDescripcion("¿Cuándo llega mi compra?");
        ticketPrueba.setEstado(EstadoTicket.ABIERTA);
    }

    @Test
    void crearTicket_GuardaYDevuelveTicket() {
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticketPrueba);

        Ticket resultado = ticketService.crearTicket(ticketPrueba);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Consulta de envío", resultado.getAsunto());
        assertEquals(EstadoTicket.ABIERTA, resultado.getEstado());
    }

    @Test
    void obtenerTodos_DevuelveLista() {
        when(ticketRepository.findAll()).thenReturn(List.of(ticketPrueba));

        List<Ticket> resultado = ticketService.obtenerTodos();

        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
    }
}