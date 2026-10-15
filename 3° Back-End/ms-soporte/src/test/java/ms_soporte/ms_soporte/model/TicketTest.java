package ms_soporte.ms_soporte.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TicketTest {

    @Test
    void testConstructoresYGettersSetters() {
        LocalDateTime ahora = LocalDateTime.now();

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setIdUsuario(10L);
        ticket.setAsunto("Asunto");
        ticket.setDescripcion("Desc");
        ticket.setCategoria("GENERAL");
        ticket.setEstado(EstadoTicket.ABIERTA);
        ticket.setFechaCreacion(ahora);

        assertEquals(1L, ticket.getId());
        assertEquals(10L, ticket.getIdUsuario());
        assertEquals("Asunto", ticket.getAsunto());
        assertEquals("Desc", ticket.getDescripcion());
        assertEquals("GENERAL", ticket.getCategoria());
        assertEquals(EstadoTicket.ABIERTA, ticket.getEstado());
        assertEquals(ahora, ticket.getFechaCreacion());
    }

    @Test
    void testCambioEstadoYCierre() {
        Ticket ticketVacio = new Ticket();
        ticketVacio.setId(2L);
        ticketVacio.setIdUsuario(20L);
        ticketVacio.setAsunto("Nuevo");
        ticketVacio.setDescripcion("Detalle");
        ticketVacio.setEstado(EstadoTicket.CERRADA);

        assertEquals(2L, ticketVacio.getId());
        assertEquals(20L, ticketVacio.getIdUsuario());
        assertEquals("Nuevo", ticketVacio.getAsunto());
        assertEquals(EstadoTicket.CERRADA, ticketVacio.getEstado());
    }
}