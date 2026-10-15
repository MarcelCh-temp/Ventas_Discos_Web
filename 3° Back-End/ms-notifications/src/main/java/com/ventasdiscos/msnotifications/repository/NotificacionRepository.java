
package com.ventasdiscos.msnotifications.repository;

import com.ventasdiscos.msnotifications.model.Notificacion;
import com.ventasdiscos.msnotifications.model.TipoNotificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRepository
        extends JpaRepository<Notificacion, Long>,
                JpaSpecificationExecutor<Notificacion> {

    // HU-54: Notificaciones de un usuario,
    // desde la mas reciente
    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(
            Long usuarioId
    );

    // HU-54: Cantidad de notificaciones no leidas
    long countByUsuarioIdAndLeidaFalse(
            Long usuarioId
    );

    // HU-55: Filtrar por tipo
    List<Notificacion> findByUsuarioIdAndTipoOrderByFechaCreacionDesc(
            Long usuarioId,
            TipoNotificacion tipo
    );

    // HU-55: Filtrar por periodo
    List<Notificacion> findByUsuarioIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(
            Long usuarioId,
            LocalDateTime desde,
            LocalDateTime hasta
    );
}
