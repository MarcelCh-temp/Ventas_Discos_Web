package com.ventasdiscos.msaccounts.repository;

import com.ventasdiscos.msaccounts.model.Role;
import com.ventasdiscos.msaccounts.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * Lote 5 (HU-78): bloquea la fila mientras cambia el estado de eliminación. Así un DELETE repetido, dos DELETE
     * simultáneos o la tarea programada no dejan la cuenta en estados contradictorios.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> bloquear(@Param("id") Long id);

    /** Lote 5: cuentas cuya eliminación quedó a medias (la retoma la tarea programada). */
    @Query("select u.id from Usuario u where u.estado = 'EN_ELIMINACION' order by u.fechaSolicitudEliminacion, u.id")
    List<Long> idsEnEliminacion(Pageable limite);

    /** Lote 5 · HU-45: las cuentas ELIMINADA (anonimizadas) ya no cuentan como usuarios. NULL = ACTIVA. */
    @Query("select count(u) from Usuario u where u.estado is null or u.estado <> 'ELIMINADA'")
    long contarNoEliminadas();

    @Query("select count(u) from Usuario u where u.rol = :rol and (u.estado is null or u.estado <> 'ELIMINADA')")
    long contarNoEliminadasPorRol(@Param("rol") Role rol);
}
