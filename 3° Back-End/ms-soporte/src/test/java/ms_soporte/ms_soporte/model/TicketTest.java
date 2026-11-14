package ms_soporte.ms_soporte.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

// Modelo de dominio: representa los datos y las reglas básicas de integridad de una entidad. Clase TicketTest.
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
        ticket.registrarEstadoInicial(10L); // Lote 1.1: antes setEstado(ABIERTA); el estado ya no se asigna a mano
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
        // Lote 1.1: antes setEstado(CERRADA). Ahora se cierra con la regla del modelo (fecha + historial)
        ticketVacio.registrarEstadoInicial(10L);
        ticketVacio.cerrar(1L);

        assertEquals(2L, ticketVacio.getId());
        assertEquals(20L, ticketVacio.getIdUsuario());
        assertEquals("Nuevo", ticketVacio.getAsunto());
        assertEquals(EstadoTicket.CERRADA, ticketVacio.getEstado());
        assertNotNull(ticketVacio.getFechaCierre());
        assertEquals(2, ticketVacio.getHistorial().size()); // null->ABIERTA, ABIERTA->CERRADA
    }

    // ---- Lote 1.1: reglas que protege la propia entidad ----

    @Test
    void registrarEstadoInicial_SoloEnSolicitudNueva() {
        Ticket ticket = new Ticket();
        ticket.registrarEstadoInicial(10L);
        ticket.cerrar(1L);
        // no se puede "reiniciar" una solicitud cerrada
        assertThrows(IllegalStateException.class, () -> ticket.registrarEstadoInicial(10L));
        assertEquals(EstadoTicket.CERRADA, ticket.getEstado());
    }

    @Test
    void cerrada_NoPuedeVolverAEnProceso() {
        Ticket ticket = new Ticket();
        ticket.registrarEstadoInicial(10L);
        ticket.cerrar(1L);
        assertThrows(IllegalStateException.class, () -> ticket.iniciarAtencion(1L));
        assertEquals(2, ticket.getHistorial().size());
    }

    @Test
    void mensajesEHistorial_SonDeSoloLectura() {
        Ticket ticket = new Ticket();
        ticket.registrarEstadoInicial(10L);
        ticket.agregarMensaje(new MensajeSoporte("Hola", RolAutor.CLIENTE, 10L));
        // vaciar la lista borraría la conversación en la BD (orphanRemoval): no se permite
        assertThrows(UnsupportedOperationException.class, () -> ticket.getMensajes().clear());
        assertThrows(UnsupportedOperationException.class, () -> ticket.getHistorial().clear());
        assertEquals(1, ticket.getMensajes().size());
        assertEquals(10L, ticket.getMensajes().get(0).getAutorId());
        assertSame(ticket, ticket.getMensajes().get(0).getTicket());
    }

    // Lote 1.1 (DOC-01, RNF-09): cada cambio de estado guarda quién lo hizo
    @Test
    void historial_RegistraElResponsableDeCadaCambio() {
        Ticket ticket = new Ticket();
        ticket.registrarEstadoInicial(10L); // el Cliente crea
        ticket.iniciarAtencion(1L);         // un Administrador la toma
        ticket.cerrar(2L);                  // otro Administrador la cierra

        assertEquals(3, ticket.getHistorial().size());
        assertEquals(10L, ticket.getHistorial().get(0).getUsuarioResponsableId());
        assertEquals(1L, ticket.getHistorial().get(1).getUsuarioResponsableId());
        assertEquals(2L, ticket.getHistorial().get(2).getUsuarioResponsableId());
    }

    // Lote 1.1 (DOC-01): no puede quedar un cambio de estado nuevo sin responsable
    @Test
    void cambioDeEstado_SinResponsable_NoSePermite() {
        Ticket ticket = new Ticket();
        assertThrows(NullPointerException.class, () -> ticket.registrarEstadoInicial(null));
        assertNull(ticket.getEstado());
        assertTrue(ticket.getHistorial().isEmpty());
    }

    @Test
    void asignarNumeroTicket_UnaSolaVezYConId() {
        Ticket ticket = new Ticket();
        assertThrows(IllegalStateException.class, ticket::asignarNumeroTicket); // aún no tiene id
        ticket.setId(5L);
        ticket.asignarNumeroTicket();
        assertEquals("SOP-000005", ticket.getNumeroTicket());
        ticket.setId(6L);
        ticket.asignarNumeroTicket();
        assertEquals("SOP-000005", ticket.getNumeroTicket()); // no cambia
    }
}