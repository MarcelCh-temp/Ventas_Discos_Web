package com.ventasdiscos.msinventory.repository;

import com.ventasdiscos.msinventory.model.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long>,
        JpaSpecificationExecutor<MovimientoStock> {

    /** Idempotencia: ¿esta operación externa ya se aplicó? */
    List<MovimientoStock> findByOperacionId(String operacionId);
}
