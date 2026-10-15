
package com.ventasdiscos.msnotifications.service;

import com.ventasdiscos.msnotifications.dto.EventoNotificacionRequest;
import com.ventasdiscos.msnotifications.model.EventoProcesado;
import com.ventasdiscos.msnotifications.model.Notificacion;
import com.ventasdiscos.msnotifications.repository.EventoProcesadoRepository;
import com.ventasdiscos.msnotifications.repository.NotificacionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoNotificacionService {

    private final EventoProcesadoRepository eventoProcesadoRepository;
    private final NotificacionRepository notificacionRepository;

    public EventoNotificacionService(
            EventoProcesadoRepository eventoProcesadoRepository,
            NotificacionRepository notificacionRepository) {

        this.eventoProcesadoRepository = eventoProcesadoRepository;
        this.notificacionRepository = notificacionRepository;
    }

    // Registra una notificacion cuando el evento no existe previamente.
    @Transactional
    public boolean procesarEvento(EventoNotificacionRequest evento) {

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
