package ms_soporte.ms_soporte.service;

import ms_soporte.ms_soporte.client.AccountsClient;
import ms_soporte.ms_soporte.client.NotificacionesClient;
import ms_soporte.ms_soporte.client.OrdersClient;
import ms_soporte.ms_soporte.dto.CrearTicketRequest;
import ms_soporte.ms_soporte.dto.TicketDetalleResponse;
import ms_soporte.ms_soporte.exception.RecursoNoEncontradoException;
import ms_soporte.ms_soporte.exception.ServicioNoDisponibleException;
import ms_soporte.ms_soporte.model.EstadoTicket;
import ms_soporte.ms_soporte.model.Ticket;
import ms_soporte.ms_soporte.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Lote 1: reglas de negocio de TicketService, sin BD (el Repository se simula). */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private NotificacionesClient notificacionesClient;

    // Lote 5 (HU-78): Accounts simulado; por defecto no lanza nada = cuenta ACTIVA
    @Mock
    private AccountsClient accountsClient;

    // Cierre (RF-SOP-01): Orders simulado; por defecto no lanza nada = pedido propio
    @Mock
    private OrdersClient ordersClient;

    @InjectMocks
    private TicketService ticketService;

    private Ticket ticketPrueba;

    @BeforeEach
    void setUp() {
        ticketPrueba = new Ticket();
        ticketPrueba.setId(1L);
        ticketPrueba.setIdUsuario(100L);
        ticketPrueba.setAsunto("Error en Pago");
        ticketPrueba.setDescripcion("No se procesó mi tarjeta");
        ticketPrueba.registrarEstadoInicial(100L);
    }

    @Test
    void crearTicket_PoneDuenoEstadoYNumero() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        TicketDetalleResponse r = ticketService.crearTicket(
                new CrearTicketRequest(null, " Pago ", "pago", "No se procesó"), 100L);

        assertEquals(100L, r.idUsuario());
        assertEquals(EstadoTicket.ABIERTA, r.estado());
        assertEquals("SOP-000007", r.numeroTicket());
        assertEquals("PAGO", r.categoria());
        assertEquals(1, r.mensajes().size());
        verify(notificacionesClient).notificar(eq("SOPORTE-7-CREADO"), eq(100L), eq("SOPORTE_CREADO"), any(), any(), eq(7L));
    }

    // Lote 1.1 (observación A): dentro de una transacción, el aviso espera a que la BD confirme (commit)
    @Test
    void crearTicket_ConTransaccion_AvisaSoloDespuesDelCommit() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(8L);
            return t;
        });
        TransactionSynchronizationManager.initSynchronization();
        try {
            ticketService.crearTicket(new CrearTicketRequest(null, "Pago", "pago", "No se procesó"), 100L);
            // todavía no hay commit: no se avisó a nadie
            verify(notificacionesClient, never()).notificar(any(), any(), any(), any(), any(), any());

            // la BD confirma -> recién ahora se envía el aviso
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(notificacionesClient).notificar(eq("SOPORTE-8-CREADO"), eq(100L), eq("SOPORTE_CREADO"), any(), any(), eq(8L));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // Lote 1.1 (observación A): si la transacción se revierte, el cliente NO recibe aviso del cierre
    @Test
    void cerrar_SiLaTransaccionSeRevierte_NoAvisa() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticketPrueba));
        TransactionSynchronizationManager.initSynchronization();
        try {
            ticketService.cerrar(1L, 1L);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verify(notificacionesClient, never()).notificar(any(), any(), any(), any(), any(), any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // Lote 1.1 (D14): un error inesperado al avisar no impide responder con la solicitud creada
    @Test
    void crearTicket_SiElAvisoFalla_LaSolicitudIgualSeDevuelve() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(9L);
            return t;
        });
        doThrow(new RuntimeException("Notifications caído"))
                .when(notificacionesClient).notificar(any(), any(), any(), any(), any(), any());

        // si el aviso lanzara la excepción, esta línea fallaría el test
        TicketDetalleResponse r = ticketService.crearTicket(
                new CrearTicketRequest(null, "Pago", "pago", "No se procesó"), 100L);
        assertEquals("SOP-000009", r.numeroTicket());
    }

    @Test
    void obtenerPropio_DeOtroUsuario_Lanza404() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticketPrueba));
        assertThrows(RecursoNoEncontradoException.class, () -> ticketService.obtenerPropio(1L, 999L));
    }

    @Test
    void obtenerTicketPorId_NoExiste_LanzaExcepcion() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> ticketService.obtenerTicketPorId(99L));
    }

    @Test
    void actualizarEstado_SoloAbiertaAEnProceso() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticketPrueba));

        assertThrows(IllegalStateException.class, () -> ticketService.actualizarEstado(1L, EstadoTicket.CERRADA, 1L));
        assertThrows(IllegalStateException.class, () -> ticketService.actualizarEstado(1L, EstadoTicket.ABIERTA, 1L));

        TicketDetalleResponse r = ticketService.actualizarEstado(1L, EstadoTicket.EN_PROCESO, 1L);
        assertEquals(EstadoTicket.EN_PROCESO, r.estado());
        assertEquals(2, r.historial().size());
        // Lote 1.1 (DOC-02): el cambio intermedio no genera aviso (RF-NOT-01, ERS 12.2 y UML 06: creada/respondida/cerrada)
        verify(notificacionesClient, never()).notificar(any(), any(), any(), any(), any(), any());
        // EN_PROCESO -> EN_PROCESO otra vez: no permitido
        assertThrows(IllegalStateException.class, () -> ticketService.actualizarEstado(1L, EstadoTicket.EN_PROCESO, 1L));
    }

    @Test
    void cerrar_DosVeces_LaSegundaEs409YNoNotificaDeNuevo() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticketPrueba));

        ticketService.cerrar(1L, 1L);
        assertTrue(ticketPrueba.estaCerrada());
        assertNotNull(ticketPrueba.getFechaCierre());
        assertThrows(IllegalStateException.class, () -> ticketService.cerrar(1L, 1L));
        verify(notificacionesClient, times(1)).notificar(any(), any(), eq("SOPORTE_CERRADO"), any(), any(), any());
    }

    @Test
    void responder_TicketCerrado_NoGuarda() {
        ticketPrueba.cerrar(1L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticketPrueba));

        assertThrows(IllegalStateException.class, () -> ticketService.responderComoCliente(1L, 100L, "Hola"));
        assertThrows(IllegalStateException.class, () -> ticketService.responderComoAdministrador(1L, 1L, "Hola"));
        verify(ticketRepository, never()).flush();
    }

    // ---------------- Cierre · RF-SOP-01 / HU-59: pedido asociado ----------------

    @Test
    void crearTicket_ConPedidoPropio_ConsultaOrdersConElUsuarioDelTokenYGuarda() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(9L);
            return t;
        });
        TicketDetalleResponse r = ticketService.crearTicket(new CrearTicketRequest(5L, "Pago", "pago", "No llega"), 100L);
        assertEquals(5L, r.orderId());
        verify(ordersClient).exigirPedidoPropio(5L, 100L);
    }

    @Test
    void crearTicket_ConPedidoAjenoOInexistente_404YNoGuarda() {
        doThrow(new RecursoNoEncontradoException("El pedido indicado no existe o no corresponde a tu cuenta"))
                .when(ordersClient).exigirPedidoPropio(5L, 100L);
        assertThrows(RecursoNoEncontradoException.class,
                () -> ticketService.crearTicket(new CrearTicketRequest(5L, "Pago", "pago", "No llega"), 100L));
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(notificacionesClient, never()).notificar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void crearTicket_ConOrdersCaido_503YNoGuarda() {
        doThrow(new ServicioNoDisponibleException("No fue posible verificar el pedido asociado"))
                .when(ordersClient).exigirPedidoPropio(5L, 100L);
        assertThrows(ServicioNoDisponibleException.class,
                () -> ticketService.crearTicket(new CrearTicketRequest(5L, "Pago", "pago", "No llega"), 100L));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void crearTicket_SinPedido_NoConsultaOrders() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });
        ticketService.crearTicket(new CrearTicketRequest(null, "Pago", "pago", "No llega"), 100L);
        verifyNoInteractions(ordersClient);
    }
}
