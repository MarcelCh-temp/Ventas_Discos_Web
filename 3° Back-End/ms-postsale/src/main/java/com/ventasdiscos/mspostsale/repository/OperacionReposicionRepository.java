package com.ventasdiscos.mspostsale.repository;

import com.ventasdiscos.mspostsale.model.OperacionReposicion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OperacionReposicionRepository extends JpaRepository<OperacionReposicion, Long> {
    Optional<OperacionReposicion> findByOperacionId(String operacionId);
}
