package ms_ordenes.ms_ordenes.repository;

import jakarta.persistence.LockModeType;
import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
// Repositorio: acceso a datos con Spring Data JPA; no define reglas de negocio. Clase OrdenRepository.
public interface OrdenRepository extends JpaRepository<Orden, Long>, JpaSpecificationExecutor<Orden> {

    /** Original. Se conserva: consulta válida por dueño (la usará el Lote 5, verificación de pedidos activos). */
    List<Orden> findByIdUsuario(Long idUsuario);

    /** Lote 5 (HU-78, RN-36): ¿el usuario tiene algún pedido en esos estados? Solo lectura. */
    boolean existsByIdUsuarioAndEstadoIn(Long idUsuario, Collection<EstadoOrden> estados);

    /** Lote 3 (RN-40): seguimiento por número de pedido. */
    Optional<Orden> findByNumeroPedido(String numeroPedido);

    /**
     * Lote 3: bloquea la fila del pedido mientras se cambia su estado. Así una cancelación y un despacho
     * simultáneos no se pisan, y el stock se repone una sola vez (RN-39, T-07).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orden o where o.id = :id")
    Optional<Orden> findForUpdate(@Param("id") Long id);
}
