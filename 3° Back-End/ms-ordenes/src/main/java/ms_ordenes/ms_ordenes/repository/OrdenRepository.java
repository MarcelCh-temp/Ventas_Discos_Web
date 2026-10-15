package ms_ordenes.ms_ordenes.repository;

import ms_ordenes.ms_ordenes.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long> {
    List<Orden> findByIdUsuario(Long idUsuario);
}