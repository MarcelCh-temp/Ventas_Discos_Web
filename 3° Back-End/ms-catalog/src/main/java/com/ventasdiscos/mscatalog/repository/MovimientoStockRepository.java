package com.ventasdiscos.mscatalog.repository;

import com.ventasdiscos.mscatalog.model.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** Lote 2: trasladado desde ms-inventory sin cambios. */
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long>,
        JpaSpecificationExecutor<MovimientoStock> {

    /** Idempotencia: ¿esta operación externa ya se aplicó? */
    List<MovimientoStock> findByOperacionId(String operacionId);
}
