package com.ventasdiscos.mscatalog.repository;

import com.ventasdiscos.mscatalog.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PromocionRepository extends JpaRepository<Promocion, Long>, JpaSpecificationExecutor<Promocion> {

    /** Promociones vigentes hoy (no desactivadas y dentro de fechas), con sus productos cargados. */
    @Query("""
            select distinct p from Promocion p left join fetch p.productos
            where p.desactivada = false and p.fechaInicio <= :hoy and p.fechaFin >= :hoy
            """)
    List<Promocion> findActivas(@Param("hoy") LocalDate hoy);
}
