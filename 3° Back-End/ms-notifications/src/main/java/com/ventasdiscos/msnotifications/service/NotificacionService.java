
package com.ventasdiscos.msnotifications.service;

import com.ventasdiscos.msnotifications.model.Notificacion;
import com.ventasdiscos.msnotifications.repository.NotificacionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(
            NotificacionRepository notificacionRepository
    ) {
        this.notificacionRepository = notificacionRepository;
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listarPorUsuario(Long usuarioId) {
        return notificacionRepository
                .findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository
                .countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Transactional
    public void marcarComoLeida(Long notificacionId, Long usuarioId) {

        Notificacion notificacion = notificacionRepository
                .findById(notificacionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La notificacion no existe"
                        )
                );

        if (!notificacion.perteneceA(usuarioId)) {
            throw new IllegalArgumentException(
                    "La notificacion no pertenece al usuario"
            );
        }

        notificacion.marcarComoLeida();

        notificacionRepository.save(notificacion);
    }
}
