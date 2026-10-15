package com.ventasdiscos.msinventory.repository;

import com.ventasdiscos.msinventory.model.Inventario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventarioRepository extends JpaRepository<Inventario, Long>, JpaSpecificationExecutor<Inventario> {

    Optional<Inventario> findByProductId(Long productId);

    List<Inventario> findByProductIdIn(Collection<Long> productIds);

    /**
     * Concurrencia (prompt 20.1): SELECT ... FOR UPDATE bloquea las filas hasta terminar
     * la transacción, así dos compras simultáneas no leen el mismo stock y lo dejan negativo.
     * Orden por productId para que dos lotes bloqueen en el mismo orden (evita deadlock).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventario i where i.productId in :ids order by i.productId")
    List<Inventario> findForUpdate(@Param("ids") Collection<Long> productIds);

    @Query("select i.productId from Inventario i where i.stockActual > i.umbralCritico")
    List<Long> findProductIdsDisponibles();

    @Query("select i.productId from Inventario i where i.stockActual > 0 and i.stockActual <= i.umbralCritico")
    List<Long> findProductIdsStockCritico();

    @Query("select i.productId from Inventario i where i.stockActual = 0")
    List<Long> findProductIdsAgotados();
}
