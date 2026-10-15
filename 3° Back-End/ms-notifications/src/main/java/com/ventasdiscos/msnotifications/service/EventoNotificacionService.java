
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

    private final NotificacionRepository notificacionRepository;
    private final EventoProcesadoRepository eventoProcesadoRepository;

    public EventoNotificacionService(
            NotificacionRepository notificacionRepository,
            EventoProcesadoRepository eventoProcesadoRepository
    ) {
        this.notificacionRepository = notificacionRepository;
        this.eventoProcesadoRepository = eventoProcesadoRepository;
    }

    @Transactional
    public boolean procesarEvento(EventoNotificacionRequest request) {

        if (eventoProcesadoRepository.existsByOrigenAndEventoId(
                request.getOrigen(),
                request.getEventoId()
        )) {
            return false;
        }

        EventoProcesado evento = new EventoProcesado(
                request.getEventoId(),
                request.getOrigen()
        );

        eventoProcesadoRepository.saveAndFlush(evento);

        Notificacion notificacion = new Notificacion();

        notificacion.setUsuarioId(request.getUsuarioId());
        notificacion.setTitulo(request.getTitulo());
        notificacion.setMensaje(request.getMensaje());
        notificacion.setTipo(request.getTipo());
        notificacion.setReferenciaId(request.getReferenciaId());

        notificacionRepository.save(notificacion);

        return true;
    }
}
