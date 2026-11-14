
package com.ventasdiscos.msnotifications.repository;

import com.ventasdiscos.msnotifications.model.Notificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Repositorio: acceso a datos con Spring Data JPA; no define reglas de negocio. Clase NotificacionRepository.
public interface NotificacionRepository
        extends JpaRepository<Notificacion, Long>,
                JpaSpecificationExecutor<Notificacion> {

    // Obtener una notificacion de un usuario.
    Optional<Notificacion> findByIdAndUsuarioId(
            Long id,
            Long usuarioId
    );

    // Contar notificaciones pendientes.
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    // Actualizar todas las notificaciones pendientes.
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query(
            "UPDATE Notificacion n " +
            "SET n.leida = true " +
            "WHERE n.usuarioId = :usuarioId " +
            "AND n.leida = false"
    )
    int marcarTodasComoLeidas(
            @Param("usuarioId") Long usuarioId
    );

    // Lote 5 (HU-78, RN-43): borrar TODAS las notificaciones personales de un usuario. Idempotente (si no hay, 0).
    // No toca la tabla de eventos procesados (no guarda datos personales y evita re-crear avisos repetidos).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Notificacion n WHERE n.usuarioId = :usuarioId")
    int borrarPorUsuario(@Param("usuarioId") Long usuarioId);
}
