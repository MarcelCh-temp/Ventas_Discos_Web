package com.ventasdiscos.msnotifications.repository;

import com.ventasdiscos.msnotifications.model.Notificacion;
import com.ventasdiscos.msnotifications.model.TipoNotificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificacionRepository
        extends JpaRepository<Notificacion, Long>,
                JpaSpecificationExecutor<Notificacion> {

    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(
            Long usuarioId
    );

    long countByUsuarioIdAndLeidaFalse(
            Long usuarioId
    );

    List<Notificacion> findByUsuarioIdAndTipoOrderByFechaCreacionDesc(
            Long usuarioId,
            TipoNotificacion tipo
    );

    List<Notificacion> findByUsuarioIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(
            Long usuarioId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    Optional<Notificacion> findByIdAndUsuarioId(
            Long id,
            Long usuarioId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Notificacion n
            SET n.leida = true
            WHERE n.usuarioId = :usuarioId
            AND n.leida = false
            """)
    int marcarTodasComoLeidas(
            @Param("usuarioId") Long usuarioId
    );
}