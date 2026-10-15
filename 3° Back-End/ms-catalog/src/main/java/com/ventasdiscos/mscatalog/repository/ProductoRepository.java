package com.ventasdiscos.mscatalog.repository;

import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.EstadoComercial;
import com.ventasdiscos.mscatalog.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    List<Producto> findByCategoriaAndEstado(Categoria categoria, EstadoComercial estado);
}
