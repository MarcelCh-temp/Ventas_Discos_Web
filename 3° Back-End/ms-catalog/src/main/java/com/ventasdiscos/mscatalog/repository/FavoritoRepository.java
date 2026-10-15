package com.ventasdiscos.mscatalog.repository;

import com.ventasdiscos.mscatalog.model.Favorito;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    boolean existsByUserIdAndProductoId(Long userId, Long productoId);

    Optional<Favorito> findByUserIdAndProductoId(Long userId, Long productoId);

    /** HU-58: solo los favoritos del usuario autenticado, con el producto cargado (evita N+1). */
    @EntityGraph(attributePaths = "producto")
    List<Favorito> findByUserIdOrderByIdDesc(Long userId);
}
