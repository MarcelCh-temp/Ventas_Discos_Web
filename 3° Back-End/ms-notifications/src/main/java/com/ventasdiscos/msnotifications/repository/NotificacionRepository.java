
package com.ventasdiscos.msnotifications.repository;

import com.ventasdiscos.msnotifications.model.Notificacion;
import com.ventasdiscos.msnotifications.model.TipoNotificacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRepository
        extends JpaRepository<Notificacion, Long> {

    // Buscar notificaciones de un usuario
    // Ordenadas desde la mas reciente
    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(
            Long usuarioId
    );

    // Contar las notificaciones no leidas de un usuario
    long countByUsuarioIdAndLeidaFalse(
            Long usuarioId
    );

    // Buscar notificaciones de un usuario por tipo
    List<Notificacion> findByUsuarioIdAndTipoOrderByFechaCreacionDesc(
            Long usuarioId,
            TipoNotificacion tipo
    );

    // Buscar notificaciones de un usuario por periodo
    List<Notificacion> findByUsuarioIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(
            Long usuarioId,
            LocalDateTime desde,
            LocalDateTime hasta
    );
}
