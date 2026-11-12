package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.dto.FiltroProducto;
import com.ventasdiscos.mscatalog.dto.InventarioResponse;
import com.ventasdiscos.mscatalog.dto.PageResponse;
import com.ventasdiscos.mscatalog.dto.Paginacion;
import com.ventasdiscos.mscatalog.dto.ProductoAdminResponse;
import com.ventasdiscos.mscatalog.dto.ProductoPublicoResponse;
import com.ventasdiscos.mscatalog.dto.ProductoRequest;
import com.ventasdiscos.mscatalog.dto.RetiroRequest;
import com.ventasdiscos.mscatalog.dto.RetiroResponse;
import com.ventasdiscos.mscatalog.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mscatalog.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mscatalog.exception.RecursoRetiradoException;
import com.ventasdiscos.mscatalog.exception.ValidacionNegocioException;
import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.Disponibilidad;
import com.ventasdiscos.mscatalog.model.DisponibilidadCatalogo;
import com.ventasdiscos.mscatalog.model.EstadoComercial;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.model.Promocion;
import com.ventasdiscos.mscatalog.repository.ProductoRepository;
import com.ventasdiscos.mscatalog.repository.PromocionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reglas de EP-02 (administración) y EP-03 (exploración).
 * Lote 2 (ERS 11.3/11.4, UML 03): el stock vive en este mismo MS. La disponibilidad se obtiene EN LOTE por página
 * desde InventarioService (consulta local), en vez de llamar por HTTP a MS-Inventory. Ya no hay degradación
 * NO_CONFIRMADA por caída de Inventory: si la BD de Catalog falla, falla la consulta completa.
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    public static final String MSG_RETIRADO = "Este producto ya no se encuentra disponible.";

    private final ProductoRepository productoRepository;
    private final PromocionRepository promocionRepository;
    private final InventarioService inventarioService;
    private final CalculadoraPromociones calculadora;

    // ===================== ADMIN (EP-02) =====================

    /**
     * HU-08: id generado por el sistema, estado ACTIVO.
     * Lote 2 (UML 03: Producto "1" *-- "1" Inventario): su inventario (stock 0) se crea en la MISMA transacción.
     */
    @Transactional
    public ProductoAdminResponse crear(ProductoRequest req) {
        validarCoherencia(req);
        Producto p = new Producto();
        copiar(req, p);
        p.setEstado(EstadoComercial.ACTIVO);
        Producto guardado = productoRepository.save(p);
        inventarioService.registrarProducto(guardado);
        return toAdmin(guardado, existencias(List.of(guardado)));
    }

    /** HU-09: id inmutable; solo campos comerciales. Pedidos ya hechos no cambian (snapshot en Orders). */
    @Transactional
    public ProductoAdminResponse actualizar(Long id, ProductoRequest req) {
        Producto p = buscar(id);
        validarCoherencia(req);
        copiar(req, p);
        return toAdmin(p, existencias(List.of(p)));
    }

    /** HU-10: retira uno o varios; informa los que ya estaban retirados y no repite la operación. */
    @Transactional
    public RetiroResponse retirar(RetiroRequest req) {
        Set<Long> ids = new LinkedHashSet<>(req.ids());
        Map<Long, Producto> encontrados = new java.util.HashMap<>();
        productoRepository.findAllById(ids).forEach(p -> encontrados.put(p.getId(), p));

        List<Long> retirados = new ArrayList<>();
        List<Long> yaRetirados = new ArrayList<>();
        List<Long> noEncontrados = new ArrayList<>();
        for (Long id : ids) {
            Producto p = encontrados.get(id);
            if (p == null) {
                noEncontrados.add(id);
            } else if (p.getEstado() == EstadoComercial.RETIRADO) {
                yaRetirados.add(id);
            } else {
                p.setEstado(EstadoComercial.RETIRADO);
                retirados.add(id);
            }
        }
        if (retirados.isEmpty()) {
            if (yaRetirados.isEmpty()) {
                throw new RecursoNoEncontradoException("Ninguno de los productos seleccionados existe");
            }
            throw new OperacionNoPermitidaException("Los productos seleccionados ya estaban retirados");
        }
        return new RetiroResponse(retirados, yaRetirados, noEncontrados);
    }

    /** HU-11/HU-12: activos, agotados y retirados; filtros combinables; paginado (RNF-11). */
    @Transactional(readOnly = true)
    public PageResponse<ProductoAdminResponse> listarAdmin(FiltroProducto filtro, int pagina, int tamano) {
        Page<Producto> page = buscarPagina(filtro, filtro.estado(), pagina, tamano);
        if (page == null) {
            return vacia(pagina, tamano);
        }
        Map<Long, InventarioResponse> inv = existencias(page.getContent());
        return PageResponse.of(page, page.getContent().stream().map(p -> toAdmin(p, inv)).toList());
    }

    @Transactional(readOnly = true)
    public ProductoAdminResponse obtenerAdmin(Long id) {
        Producto p = buscar(id);
        return toAdmin(p, existencias(List.of(p)));
    }

    // ===================== CLIENTE (EP-03) =====================

    /** HU-13..16: solo ACTIVO (RN-07). Agotados se muestran identificados (RN-08). */
    @Transactional(readOnly = true)
    public PageResponse<ProductoPublicoResponse> listarPublico(FiltroProducto filtro, int pagina, int tamano) {
        Page<Producto> page = buscarPagina(filtro, EstadoComercial.ACTIVO, pagina, tamano);
        if (page == null) {
            return vacia(pagina, tamano);
        }
        Map<Long, InventarioResponse> inv = existencias(page.getContent());
        List<Promocion> activas = promocionRepository.findActivas(LocalDate.now());
        return PageResponse.of(page, page.getContent().stream().map(p -> toPublico(p, inv, activas)).toList());
    }

    /** HU-17: detalle. Retirado por URL antigua -> 410 con el mensaje exigido (RN-07). */
    @Transactional(readOnly = true)
    public ProductoPublicoResponse obtenerPublico(Long id) {
        Producto p = buscar(id);
        if (!p.estaActivo()) {
            throw new RecursoRetiradoException(MSG_RETIRADO);
        }
        return toPublico(p, existencias(List.of(p)), promocionRepository.findActivas(LocalDate.now()));
    }

    // ===================== helpers =====================

    Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
    }

    /** Devuelve null si el filtro de disponibilidad no tiene ningún producto posible. */
    private Page<Producto> buscarPagina(FiltroProducto f, EstadoComercial estado, int pagina, int tamano) {
        validarRangoPrecio(f);
        Collection<Long> incluir = null;
        Collection<Long> excluir = null;
        if (f.disponibilidad() != null) {
            // Lote 2: consulta local al inventario de este mismo MS (antes era una llamada HTTP a Inventory).
            switch (f.disponibilidad()) {
                case DISPONIBLE, STOCK_CRITICO -> {
                    incluir = inventarioService.productIdsPorDisponibilidad(Disponibilidad.valueOf(f.disponibilidad().name()));
                    if (incluir.isEmpty()) {
                        return null;
                    }
                }
                case AGOTADO -> {
                    // Agotado = todo producto SIN stock, incluso los que aún no tienen registro de inventario
                    Set<Long> conStock = new HashSet<>(inventarioService.productIdsPorDisponibilidad(Disponibilidad.DISPONIBLE));
                    conStock.addAll(inventarioService.productIdsPorDisponibilidad(Disponibilidad.STOCK_CRITICO));
                    excluir = conStock;
                }
                case NO_CONFIRMADA -> throw new ValidacionNegocioException("Filtro de disponibilidad inválido");
            }
        }
        return productoRepository.findAll(spec(f, estado, incluir, excluir),
                Paginacion.of(pagina, tamano, Sort.by("nombre", "id")));
    }

    private Specification<Producto> spec(FiltroProducto f, EstadoComercial estado,
                                         Collection<Long> incluir, Collection<Long> excluir) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (estado != null) {
                ps.add(cb.equal(root.get("estado"), estado));
            }
            if (tieneTexto(f.q())) {
                String texto = f.q().trim();
                Predicate porNombre = cb.like(cb.lower(root.<String>get("nombre")), "%" + texto.toLowerCase() + "%");
                ps.add(texto.matches("\\d{1,18}")
                        ? cb.or(porNombre, cb.equal(root.get("id"), Long.valueOf(texto)))
                        : porNombre);
            }
            if (f.categoria() != null) {
                ps.add(cb.equal(root.get("categoria"), f.categoria()));
            }
            agregarIgualTexto(ps, cb, root.get("genero"), f.genero());
            agregarIgualTexto(ps, cb, root.get("formato"), f.formato());
            agregarIgualTexto(ps, cb, root.get("plataforma"), f.plataforma());
            if (f.precioMin() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("precioBase"), f.precioMin()));
            }
            if (f.precioMax() != null) {
                ps.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("precioBase"), f.precioMax()));
            }
            if (incluir != null) {
                ps.add(root.get("id").in(incluir));
            }
            if (excluir != null && !excluir.isEmpty()) {
                ps.add(cb.not(root.get("id").in(excluir)));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }

    private void agregarIgualTexto(List<Predicate> ps, jakarta.persistence.criteria.CriteriaBuilder cb,
                                   jakarta.persistence.criteria.Path<Object> campo, String valor) {
        if (tieneTexto(valor)) {
            ps.add(cb.equal(cb.lower(campo.as(String.class)), valor.trim().toLowerCase()));
        }
    }

    /** HU-16: un rango incoherente no se aplica. */
    private void validarRangoPrecio(FiltroProducto f) {
        if ((f.precioMin() != null && f.precioMin().signum() < 0)
                || (f.precioMax() != null && f.precioMax().signum() < 0)) {
            throw new ValidacionNegocioException("Los precios del filtro no pueden ser negativos");
        }
        if (f.precioMin() != null && f.precioMax() != null && f.precioMin().compareTo(f.precioMax()) > 0) {
            throw new ValidacionNegocioException("El precio mínimo no puede ser mayor que el máximo");
        }
    }

    /** HU-08: clasificación coherente. DECISIÓN TOMADA: plataforma solo (y siempre) para Videojuegos. */
    private void validarCoherencia(ProductoRequest req) {
        boolean tienePlataforma = tieneTexto(req.plataforma());
        if (req.categoria() == Categoria.VIDEOJUEGOS && !tienePlataforma) {
            throw new ValidacionNegocioException("Datos incoherentes",
                    Map.of("plataforma", "La plataforma es obligatoria para videojuegos"));
        }
        if (req.categoria() != Categoria.VIDEOJUEGOS && tienePlataforma) {
            throw new ValidacionNegocioException("Datos incoherentes",
                    Map.of("plataforma", "La plataforma solo aplica a videojuegos"));
        }
    }

    private void copiar(ProductoRequest req, Producto p) {
        p.setNombre(req.nombre().trim());
        p.setDescripcion(req.descripcion());
        p.setPrecioBase(req.precioBase());
        p.setCategoria(req.categoria());
        p.setGenero(req.genero().trim());
        p.setFormato(req.formato().trim());
        p.setPlataforma(tieneTexto(req.plataforma()) ? req.plataforma().trim() : null);
        p.setImagenUrl(req.imagenUrl());
    }

    /** Lote 2: existencias de varios productos en UNA consulta local (antes: llamada HTTP a Inventory). */
    Map<Long, InventarioResponse> existencias(Collection<Producto> productos) {
        return inventarioService.existencias(productos);
    }

    /** NO_CONFIRMADA queda solo por compatibilidad del contrato: con el stock local ya no se produce. */
    static DisponibilidadCatalogo disponibilidad(Map<Long, InventarioResponse> inv, Long id) {
        if (inv == null || !inv.containsKey(id)) {
            return DisponibilidadCatalogo.NO_CONFIRMADA;
        }
        return DisponibilidadCatalogo.valueOf(inv.get(id).disponibilidad().name());
    }

    private ProductoAdminResponse toAdmin(Producto p, Map<Long, InventarioResponse> inv) {
        Integer stock = inv != null && inv.containsKey(p.getId()) ? inv.get(p.getId()).stockActual() : null;
        return new ProductoAdminResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecioBase(),
                p.getCategoria(), p.getGenero(), p.getFormato(), p.getPlataforma(), p.getImagenUrl(),
                p.getEstado(), stock, disponibilidad(inv, p.getId()));
    }

    private ProductoPublicoResponse toPublico(Producto p, Map<Long, InventarioResponse> inv, List<Promocion> activas) {
        DisponibilidadCatalogo disp = disponibilidad(inv, p.getId());
        Optional<Promocion> promo = calculadora.mejorPromocion(p, activas, null);
        return new ProductoPublicoResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.getCategoria(),
                p.getGenero(), p.getFormato(), p.getPlataforma(), p.getImagenUrl(), p.getPrecioBase(),
                promo.map(pr -> calculadora.precioFinal(p, pr)).orElse(null),
                promo.map(Promocion::getNombre).orElse(null),
                disp, p.estaActivo() && disp.permiteCompra());
    }

    private static <T> PageResponse<T> vacia(int pagina, int tamano) {
        Paginacion.of(pagina, tamano, Sort.unsorted());
        return new PageResponse<>(List.of(), pagina, tamano, 0, 0);
    }

    private static boolean tieneTexto(String s) {
        return s != null && !s.isBlank();
    }
}
