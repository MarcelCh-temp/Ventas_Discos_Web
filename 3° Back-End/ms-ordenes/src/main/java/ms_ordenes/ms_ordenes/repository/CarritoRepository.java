package ms_ordenes.ms_ordenes.repository;

import jakarta.persistence.LockModeType;
import ms_ordenes.ms_ordenes.model.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Lote 3 (UML 02 Carrito). */
@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByUserId(Long userId);

    /** Bloquea el carrito al modificarlo o al confirmar la compra: un doble clic no crea dos pedidos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Carrito c where c.userId = :userId")
    Optional<Carrito> findByUserIdForUpdate(@Param("userId") Long userId);
}
