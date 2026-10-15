package com.ventasdiscos.mspostsale.repository;

import com.ventasdiscos.mspostsale.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DevolucionRepository extends JpaRepository<SolicitudDevolucion, Long> {
    List<SolicitudDevolucion> findByUsuarioIdOrderByFechaSolicitudDesc(Long usuarioId);
    boolean existsByPedidoIdAndEstado(Long pedidoId, EstadoDevolucion estado);
    // Bloquea la solicitud durante una recepcion para evitar superar cantidades
    // cuando dos peticiones llegan simultaneamente.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from SolicitudDevolucion d where d.id = :id")
    Optional<SolicitudDevolucion> buscarParaRecepcion(@Param("id") Long id);
}
