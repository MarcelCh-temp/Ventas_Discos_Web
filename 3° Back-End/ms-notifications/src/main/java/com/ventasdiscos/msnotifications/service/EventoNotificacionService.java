
package com.ventasdiscos.msnotifications.service;

import com.ventasdiscos.msnotifications.dto.EventoNotificacionRequest;
import com.ventasdiscos.msnotifications.model.EventoProcesado;
import com.ventasdiscos.msnotifications.model.Notificacion;
import com.ventasdiscos.msnotifications.repository.EventoProcesadoRepository;
import com.ventasdiscos.msnotifications.repository.NotificacionRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EventoNotificacionService {

    private final EventoProcesadoRepository eventoProcesadoRepository;
    private final NotificacionRepository notificacionRepository;
    private final TransactionTemplate transaccion;

    public EventoNotificacionService(
            EventoProcesadoRepository eventoProcesadoRepository,
            NotificacionRepository notificacionRepository,
            PlatformTransactionManager transactionManager) {

        this.eventoProcesadoRepository = eventoProcesadoRepository;
        this.notificacionRepository = notificacionRepository;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    /*
     * Lote 1.1 (RN-33, "reejecuciones accidentales de una operación ya completada").
     * Contrato: evento nuevo -> true (201); evento repetido -> false (200).
     *
     * Caso que faltaba: dos eventos IGUALES que llegan al mismo tiempo. Los dos pasan el "ya existe?",
     * y la clave única (origen + eventoId) frena al segundo en la BD. Antes eso terminaba en error
     * (500 y luego 409), aunque el aviso SÍ había quedado registrado una vez.
     *
     * Corrección mínima: el registro se hace dentro de una transacción (igual que antes con @Transactional,
     * ahora escrita con TransactionTemplate de Spring, la misma librería). Así, si la BD rechaza el duplicado,
     * la transacción ya terminó y se puede volver a consultar: si el evento quedó registrado, es un repetido
     * y se responde como cualquier repetido (false -> 200). Nada se duplica: la clave única sigue mandando.
     * (Con @Transactional no se puede: después del rechazo, Spring obliga a deshacer y responde error.)
     */
    public boolean procesarEvento(EventoNotificacionRequest evento) {
        try {
            return Boolean.TRUE.equals(transaccion.execute(estado -> registrarSiEsNuevo(evento)));
        } catch (DataIntegrityViolationException e) {
            if (eventoProcesadoRepository.existsByOrigenAndEventoId(evento.getOrigen(), evento.getEventoId())) {
                return false; // llegó otro igual al mismo tiempo y ya quedó registrado
            }
            throw e; // otro problema de datos: lo responde GlobalExceptionHandler (409)
        }
    }

    // Registra una notificacion cuando el evento no existe previamente (lógica original del equipo).
    private boolean registrarSiEsNuevo(EventoNotificacionRequest evento) {

        boolean yaProcesado =
                eventoProcesadoRepository.existsByOrigenAndEventoId(
                        evento.getOrigen(),
                        evento.getEventoId()
                );

        if (yaProcesado) {
            return false;
        }

        EventoProcesado registro = new EventoProcesado();
        registro.setEventoId(evento.getEventoId());
        registro.setOrigen(evento.getOrigen());

        eventoProcesadoRepository.saveAndFlush(registro);

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(evento.getUsuarioId());
        notificacion.setTitulo(evento.getTitulo());
        notificacion.setMensaje(evento.getMensaje());
        notificacion.setTipo(evento.getTipo());
        notificacion.setReferenciaId(evento.getReferenciaId());

        notificacionRepository.save(notificacion);

        return true;
    }
}
