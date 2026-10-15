package com.ventasdiscos.mspostsale.repository;

import com.ventasdiscos.mspostsale.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReembolsoRepository extends JpaRepository<SolicitudReembolso,Long> {
    boolean existsByTipoOrigenAndSolicitudOrigenId(TipoOrigenReembolso tipo,Long solicitudOrigenId);
}
