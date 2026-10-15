package com.ventasdiscos.msaccounts.repository;

import com.ventasdiscos.msaccounts.model.Role;
import com.ventasdiscos.msaccounts.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acceso a la tabla usuario. Solo consultas; las reglas viven en CuentaService. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByRut(String rut);

    boolean existsByCorreo(String correo);

    boolean existsByRut(String rut);

    /** HU-05: el nuevo correo no puede pertenecer a OTRA cuenta. */
    boolean existsByCorreoAndIdNot(String correo, Long id);

    long countByRol(Role rol);
}
