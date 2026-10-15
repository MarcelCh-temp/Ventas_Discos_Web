package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.dto.OfertaResponse;
import com.ventasdiscos.mscatalog.dto.PageResponse;
import com.ventasdiscos.mscatalog.dto.Paginacion;
import com.ventasdiscos.mscatalog.dto.PromocionRequest;
import com.ventasdiscos.mscatalog.dto.PromocionResponse;
import com.ventasdiscos.mscatalog.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mscatalog.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mscatalog.exception.ValidacionNegocioException;
import com.ventasdiscos.mscatalog.model.EstadoComercial;
import com.ventasdiscos.mscatalog.model.EstadoPromocion;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.model.Promocion;
import com.ventasdiscos.mscatalog.model.TipoDescuento;
import com.ventasdiscos.mscatalog.repository.ProductoRepository;
import com.ventasdiscos.mscatalog.repository.PromocionRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reglas de EP-09 (HU-47..52). El estado nunca se guarda: se deriva de fechas + desactivada. */
@Service
@RequiredArgsConstructor
public class PromocionService {

    private final PromocionRepository promocionRepository;
    private final ProductoRepository productoRepository;
    private final CalculadoraPromociones calculadora;

    /** HU-47 */
    @Transactional
    public PromocionResponse crear(PromocionRequest req) {
        LocalDate hoy = LocalDate.now();
        if (req.fechaFin().isBefore(hoy)) {
            throw new ValidacionNegocioException("La fecha de término no puede estar en el pasado");
        }
        Promocion p = new Promocion();
        aplicar(req, p);
        return PromocionResponse.from(promocionRepository.save(p), hoy);
    }

    /** HU-50: solo Programada o Activa; en Activa no se mueve la fecha de inicio. */
    @Transactional
    public PromocionResponse actualizar(Long id, PromocionRequest req) {
        LocalDate hoy = LocalDate.now();
        Promocion p = buscar(id);
        EstadoPromocion estado = p.estado(hoy);
        if (estado == EstadoPromocion.FINALIZADA || estado == EstadoPromocion.DESACTIVADA) {
            throw new OperacionNoPermitidaException("Una promoción finalizada o desactivada no puede editarse");
        }
        if (estado == EstadoPromocion.ACTIVA && !req.fechaInicio().equals(p.getFechaInicio())) {
            throw new OperacionNoPermitidaException("No se puede cambiar la fecha de inicio de una promoción activa");
        }
        if (req.fechaFin().isBefore(hoy)) {
            throw new ValidacionNegocioException("La fecha de término no puede estar en el pasado");
        }
        aplicar(req, p);
        return PromocionResponse.from(p, hoy);
    }

    /** HU-51: término manual anticipado; deja de aplicarse de inmediato y conserva su historial. */
    @Transactional
    public PromocionResponse desactivar(Long id) {
        LocalDate hoy = LocalDate.now();
        Promocion p = buscar(id);
        switch (p.estado(hoy)) {
            case DESACTIVADA -> throw new OperacionNoPermitidaException("La promoción ya está desactivada");
            case FINALIZADA -> throw new OperacionNoPermitidaException("La promoción ya finalizó");
            default -> {
                p.setDesactivada(true);
                p.setFechaDesactivacion(LocalDateTime.now());
            }
        }
        return PromocionResponse.from(p, hoy);
    }

    @Transactional(readOnly = true)
    public PromocionResponse obtener(Long id) {
        return PromocionResponse.from(buscar(id), LocalDate.now());
    }

    /** HU-48/HU-49: listado con filtros por estado, período (vigencia que se cruza), producto y nombre. */
    @Transactional(readOnly = true)
    public PageResponse<PromocionResponse> listar(EstadoPromocion estado, LocalDate desde, LocalDate hasta,
                                                  Long productId, String q, int pagina, int tamano) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ValidacionNegocioException("La fecha inicial no puede ser posterior a la final");
        }
        LocalDate hoy = LocalDate.now();
        Producto producto = productId == null ? null : productoRepository.findById(productId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));

        Specification<Promocion> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (estado != null) {
                switch (estado) {
                    case DESACTIVADA -> ps.add(cb.isTrue(root.get("desactivada")));
                    case PROGRAMADA -> ps.add(cb.and(cb.isFalse(root.get("desactivada")),
                            cb.greaterThan(root.<LocalDate>get("fechaInicio"), hoy)));
                    case ACTIVA -> ps.add(cb.and(cb.isFalse(root.get("desactivada")),
                            cb.lessThanOrEqualTo(root.<LocalDate>get("fechaInicio"), hoy),
                            cb.greaterThanOrEqualTo(root.<LocalDate>get("fechaFin"), hoy)));
                    case FINALIZADA -> ps.add(cb.and(cb.isFalse(root.get("desactivada")),
                            cb.lessThan(root.<LocalDate>get("fechaFin"), hoy)));
                }
            }
            if (desde != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("fechaFin"), desde));
            }
            if (hasta != null) {
                ps.add(cb.lessThanOrEqualTo(root.<LocalDate>get("fechaInicio"), hasta));
            }
            if (q != null && !q.isBlank()) {
                ps.add(cb.like(cb.lower(root.<String>get("nombre")), "%" + q.trim().toLowerCase() + "%"));
            }
            if (producto != null) {
                Join<Promocion, Producto> join = root.join("productos", JoinType.LEFT);
                query.distinct(true);
                ps.add(cb.or(cb.equal(join.get("id"), producto.getId()),
                        cb.equal(root.get("categoria"), producto.getCategoria())));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        Page<Promocion> page = promocionRepository.findAll(spec,
                Paginacion.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaInicio", "id")));
        return PageResponse.of(page, p -> PromocionResponse.from(p, hoy));
    }

    /** HU-52: solo promociones vigentes, con productos ACTIVOS y su precio promocional. */
    @Transactional(readOnly = true)
    public List<OfertaResponse> ofertasVigentes() {
        List<OfertaResponse> ofertas = new ArrayList<>();
        for (Promocion promo : promocionRepository.findActivas(LocalDate.now())) {
            Map<Long, Producto> productos = new LinkedHashMap<>();
            promo.getProductos().stream().filter(Producto::estaActivo).forEach(p -> productos.put(p.getId(), p));
            if (promo.getCategoria() != null) {
                productoRepository.findByCategoriaAndEstado(promo.getCategoria(), EstadoComercial.ACTIVO)
                        .forEach(p -> productos.putIfAbsent(p.getId(), p));
            }
            if (productos.isEmpty()) {
                continue;
            }
            List<OfertaResponse.ProductoEnOferta> items = productos.values().stream()
                    .sorted(Comparator.comparing(Producto::getNombre))
                    .map(p -> new OfertaResponse.ProductoEnOferta(p.getId(), p.getNombre(), p.getImagenUrl(),
                            p.getPrecioBase(), calculadora.precioFinal(p, promo)))
                    .toList();
            ofertas.add(new OfertaResponse(promo.getId(), promo.getNombre(), promo.getTipoDescuento(),
                    promo.getValor(), promo.getMontoMinimoCompra(), promo.getFechaFin(), items));
        }
        return ofertas;
    }

    // ===================== helpers =====================

    private Promocion buscar(Long id) {
        return promocionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Promoción no encontrada"));
    }

    /** Valida (HU-47/HU-50) y copia los datos al entity. */
    private void aplicar(PromocionRequest req, Promocion p) {
        if (req.fechaInicio().isAfter(req.fechaFin())) {
            throw new ValidacionNegocioException("La fecha de inicio no puede ser posterior a la de término");
        }
        if (req.tipoDescuento() == TipoDescuento.PORCENTAJE && req.valor().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ValidacionNegocioException("El porcentaje no puede superar 100");
        }
        List<Long> ids = req.productIds() == null ? List.of() : req.productIds();
        if (ids.isEmpty() && req.categoria() == null) {
            throw new ValidacionNegocioException("Debes indicar los productos o la categoría a la que aplica");
        }
        Set<Producto> productos = new HashSet<>(productoRepository.findAllById(ids));
        if (productos.size() != new HashSet<>(ids).size()) {
            Set<Long> encontrados = new HashSet<>();
            productos.forEach(x -> encontrados.add(x.getId()));
            List<Long> faltan = ids.stream().filter(i -> !encontrados.contains(i)).distinct().toList();
            throw new ValidacionNegocioException("No existen los productos: " + faltan);
        }
        // RN-27: un monto fijo mayor al precio dejaría precio final negativo
        if (req.tipoDescuento() == TipoDescuento.MONTO) {
            List<Producto> afectados = new ArrayList<>(productos);
            if (req.categoria() != null) {
                afectados.addAll(productoRepository.findByCategoriaAndEstado(req.categoria(), EstadoComercial.ACTIVO));
            }
            afectados.stream()
                    .filter(x -> req.valor().compareTo(x.getPrecioBase()) > 0)
                    .findFirst()
                    .ifPresent(x -> {
                        throw new ValidacionNegocioException("El descuento (" + req.valor()
                                + ") supera el precio de '" + x.getNombre() + "' (" + x.getPrecioBase()
                                + "): el precio final sería negativo");
                    });
        }
        p.setNombre(req.nombre().trim());
        p.setTipoDescuento(req.tipoDescuento());
        p.setValor(req.valor());
        p.setFechaInicio(req.fechaInicio());
        p.setFechaFin(req.fechaFin());
        p.setMontoMinimoCompra(req.montoMinimoCompra());
        p.setCategoria(req.categoria());
        p.getProductos().clear();
        p.getProductos().addAll(productos);
    }
}
