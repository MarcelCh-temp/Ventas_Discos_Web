package ms_soporte.ms_soporte.repository;

import ms_soporte.ms_soporte.model.EstadoTicket;
import ms_soporte.ms_soporte.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByIdUsuario(Long idUsuario);

    // Lote 5 (HU-78, RN-36): ¿el usuario tiene tickets en alguno de esos estados? Solo lectura.
    boolean existsByIdUsuarioAndEstadoIn(Long idUsuario, Collection<EstadoTicket> estados);

    // Lote 1 (HU-60): solicitudes propias, la más reciente primero
    List<Ticket> findByIdUsuarioOrderByFechaCreacionDesc(Long idUsuario);
}
