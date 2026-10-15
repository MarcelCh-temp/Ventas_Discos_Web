package ms_soporte.ms_soporte.repository;

import ms_soporte.ms_soporte.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByIdUsuario(Long idUsuario);
}