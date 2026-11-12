package com.ventasdiscos.mspostsale.repository;

import com.ventasdiscos.mspostsale.model.EstadoReposicion;
import com.ventasdiscos.mspostsale.model.ItemRecepcionDevolucion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Lote 4: busca las reposiciones que Catalog todavía no confirmó, para reintentarlas con la misma operacionId. */
public interface ItemRecepcionDevolucionRepository extends JpaRepository<ItemRecepcionDevolucion, Long> {

    @Query("select i from ItemRecepcionDevolucion i join fetch i.reposicion r join fetch i.itemDevolucion d "
            + "where r.estado = :estado order by r.id")
    List<ItemRecepcionDevolucion> conReposicionEn(@Param("estado") EstadoReposicion estado, Pageable limite);
}
