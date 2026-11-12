package com.ventasdiscos.mspostsale.service;

import com.ventasdiscos.mspostsale.dto.CancelacionResueltaResponse;
import com.ventasdiscos.mspostsale.dto.Cancelar;
import com.ventasdiscos.mspostsale.dto.Devolver;
import com.ventasdiscos.mspostsale.dto.ExisteResponse;
import com.ventasdiscos.mspostsale.dto.PageResponse;
import com.ventasdiscos.mspostsale.dto.Recibir;
import com.ventasdiscos.mspostsale.dto.Reembolsar;
import com.ventasdiscos.mspostsale.dto.Resolver;
import com.ventasdiscos.mspostsale.dto.SolicitudDetalleResponse;
import com.ventasdiscos.mspostsale.dto.SolicitudResumenResponse;
import com.ventasdiscos.mspostsale.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mspostsale.exception.ValidacionNegocioException;
import com.ventasdiscos.mspostsale.model.EstadoSolicitud;
import com.ventasdiscos.mspostsale.model.SolicitudPostVenta;
import com.ventasdiscos.mspostsale.model.TipoSolicitud;
import com.ventasdiscos.mspostsale.repository.SolicitudPostVentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Clase original: punto de entrada de los Controllers. Conserva las consultas (HU-70 a HU-73) y delega la creación y
 * resolución en CancelacionService, DevolucionService y ReembolsoService.
 * Lote 4: una sola entidad, así que ya no se mezclan listas de dos repositorios ni se pide "tipo" para desambiguar
 * un id (los ids son únicos en solicitudes_postventa). Paginación y filtros en la base.
 */
@Service
public class PostSaleService {

    static final int MAX_TAMANO = 100;
    private static final Sort RECIENTES = Sort.by(Sort.Order.desc("fechaSolicitud"), Sort.Order.desc("id"));

    private final SolicitudPostVentaRepository solicitudes;
    private final CancelacionService cancelacionService;
    private final DevolucionService devolucionService;
    private final ReembolsoService reembolsoService;

    public PostSaleService(SolicitudPostVentaRepository solicitudes, CancelacionService cancelacionService,
                           DevolucionService devolucionService, ReembolsoService reembolsoService) {
        this.solicitudes = solicitudes;
        this.cancelacionService = cancelacionService;
        this.devolucionService = devolucionService;
        this.reembolsoService = reembolsoService;
    }

    // ===================== Cliente =====================

    public SolicitudDetalleResponse solicitarCancelacion(Long usuarioId, Cancelar request) {
        return cancelacionService.solicitar(usuarioId, request);
    }

    public SolicitudDetalleResponse solicitarDevolucion(Long usuarioId, Devolver request) {
        return devolucionService.solicitar(usuarioId, request);
    }

    public SolicitudDetalleResponse registrarReembolso(Long usuarioId, Reembolsar request) {
        return reembolsoService.registrar(usuarioId, request);
    }

    /** HU-70 / RF-POS-03: solo las propias (cancelaciones, devoluciones y reembolsos). Sin solicitudes: contenido []. */
    @Transactional(readOnly = true)
    public PageResponse<SolicitudResumenResponse> misSolicitudes(Long usuarioId, TipoSolicitud tipo,
                                                                 EstadoSolicitud estado, int page, int size) {
        Page<SolicitudPostVenta> p = solicitudes.findAll(filtro(null, null, usuarioId, tipo, estado, null, null),
                pagina(page, size));
        return PageResponse.of(p, p.getContent().stream().map(SolicitudResumenResponse::from).toList());
    }

    /** HU-71 / RF-POS-04: una ajena responde 404 (no se revela que existe). */
    @Transactional(readOnly = true)
    public SolicitudDetalleResponse detalleMio(Long usuarioId, Long id) {
        SolicitudPostVenta s = solicitudes.findById(id)
                .filter(x -> x.estaMigrada() && x.perteneceA(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));
        return SolicitudDetalleResponse.from(s, false);
    }

    // ===================== Lote 5: contrato interno para Accounts (HU-78) =====================

    /**
     * RN-36: solicitudes PENDIENTE. Por precaución también responde true si el usuario tiene solicitudes antiguas sin
     * migrar (REGISTRO_LOTE_4 §5): de esas no se sabe si están pendientes, y Accounts nunca debe asumir que no hay.
     */
    @Transactional(readOnly = true)
    public ExisteResponse tieneSolicitudesPendientes(Long userId) {
        boolean existe = solicitudes.existsByUsuarioIdAndEstado(userId, EstadoSolicitud.PENDIENTE)
                || solicitudes.existenSinMigrar(userId);
        return new ExisteResponse(userId, existe);
    }

    // ===================== Administrador =====================

    /**
     * HU-72 / HU-73 · RF-POS-05/06: buscar por identificador, pedido o Cliente y filtrar por tipo (incluido
     * REEMBOLSO), estado y período (fecha de solicitud). Los criterios se combinan. Sin coincidencias: contenido [].
     */
    @Transactional(readOnly = true)
    public PageResponse<SolicitudDetalleResponse> listarAdmin(Long id, Long pedidoId, Long usuarioId, TipoSolicitud tipo,
                                                              EstadoSolicitud estado, LocalDate desde, LocalDate hasta,
                                                              int page, int size) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ValidacionNegocioException("El período no es válido",
                    Map.of("desde", "La fecha inicial no puede ser posterior a la final"));
        }
        Page<SolicitudPostVenta> p = solicitudes.findAll(filtro(id, pedidoId, usuarioId, tipo, estado, desde, hasta),
                pagina(page, size));
        return PageResponse.of(p, p.getContent().stream().map(s -> SolicitudDetalleResponse.from(s, true)).toList());
    }

    public CancelacionResueltaResponse resolverCancelacion(Long id, Resolver request, Long adminId) {
        return cancelacionService.resolver(id, request, adminId);
    }

    public SolicitudDetalleResponse resolverDevolucion(Long id, Resolver request, Long adminId) {
        return devolucionService.resolver(id, request, adminId);
    }

    public SolicitudDetalleResponse recibir(Long id, Recibir request, Long adminId) {
        return devolucionService.recibir(id, request, adminId);
    }

    // ===================== Apoyo =====================

    /**
     * Criterios combinados (los null no filtran), igual que el listado de Orders. Las filas antiguas de la herencia
     * todavía sin migrar (tipo o estado vacíos) no se muestran: ver REGISTRO_LOTE_4.
     */
    private static Specification<SolicitudPostVenta> filtro(Long id, Long pedidoId, Long usuarioId, TipoSolicitud tipo,
                                                           EstadoSolicitud estado, LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isNotNull(root.get("tipo")));
            p.add(cb.isNotNull(root.get("estado")));
            if (id != null) {
                p.add(cb.equal(root.get("id"), id));
            }
            if (pedidoId != null) {
                p.add(cb.equal(root.get("pedidoId"), pedidoId));
            }
            if (usuarioId != null) {
                p.add(cb.equal(root.get("usuarioId"), usuarioId));
            }
            if (tipo != null) {
                p.add(cb.equal(root.get("tipo"), tipo));
            }
            if (estado != null) {
                p.add(cb.equal(root.get("estado"), estado));
            }
            if (desde != null) {
                p.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fechaSolicitud"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                p.add(cb.lessThan(root.<LocalDateTime>get("fechaSolicitud"), hasta.plusDays(1).atStartOfDay()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static PageRequest pagina(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_TAMANO) {
            throw new ValidacionNegocioException("Paginación no válida",
                    Map.of("size", "page debe ser 0 o más y size entre 1 y " + MAX_TAMANO));
        }
        return PageRequest.of(page, size, RECIENTES);
    }
}
