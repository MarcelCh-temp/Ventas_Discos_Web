package com.ventasdiscos.msinventory.service;

import com.ventasdiscos.msinventory.dto.AjusteRequest;
import com.ventasdiscos.msinventory.dto.InventarioResponse;
import com.ventasdiscos.msinventory.dto.MovimientoRequest;
import com.ventasdiscos.msinventory.dto.MovimientoResponse;
import com.ventasdiscos.msinventory.dto.OperacionInternaItem;
import com.ventasdiscos.msinventory.dto.OperacionInternaRequest;
import com.ventasdiscos.msinventory.dto.OperacionInternaResponse;
import com.ventasdiscos.msinventory.dto.OperacionMasivaItem;
import com.ventasdiscos.msinventory.dto.OperacionMasivaRequest;
import com.ventasdiscos.msinventory.dto.PageResponse;
import com.ventasdiscos.msinventory.dto.Paginacion;
import com.ventasdiscos.msinventory.dto.UmbralRequest;
import com.ventasdiscos.msinventory.exception.OperacionNoPermitidaException;
import com.ventasdiscos.msinventory.exception.StockInsuficienteException;
import com.ventasdiscos.msinventory.exception.ValidacionNegocioException;
import com.ventasdiscos.msinventory.model.Disponibilidad;
import com.ventasdiscos.msinventory.model.Inventario;
import com.ventasdiscos.msinventory.model.MovimientoStock;
import com.ventasdiscos.msinventory.model.TipoMovimiento;
import com.ventasdiscos.msinventory.repository.InventarioRepository;
import com.ventasdiscos.msinventory.repository.MovimientoStockRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reglas de EP-07 (HU-38..44) + operaciones que piden Orders/PostSale.
 * Todo cambio de stock pasa por aplicarLote(): valida TODO primero, bloquea filas
 * y recién entonces aplica y registra historial (RNF-08, RN-10, HU-44).
 */
@Slf4j
@Service
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final MovimientoStockRepository movimientoRepository;
    private final int umbralPorDefecto;

    public InventarioService(InventarioRepository inventarioRepository,
                             MovimientoStockRepository movimientoRepository,
                             @Value("${inventory.umbral-critico-por-defecto}") int umbralPorDefecto) {
        this.inventarioRepository = inventarioRepository;
        this.movimientoRepository = movimientoRepository;
        this.umbralPorDefecto = umbralPorDefecto;
    }

    /** Una línea a aplicar; común para movimiento simple, masivo e interno. */
    record Operacion(Long productId, TipoMovimiento tipo, int cantidad, String motivo) {
    }

    // ===================== CONSULTAS =====================

    /** HU-38/HU-39: listado paginado con filtros combinables. */
    @Transactional(readOnly = true)
    public PageResponse<InventarioResponse> listar(Long productId, List<Long> productIds, Disponibilidad disponibilidad,
                                                   int pagina, int tamano) {
        Specification<Inventario> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (productId != null) {
                ps.add(cb.equal(root.get("productId"), productId));
            }
            if (productIds != null && !productIds.isEmpty()) {
                ps.add(root.get("productId").in(productIds));
            }
            if (disponibilidad != null) {
                switch (disponibilidad) {
                    case AGOTADO -> ps.add(cb.equal(root.get("stockActual"), 0));
                    case STOCK_CRITICO -> ps.add(cb.and(
                            cb.greaterThan(root.<Integer>get("stockActual"), 0),
                            cb.lessThanOrEqualTo(root.<Integer>get("stockActual"), root.<Integer>get("umbralCritico"))));
                    case DISPONIBLE -> ps.add(cb.greaterThan(root.<Integer>get("stockActual"),
                            root.<Integer>get("umbralCritico")));
                }
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        Page<Inventario> page = inventarioRepository.findAll(spec,
                Paginacion.of(pagina, tamano, Sort.by("productId")));
        return PageResponse.of(page, InventarioResponse::from);
    }

    /** HU-38 detalle. Un producto sin registro aún se informa con stock 0 (= Agotado, RN-09). */
    @Transactional(readOnly = true)
    public InventarioResponse obtener(Long productId) {
        return inventarioRepository.findByProductId(productId)
                .map(InventarioResponse::from)
                .orElseGet(() -> InventarioResponse.from(new Inventario(productId, umbralPorDefecto)));
    }

    /** Catalog -> Inventory: disponibilidad de varios productos en UNA llamada (evita N+1). */
    @Transactional(readOnly = true)
    public List<InventarioResponse> disponibilidadLote(Collection<Long> productIds) {
        if (productIds.size() > 500) {
            throw new ValidacionNegocioException("Máximo 500 productos por consulta");
        }
        Map<Long, Inventario> existentes = inventarioRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(Inventario::getProductId, Function.identity()));
        return new TreeSet<>(productIds).stream()
                .map(id -> InventarioResponse.from(existentes.getOrDefault(id, new Inventario(id, umbralPorDefecto))))
                .toList();
    }

    /** Catalog -> Inventory: ids con cierta disponibilidad, para filtrar el catálogo (HU-12, HU-16). */
    @Transactional(readOnly = true)
    public List<Long> productIdsPorDisponibilidad(Disponibilidad disponibilidad) {
        return switch (disponibilidad) {
            case DISPONIBLE -> inventarioRepository.findProductIdsDisponibles();
            case STOCK_CRITICO -> inventarioRepository.findProductIdsStockCritico();
            case AGOTADO -> inventarioRepository.findProductIdsAgotados();
        };
    }

    /** HU-43: historial filtrable por producto, tipo y período. */
    @Transactional(readOnly = true)
    public PageResponse<MovimientoResponse> historial(Long productId, TipoMovimiento tipo, LocalDate desde,
                                                      LocalDate hasta, int pagina, int tamano) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ValidacionNegocioException("La fecha inicial no puede ser posterior a la final");
        }
        Specification<MovimientoStock> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (productId != null) {
                ps.add(cb.equal(root.get("inventario").get("productId"), productId));
            }
            if (tipo != null) {
                ps.add(cb.equal(root.get("tipo"), tipo));
            }
            if (desde != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                ps.add(cb.lessThan(root.get("fechaHora"), hasta.plusDays(1).atStartOfDay()));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        Page<MovimientoStock> page = movimientoRepository.findAll(spec,
                Paginacion.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaHora", "id")));
        return PageResponse.of(page, MovimientoResponse::from);
    }

    // ===================== COMANDOS (Admin) =====================

    /** HU-41: entrada o salida. Una salida mayor al stock se rechaza (RN-10). */
    @Transactional
    public MovimientoResponse registrarMovimiento(Long productId, MovimientoRequest req, Long usuarioId) {
        if (req.tipo() == TipoMovimiento.AJUSTE) {
            throw new ValidacionNegocioException("Para ajustar por diferencia de inventario usa /adjustments");
        }
        Operacion op = new Operacion(productId, req.tipo(), entero(req.cantidad()), req.motivo());
        return MovimientoResponse.from(aplicarLote(List.of(op), null, usuarioId).get(0));
    }

    /** HU-40: ajuste al valor físico, con motivo; queda valor anterior y nuevo en historial. */
    @Transactional
    public MovimientoResponse ajustar(Long productId, AjusteRequest req, Long usuarioId) {
        Operacion op = new Operacion(productId, TipoMovimiento.AJUSTE, entero(req.nuevoStock()), req.motivo());
        return MovimientoResponse.from(aplicarLote(List.of(op), null, usuarioId).get(0));
    }

    /** HU-42: el Admin define el umbral; la disponibilidad se recalcula sola. */
    @Transactional
    public InventarioResponse configurarUmbral(Long productId, UmbralRequest req) {
        Inventario inv = bloquearOCrear(Set.of(productId)).get(productId);
        inv.setUmbralCritico(entero(req.umbralCritico()));
        return InventarioResponse.from(inv);
    }

    /** HU-44: valida el lote completo; si UNA fila falla no se aplica ninguna. */
    @Transactional
    public List<MovimientoResponse> aplicarMasivo(OperacionMasivaRequest req, Long usuarioId) {
        Map<String, String> errores = new LinkedHashMap<>();
        List<Operacion> ops = new ArrayList<>();
        for (int i = 0; i < req.operaciones().size(); i++) {
            OperacionMasivaItem item = req.operaciones().get(i);
            String campo = "operaciones[" + i + "]";
            int cantidad = entero(item.cantidad());
            if (item.tipo() != TipoMovimiento.AJUSTE && cantidad == 0) {
                errores.put(campo + ".cantidad", "Para entradas y salidas la cantidad debe ser mayor que cero");
            }
            if (item.tipo() == TipoMovimiento.AJUSTE && (item.motivo() == null || item.motivo().isBlank())) {
                errores.put(campo + ".motivo", "El ajuste requiere motivo");
            }
            ops.add(new Operacion(item.productId(), item.tipo(), cantidad, item.motivo()));
        }
        if (!errores.isEmpty()) {
            throw new ValidacionNegocioException("Corrige las filas indicadas; no se aplicó ningún cambio", errores);
        }
        return aplicarLote(ops, null, usuarioId).stream().map(MovimientoResponse::from).toList();
    }

    // ===================== COMANDOS (otros MS) =====================

    /** Catalog avisa que se creó un producto: se registra con stock 0. Idempotente. */
    @Transactional
    public InventarioResponse registrarProducto(Long productId) {
        return InventarioResponse.from(bloquearOCrear(Set.of(productId)).get(productId));
    }

    /**
     * Orders (venta) / PostSale (reposición). Idempotente por operacionId:
     * - misma operacionId + mismo contenido -> devuelve lo ya registrado sin mover stock (doble clic, reintento);
     * - misma operacionId + contenido distinto -> 409 (auditoría IN1): es otra operación usando la misma clave.
     * Todo o nada: si falta stock para una línea, no se descuenta ninguna (HU-25).
     * Concurrencia (auditoría IN2): primero se BLOQUEAN las filas de los productos y recién después se busca
     * la operacionId; así dos reintentos simultáneos se ejecutan uno tras otro y el segundo ve al primero.
     */
    @Transactional
    public OperacionInternaResponse aplicarOperacionInterna(OperacionInternaRequest req) {
        Set<Long> vistos = new TreeSet<>();
        List<Operacion> ops = new ArrayList<>();
        for (OperacionInternaItem item : req.items()) {
            if (item.tipo() == TipoMovimiento.AJUSTE) {
                throw new ValidacionNegocioException("Las operaciones internas solo admiten ENTRADA o SALIDA");
            }
            if (!vistos.add(item.productId())) {
                throw new ValidacionNegocioException("Producto repetido en la operación: " + item.productId());
            }
            ops.add(new Operacion(item.productId(), item.tipo(), item.cantidad(), req.motivo()));
        }

        bloquearOCrear(vistos);
        List<MovimientoStock> previos = movimientoRepository.findByOperacionId(req.operacionId());
        if (!previos.isEmpty()) {
            if (!huella(ops).equals(huellaMovimientos(previos))) {
                throw new OperacionNoPermitidaException("La operación " + req.operacionId()
                        + " ya fue registrada con otros productos, cantidades o motivo");
            }
            log.info("Operación {} ya aplicada; respuesta idempotente", req.operacionId());
            return new OperacionInternaResponse(req.operacionId(), true,
                    previos.stream().map(MovimientoResponse::from).toList());
        }
        List<MovimientoResponse> movs = aplicarLote(ops, req.operacionId(), null).stream()
                .map(MovimientoResponse::from).toList();
        return new OperacionInternaResponse(req.operacionId(), false, movs);
    }

    /** Contenido normalizado (sin importar el orden de los ítems) para comparar reintentos. */
    private static Set<String> huella(List<Operacion> ops) {
        return ops.stream()
                .map(o -> o.productId() + "|" + o.tipo() + "|" + o.cantidad() + "|" + o.motivo())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> huellaMovimientos(List<MovimientoStock> movimientos) {
        return movimientos.stream()
                .<String>map(m -> m.getInventario().getProductId() + "|" + m.getTipo() + "|" + m.getCantidad() + "|" + m.getMotivo())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    // ===================== NÚCLEO =====================

    /**
     * 1) bloquea las filas (FOR UPDATE) 2) simula TODO el lote 3) si alguna fila deja stock < 0,
     * lanza excepción y no toca nada 4) si todo es válido, aplica y registra cada movimiento.
     */
    List<MovimientoStock> aplicarLote(List<Operacion> ops, String operacionId, Long usuarioId) {
        Set<Long> ids = ops.stream().map(Operacion::productId).collect(Collectors.toCollection(TreeSet::new));
        Map<Long, Inventario> inventarios = bloquearOCrear(ids);

        // Paso 2: simulación (un producto puede aparecer varias veces en un lote masivo)
        Map<Long, Integer> simulado = new HashMap<>();
        inventarios.forEach((id, inv) -> simulado.put(id, inv.getStockActual()));
        Map<String, String> errores = new LinkedHashMap<>();
        for (int i = 0; i < ops.size(); i++) {
            Operacion op = ops.get(i);
            int actual = simulado.get(op.productId());
            int resultado = calcular(actual, op.tipo(), op.cantidad());
            if (resultado < 0) {
                errores.put("operaciones[" + i + "].cantidad", "Stock insuficiente para el producto "
                        + op.productId() + ": disponible " + actual + ", solicitado " + op.cantidad());
            } else {
                simulado.put(op.productId(), resultado);
            }
        }
        if (!errores.isEmpty()) {
            if (ops.size() == 1) {
                throw new StockInsuficienteException(errores.values().iterator().next());
            }
            throw new StockInsuficienteException(
                    "Hay operaciones que dejarían stock negativo; no se aplicó ningún cambio", errores);
        }

        // Paso 4: aplicar + historial automático (HU-43)
        LocalDateTime ahora = LocalDateTime.now();
        List<MovimientoStock> movimientos = new ArrayList<>();
        for (Operacion op : ops) {
            Inventario inv = inventarios.get(op.productId());
            int anterior = inv.getStockActual();
            inv.setStockActual(inv.calcularResultado(op.tipo(), op.cantidad()));

            MovimientoStock mov = new MovimientoStock();
            mov.setInventario(inv);
            mov.setOperacionId(operacionId);
            mov.setTipo(op.tipo());
            mov.setCantidad(op.cantidad());
            mov.setStockAnterior(anterior);
            mov.setStockResultante(inv.getStockActual());
            mov.setFechaHora(ahora);
            mov.setMotivo(op.motivo());
            mov.setUsuarioId(usuarioId);
            movimientos.add(movimientoRepository.save(mov));
        }
        return movimientos;
    }

    /** Bloquea las filas existentes y crea (stock 0) las que aún no existen. */
    private Map<Long, Inventario> bloquearOCrear(Set<Long> productIds) {
        Map<Long, Inventario> mapa = new HashMap<>();
        for (Inventario inv : inventarioRepository.findForUpdate(productIds)) {
            mapa.put(inv.getProductId(), inv);
        }
        for (Long id : productIds) {
            if (!mapa.containsKey(id)) {
                mapa.put(id, inventarioRepository.save(new Inventario(id, umbralPorDefecto)));
            }
        }
        return mapa;
    }

    private static int calcular(int actual, TipoMovimiento tipo, int cantidad) {
        return switch (tipo) {
            case ENTRADA -> actual + cantidad;
            case SALIDA -> actual - cantidad;
            case AJUSTE -> cantidad;
        };
    }

    /** BigDecimal ya validado con @Digits(fraction=0) -> int. */
    private static int entero(BigDecimal valor) {
        try {
            return valor.intValueExact();
        } catch (ArithmeticException e) {
            throw new ValidacionNegocioException("La cantidad debe ser un número entero válido");
        }
    }
}
