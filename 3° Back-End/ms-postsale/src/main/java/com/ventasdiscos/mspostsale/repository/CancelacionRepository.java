package com.ventasdiscos.mspostsale.repository;

import com.ventasdiscos.mspostsale.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CancelacionRepository extends JpaRepository<SolicitudCancelacion,Long> {
    List<SolicitudCancelacion> findByUsuarioIdOrderByFechaSolicitudDesc(Long usuarioId);
    boolean existsByPedidoIdAndEstado(Long pedidoId, EstadoCancelacion estado);
}
