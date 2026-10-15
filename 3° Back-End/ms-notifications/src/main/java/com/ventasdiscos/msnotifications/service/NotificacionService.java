package com.ventasdiscos.msnotifications.service;

import com.ventasdiscos.msnotifications.model.Notificacion;
import com.ventasdiscos.msnotifications.model.TipoNotificacion;
import com.ventasdiscos.msnotifications.repository.NotificacionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(
            NotificacionRepository notificacionRepository
    ) {
        this.notificacionRepository = notificacionRepository;
    }

    // HU-54: Listar notificaciones de un usuario
    @Transactional(readOnly = true)
    public List<Notificacion> listarPorUsuario(Long usuarioId) {

        return notificacionRepository
                .findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    // HU-54: Contar notificaciones no leidas
    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {

        return notificacionRepository
                .countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    // HU-54: Marcar una notificacion como leida
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

    // HU-55: Filtrar notificaciones por tipo
    @Transactional(readOnly = true)
    public List<Notificacion> filtrarPorTipo(
            Long usuarioId,
            TipoNotificacion tipo
    ) {

        return notificacionRepository
                .findByUsuarioIdAndTipoOrderByFechaCreacionDesc(
                        usuarioId,
                        tipo
                );
    }

    // HU-55: Filtrar notificaciones por periodo
    @Transactional(readOnly = true)
    public List<Notificacion> filtrarPorPeriodo(
            Long usuarioId,
            LocalDateTime desde,
            LocalDateTime hasta
    ) {

        if (desde == null || hasta == null) {

            throw new IllegalArgumentException(
                    "Debe indicar ambas fechas"
            );
        }

        if (desde.isAfter(hasta)) {

            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }

        return notificacionRepository
                .findByUsuarioIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(
                        usuarioId,
                        desde,
                        hasta
                );
    }
}