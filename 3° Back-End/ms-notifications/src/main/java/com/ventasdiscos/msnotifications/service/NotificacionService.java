
package com.ventasdiscos.msnotifications.service;

import com.ventasdiscos.msnotifications.model.Notificacion;
import com.ventasdiscos.msnotifications.repository.NotificacionRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(
            NotificacionRepository notificacionRepository
    ) {
        this.notificacionRepository = notificacionRepository;
    }

    public List<Notificacion> listarPorUsuario(Long usuarioId) {

        return notificacionRepository
                .findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }
}
