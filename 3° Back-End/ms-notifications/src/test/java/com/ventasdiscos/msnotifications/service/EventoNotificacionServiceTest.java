package com.ventasdiscos.msnotifications.service;

import com.ventasdiscos.msnotifications.dto.EventoNotificacionRequest;
import com.ventasdiscos.msnotifications.model.EventoProcesado;
import com.ventasdiscos.msnotifications.model.Notificacion;
import com.ventasdiscos.msnotifications.model.TipoNotificacion;
import com.ventasdiscos.msnotifications.repository.EventoProcesadoRepository;
import com.ventasdiscos.msnotifications.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lote 1.1 (RN-33): unidad del Service, sin BD. Repositorios y gestor de transacciones simulados.
 * El caso "dos eventos iguales al mismo tiempo" se SIMULA haciendo que la BD rechace el guardado
 * por la clave única; no es una prueba con dos hilos reales.
 */
class EventoNotificacionServiceTest {

    private EventoProcesadoRepository eventoRepo;
    private NotificacionRepository notificacionRepo;
    private EventoNotificacionService service;

    private final EventoNotificacionRequest evento = new EventoNotificacionRequest(
            "SOPORTE-12-CREADO", "SUPPORT", 10L, "Solicitud de soporte creada",
            "Tu solicitud SOP-000012 fue registrada.", TipoNotificacion.SOPORTE_CREADO, 12L);

    @BeforeEach
    void setUp() {
        eventoRepo = mock(EventoProcesadoRepository.class);
        notificacionRepo = mock(NotificacionRepository.class);
        service = new EventoNotificacionService(eventoRepo, notificacionRepo, mock(PlatformTransactionManager.class));
    }

    @Test
    void eventoNuevo_RegistraLaNotificacion() {
        when(eventoRepo.existsByOrigenAndEventoId("SUPPORT", "SOPORTE-12-CREADO")).thenReturn(false);

        assertTrue(service.procesarEvento(evento));
        verify(notificacionRepo).save(any(Notificacion.class));
    }

    @Test
    void eventoRepetido_NoRegistraOtraNotificacion() {
        when(eventoRepo.existsByOrigenAndEventoId("SUPPORT", "SOPORTE-12-CREADO")).thenReturn(true);

        assertFalse(service.procesarEvento(evento));
        verify(eventoRepo, never()).saveAndFlush(any());
        verify(notificacionRepo, never()).save(any());
    }

    // Antes: este caso terminaba en error aunque el aviso ya estaba registrado. Ahora: igual que un repetido (200).
    @Test
    void repetidoSimultaneo_LaClaveUnicaLoFrena_YSeRespondeComoRepetido() {
        when(eventoRepo.existsByOrigenAndEventoId("SUPPORT", "SOPORTE-12-CREADO"))
                .thenReturn(false)  // 1ª consulta: el otro todavía no había terminado
                .thenReturn(true);  // nueva consulta tras el rechazo: el otro ya lo registró
        when(eventoRepo.saveAndFlush(any(EventoProcesado.class)))
                .thenThrow(new DataIntegrityViolationException("uk_evento_origen"));

        assertFalse(service.procesarEvento(evento));
        verify(notificacionRepo, never()).save(any()); // no se duplica el aviso
    }

    // Si la BD rechaza por otro motivo (el evento NO quedó registrado), no se oculta el error
    @Test
    void conflictoQueNoEsUnRepetido_SePropagaElError() {
        when(eventoRepo.existsByOrigenAndEventoId("SUPPORT", "SOPORTE-12-CREADO")).thenReturn(false);
        when(eventoRepo.saveAndFlush(any(EventoProcesado.class)))
                .thenThrow(new DataIntegrityViolationException("otro problema"));

        assertThrows(DataIntegrityViolationException.class, () -> service.procesarEvento(evento));
        verify(notificacionRepo, never()).save(any());
    }
}
