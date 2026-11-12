package com.ventasdiscos.mspostsale.repository;

import com.ventasdiscos.mspostsale.model.EstadoSolicitud;
import com.ventasdiscos.mspostsale.model.SolicitudPostVenta;
import com.ventasdiscos.mspostsale.model.TipoSolicitud;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Lote 4: repositorio de la entidad única (reemplaza a CancelacionRepository, DevolucionRepository y
 * ReembolsoRepository de la herencia). Solo persistencia: las reglas están en Service/Model (CSR).
 */
public interface SolicitudPostVentaRepository extends JpaRepository<SolicitudPostVenta, Long>,
        JpaSpecificationExecutor<SolicitudPostVenta> {

    /**
     * Bloquea la solicitud mientras se resuelve, se registra una recepción o se usa como origen de un reembolso:
     * dos Administradores (o dos clics) no pueden resolverla ni superar cantidades al mismo tiempo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudPostVenta s where s.id = :id")
    Optional<SolicitudPostVenta> bloquear(@Param("id") Long id);

    /**
     * RN-23: lectura con bloqueo de las solicitudes del mismo pedido y tipo antes de crear una nueva. En MariaDB
     * (REPEATABLE READ) el bloqueo cubre también el hueco del índice idx_solicitud_pedido: si dos Clientes crean a la
     * vez, una de las dos operaciones espera o se cancela (409) y no quedan dos solicitudes activas.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudPostVenta s where s.pedidoId = :pedidoId and s.tipo = :tipo")
    List<SolicitudPostVenta> bloquearPorPedido(@Param("pedidoId") Long pedidoId, @Param("tipo") TipoSolicitud tipo);

    /** RN-23 / RF-POS-10: un solo reembolso por cancelación o devolución (además de la restricción única). */
    boolean existsBySolicitudOrigenId(Long solicitudOrigenId);

    /** Lote 5 (HU-78, RN-36): ¿el usuario tiene alguna solicitud en ese estado? Solo lectura. */
    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoSolicitud estado);

    /**
     * Lote 5 (HU-78): ¿el usuario tiene solicitudes antiguas de la herencia todavía SIN migrar (tipo o estado vacíos)?
     * No se puede saber si están pendientes, así que Accounts no debe tratarlas como "sin pendientes".
     */
    @Query("select case when count(s) > 0 then true else false end from SolicitudPostVenta s "
            + "where s.usuarioId = :usuarioId and (s.tipo is null or s.estado is null)")
    boolean existenSinMigrar(@Param("usuarioId") Long usuarioId);
}
