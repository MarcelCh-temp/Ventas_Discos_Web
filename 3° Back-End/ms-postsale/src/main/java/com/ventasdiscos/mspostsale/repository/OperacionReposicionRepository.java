package com.ventasdiscos.mspostsale.repository;

import com.ventasdiscos.mspostsale.model.OperacionReposicion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// Repositorio: acceso a datos con Spring Data JPA; no define reglas de negocio. Clase OperacionReposicionRepository.
public interface OperacionReposicionRepository extends JpaRepository<OperacionReposicion, Long> {
    Optional<OperacionReposicion> findByOperacionId(String operacionId);
}
