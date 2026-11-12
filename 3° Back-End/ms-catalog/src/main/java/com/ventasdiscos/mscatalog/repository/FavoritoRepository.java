package com.ventasdiscos.mscatalog.repository;

import com.ventasdiscos.mscatalog.model.Favorito;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    boolean existsByUserIdAndProductoId(Long userId, Long productoId);

    Optional<Favorito> findByUserIdAndProductoId(Long userId, Long productoId);

    /** HU-58: solo los favoritos del usuario autenticado, con el producto cargado (evita N+1). */
    @EntityGraph(attributePaths = "producto")
    List<Favorito> findByUserIdOrderByIdDesc(Long userId);

    /**
     * Lote 5 (HU-78, RN-43): borra TODOS los favoritos del usuario. Idempotente: si ya no tiene, borra 0.
     * No toca productos, existencias ni movimientos de stock.
     */
    @Modifying
    @Query("delete from Favorito f where f.userId = :userId")
    int borrarPorUsuario(@Param("userId") Long userId);
}
