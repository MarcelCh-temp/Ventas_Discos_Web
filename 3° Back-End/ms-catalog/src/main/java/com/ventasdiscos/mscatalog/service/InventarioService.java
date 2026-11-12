package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.dto.AjusteRequest;
import com.ventasdiscos.mscatalog.dto.ConsultaDisponibilidadRequest;
import com.ventasdiscos.mscatalog.dto.DisponibilidadItemResponse;
import com.ventasdiscos.mscatalog.dto.DisponibilidadResponse;
import com.ventasdiscos.mscatalog.dto.InventarioResponse;
import com.ventasdiscos.mscatalog.dto.MovimientoRequest;
import com.ventasdiscos.mscatalog.dto.MovimientoResponse;
import com.ventasdiscos.mscatalog.dto.OperacionInternaItem;
import com.ventasdiscos.mscatalog.dto.OperacionInternaRequest;
import com.ventasdiscos.mscatalog.dto.OperacionInternaResponse;
import com.ventasdiscos.mscatalog.dto.OperacionMasivaItem;
import com.ventasdiscos.mscatalog.dto.OperacionMasivaRequest;
import com.ventasdiscos.mscatalog.dto.PageResponse;
import com.ventasdiscos.mscatalog.dto.Paginacion;
import com.ventasdiscos.mscatalog.dto.UmbralRequest;
import com.ventasdiscos.mscatalog.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mscatalog.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mscatalog.exception.RecursoRetiradoException;
import com.ventasdiscos.mscatalog.exception.StockInsuficienteException;
import com.ventasdiscos.mscatalog.exception.ValidacionNegocioException;
import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.Disponibilidad;
import com.ventasdiscos.mscatalog.model.Inventario;
import com.ventasdiscos.mscatalog.model.MovimientoStock;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.model.TipoMovimiento;
import com.ventasdiscos.mscatalog.repository.InventarioRepository;
import com.ventasdiscos.mscatalog.repository.MovimientoStockRepository;
import com.ventasdiscos.mscatalog.repository.ProductoRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.ventasdiscos.mscatalog.security.Servicios;
import com.ventasdiscos.mscatalog.exception.AccesoDenegadoException;

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
 * Lote 2: trasladado desde ms-inventory a ms-catalog (ERS 11.4, CC-04, UML 03). El núcleo es el original:
 * todo cambio de stock pasa por aplicarLote(), que valida TODO primero, bloquea filas y recién entonces aplica
 * y registra el historial (RNF-08, RN-10, HU-44). Cambios del Lote 2:
 *  - Un producto debe EXISTIR en el catálogo para tener stock (antes Inventory aceptaba cualquier id).
 *  - Filtros por texto (nombre o id) y categoría en el listado (RF-INV-02), posibles ahora que Producto está aquí.
 *  - El stock cambia con los métodos del UML 03 en Inventario (stock nunca negativo también en el Model).
 *  - Disponibilidad pública y consulta en lote (rutas del ERS 12), sin exponer la cantidad exacta.
 * Ajustes de la revisión del Lote 2:
 *  - Los comandos que mueven stock usan READ_COMMITTED: el bloqueo FOR UPDATE toma solo las filas que existen
 *    (sin "bloqueos de hueco" de MySQL) y la búsqueda de operacionId ve lo que otra operación ya confirmó.
 *  - La fila que falta se crea en una transacción aparte y se tolera que otra operación la cree a la vez.
 *  - Una entrada que pasaría el máximo de int se rechaza (400) en vez de dar un número equivocado.
 */
@Slf4j
@Service
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final MovimientoStockRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final int umbralPorDefecto;
    /** Transacción NUEVA y corta, solo para crear la fila de inventario que falta (misma técnica que el Lote 1). */
    private final TransactionTemplate transaccionNueva;

    public InventarioService(InventarioRepository inventarioRepository,
                             MovimientoStockRepository movimientoRepository,
                             ProductoRepository productoRepository,
                             PlatformTransactionManager transactionManager,
                             @Value("${inventory.umbral-critico-por-defecto}") int umbralPorDefecto) {
        this.inventarioRepository = inventarioRepository;
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.umbralPorDefecto = umbralPorDefecto;
        this.transaccionNueva = new TransactionTemplate(transactionManager);
        this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transaccionNueva.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    /**
     * DOC-08 APROBADA (RNF-09): prefijo de operacionId según el servicio que pide el movimiento automático.
     * Identifica al responsable en el historial; NO es autenticación (eso es DOC-05). Desde el Lote 6 además se
     * comprueba que el servicio autenticado por su JWT de servicio coincida con el prefijo
     * (ver {@link #aplicarOperacionInterna(String, OperacionInternaRequest)}).
     */
    static final List<String> PREFIJOS_PERMITIDOS = List.of("ORDERS-", "POSTSALE-");

    /** Una línea a aplicar; común para movimiento simple, masivo e interno. */
    record Operacion(Long productId, TipoMovimiento tipo, int cantidad, String motivo) {
    }

    // ===================== CONSULTAS (Admin) =====================

    /** HU-38/HU-39 (RF-INV-01/02): listado paginado con filtros combinables. */
    @Transactional(readOnly = true)
    public PageResponse<InventarioResponse> listar(String q, Categoria categoria, Long productId, List<Long> productIds,
                                                   Disponibilidad disponibilidad, int pagina, int tamano) {
        Specification<Inventario> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Join<Inventario, Producto> producto = root.join("producto");
            if (q != null && !q.isBlank()) {
                String texto = q.trim();
                Predicate porNombre = cb.like(cb.lower(producto.<String>get("nombre")), "%" + texto.toLowerCase() + "%");
                // RF-INV-02 "buscar por producto/identificador": un número también busca el id exacto
                ps.add(texto.matches("\\d{1,18}")
                        ? cb.or(porNombre, cb.equal(root.get("productId"), Long.valueOf(texto)))
                        : porNombre);
            }
            if (categoria != null) {
                ps.add(cb.equal(producto.get("categoria"), categoria));
            }
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
        // Carga los productos de la página en UNA consulta: así InventarioResponse.from no consulta uno por uno (N+1)
        productoRepository.findAllById(page.map(Inventario::getProductId).getContent());
        return PageResponse.of(page, InventarioResponse::from);
    }

    /** HU-38 detalle. Un producto existente sin registro aún se informa con stock 0 (= Agotado, RN-09). */
    @Transactional(readOnly = true)
    public InventarioResponse obtener(Long productId) {
        Producto producto = buscarProducto(productId);
        return InventarioResponse.from(inventarioRepository.findByProductId(productId)
                .orElseGet(() -> new Inventario(producto, umbralPorDefecto)));
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

    // ===================== CONSULTAS (dentro de Catalog y públicas) =====================

    /**
     * Existencias de los productos de una página del catálogo, en UNA consulta (evita N+1).
     * Reemplaza la llamada HTTP que Catalog hacía a Inventory (ERS 11.3: "se resuelve localmente").
     * Un producto sin registro se informa con stock 0 (Agotado).
     */
    @Transactional(readOnly = true)
    public Map<Long, InventarioResponse> existencias(Collection<Producto> productos) {
        if (productos.isEmpty()) {
            return Map.of();
        }
        Map<Long, Inventario> existentes = inventarioRepository
                .findByProductIdIn(productos.stream().map(Producto::getId).toList()).stream()
                .collect(Collectors.toMap(Inventario::getProductId, Function.identity()));
        Map<Long, InventarioResponse> resultado = new HashMap<>();
        for (Producto p : productos) {
            Inventario inv = existentes.get(p.getId());
            resultado.put(p.getId(), InventarioResponse.from(inv != null ? inv : new Inventario(p, umbralPorDefecto)));
        }
        return resultado;
    }

    /** Ids con cierta disponibilidad, para el filtro del catálogo (HU-12, HU-16). */
    @Transactional(readOnly = true)
    public List<Long> productIdsPorDisponibilidad(Disponibilidad disponibilidad) {
        return switch (disponibilidad) {
            case DISPONIBLE -> inventarioRepository.findProductIdsDisponibles();
            case STOCK_CRITICO -> inventarioRepository.findProductIdsStockCritico();
            case AGOTADO -> inventarioRepository.findProductIdsAgotados();
        };
    }

    /**
     * ERS 12: GET /api/v1/inventory/{productId}/availability. Sin cantidad exacta.
     * Retirado por URL antigua -> 410, igual que el detalle del producto (RN-07, HU-17).
     */
    @Transactional(readOnly = true)
    public DisponibilidadResponse disponibilidadPublica(Long productId) {
        Producto producto = buscarProducto(productId);
        if (!producto.estaActivo()) {
            throw new RecursoRetiradoException(ProductoService.MSG_RETIRADO);
        }
        Inventario inv = inventarioRepository.findByProductId(productId)
                .orElseGet(() -> new Inventario(producto, umbralPorDefecto));
        return new DisponibilidadResponse(productId, inv.getDisponibilidad(), !inv.estaAgotado());
    }

    /**
     * ERS 12: POST /api/v1/inventory/availability/query. Para el carrito y la revalidación previa al checkout
     * (RN-13). Solo consulta: no bloquea ni descuenta (eso lo hace /internal/inventory/operations).
     */
    @Transactional(readOnly = true)
    public List<DisponibilidadItemResponse> consultarDisponibilidad(ConsultaDisponibilidadRequest req) {
        Set<Long> ids = new TreeSet<>();
        for (ConsultaDisponibilidadRequest.Item item : req.items()) {
            if (!ids.add(item.productId())) {
                throw new ValidacionNegocioException("Producto repetido en la consulta: " + item.productId());
            }
        }
        Map<Long, Producto> productos = productoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
        List<Long> inexistentes = ids.stream().filter(id -> !productos.containsKey(id)).toList();
        if (!inexistentes.isEmpty()) {
            throw new RecursoNoEncontradoException("Productos no encontrados: " + inexistentes);
        }
        Map<Long, Inventario> inventarios = inventarioRepository.findByProductIdIn(ids).stream()
                .collect(Collectors.toMap(Inventario::getProductId, Function.identity()));
        return req.items().stream().map(item -> {
            Producto p = productos.get(item.productId());
            Inventario inv = inventarios.getOrDefault(p.getId(), new Inventario(p, umbralPorDefecto));
            boolean haySuficiente = inv.hayStockSuficiente(item.cantidad());
            return new DisponibilidadItemResponse(p.getId(), item.cantidad(), inv.getDisponibilidad(),
                    haySuficiente, p.estaActivo() && haySuficiente);
        }).toList();
    }

    // ===================== COMANDOS (Admin) =====================

    /** HU-41: entrada o salida. Una salida mayor al stock se rechaza (RN-10). */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MovimientoResponse registrarMovimiento(Long productId, MovimientoRequest req, Long usuarioId) {
        if (req.tipo() == TipoMovimiento.AJUSTE) {
            throw new ValidacionNegocioException("Para ajustar por diferencia de inventario usa /adjustments");
        }
        Operacion op = new Operacion(productId, req.tipo(), entero(req.cantidad()), req.motivo());
        return MovimientoResponse.from(aplicarLote(List.of(op), null, usuarioId).get(0));
    }

    /** HU-40: ajuste al valor físico, con motivo; queda valor anterior y nuevo en historial. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MovimientoResponse ajustar(Long productId, AjusteRequest req, Long usuarioId) {
        Operacion op = new Operacion(productId, TipoMovimiento.AJUSTE, entero(req.nuevoStock()), req.motivo());
        return MovimientoResponse.from(aplicarLote(List.of(op), null, usuarioId).get(0));
    }

    /** HU-42: el Admin define el umbral; la disponibilidad se recalcula sola. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InventarioResponse configurarUmbral(Long productId, UmbralRequest req) {
        Inventario inv = bloquearOCrear(Set.of(productId)).get(productId);
        inv.configurarUmbral(entero(req.umbralCritico()));
        return InventarioResponse.from(inv);
    }

    /** HU-44: valida el lote completo; si UNA fila falla no se aplica ninguna. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

    // ===================== COMANDOS (dentro de Catalog y otros MS) =====================

    /**
     * UML 03 (Producto "1" *-- "1" Inventario): al crear un producto se crea su inventario con stock 0,
     * en la MISMA transacción. Reemplaza el aviso HTTP que Catalog hacía a Inventory. Idempotente.
     */
    @Transactional
    public InventarioResponse registrarProducto(Producto producto) {
        Inventario inv = inventarioRepository.findByProductId(producto.getId())
                .orElseGet(() -> inventarioRepository.save(new Inventario(producto, umbralPorDefecto)));
        return InventarioResponse.from(inv);
    }

    /**
     * Orders (venta) / PostSale (reposición). Idempotente por operacionId:
     * - misma operacionId + mismo contenido -> devuelve lo ya registrado sin mover stock (doble clic, reintento);
     * - misma operacionId + contenido distinto -> 409: es otra operación usando la misma clave.
     * Todo o nada: si falta stock para una línea, no se descuenta ninguna (HU-25).
     * Concurrencia: primero se BLOQUEAN las filas de los productos y recién después se busca la operacionId;
     * así dos reintentos simultáneos se ejecutan uno tras otro y el segundo ve al primero
     * (READ_COMMITTED garantiza que esa búsqueda vea lo que el primero ya confirmó).
     * RNF-09: aquí no hay usuario; el servicio responsable se identifica por el prefijo de operacionId (DOC-08).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    /**
     * Lote 6 · DOC-08 + DOC-05: punto de entrada de la ruta interna. Primero el formato del prefijo (400, igual que
     * antes); después, que el prefijo corresponda al servicio autenticado: ORDERS- solo desde ms-orders y POSTSALE-
     * solo desde ms-postsale (403). Recién entonces se aplica la operación.
     */
    public OperacionInternaResponse aplicarOperacionInterna(String servicioEmisor, OperacionInternaRequest req) {
        validarPrefijo(req.operacionId());
        validarOrigen(servicioEmisor, req.operacionId());
        return aplicarOperacionInterna(req);
    }

    public OperacionInternaResponse aplicarOperacionInterna(OperacionInternaRequest req) {
        validarPrefijo(req.operacionId());
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
     * lanza excepción y no toca nada 4) si todo es válido, aplica (métodos del UML en Inventario) y registra
     * cada movimiento.
     */
    List<MovimientoStock> aplicarLote(List<Operacion> ops, String operacionId, Long usuarioId) {
        Set<Long> ids = ops.stream().map(Operacion::productId).collect(Collectors.toCollection(TreeSet::new));
        Map<Long, Inventario> inventarios = bloquearOCrear(ids);

        // Paso 2: simulación (un producto puede aparecer varias veces en un lote masivo)
        Map<Long, Integer> simulado = new HashMap<>();
        inventarios.forEach((id, inv) -> simulado.put(id, inv.getStockActual()));
        Map<String, String> errores = new LinkedHashMap<>();
        Map<String, String> excesos = new LinkedHashMap<>();
        for (int i = 0; i < ops.size(); i++) {
            Operacion op = ops.get(i);
            int actual = simulado.get(op.productId());
            Integer resultado = calcular(actual, op.tipo(), op.cantidad());
            if (resultado == null) {
                // Revisión Lote 2: actual + cantidad no cabe en int (antes daba un número negativo equivocado)
                excesos.put("operaciones[" + i + "].cantidad", "La entrada dejaría el stock del producto "
                        + op.productId() + " sobre el máximo permitido (" + Inventario.STOCK_MAXIMO + ")");
            } else if (resultado < 0) {
                errores.put("operaciones[" + i + "].cantidad", "Stock insuficiente para el producto "
                        + op.productId() + ": disponible " + actual + ", solicitado " + op.cantidad());
            } else {
                simulado.put(op.productId(), resultado);
            }
        }
        if (!excesos.isEmpty()) {
            if (ops.size() == 1) {
                throw new ValidacionNegocioException(excesos.values().iterator().next(), excesos);
            }
            throw new ValidacionNegocioException(
                    "Hay operaciones que superarían el stock máximo; no se aplicó ningún cambio", excesos);
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
            inv.aplicar(op.tipo(), op.cantidad()); // Lote 2: métodos del UML 03 (stock nunca negativo)

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

    /**
     * Bloquea las filas existentes y crea (stock 0) las que aún no existen.
     * Lote 2: el producto debe existir en el catálogo (antes Inventory creaba stock para cualquier id).
     */
    private Map<Long, Inventario> bloquearOCrear(Set<Long> productIds) {
        Map<Long, Inventario> mapa = bloquear(productIds);
        Set<Long> faltantes = productIds.stream().filter(id -> !mapa.containsKey(id))
                .collect(Collectors.toCollection(TreeSet::new));
        if (faltantes.isEmpty()) {
            return mapa; // caso normal: todos los productos ya tienen su fila
        }
        Map<Long, Producto> productos = productoRepository.findAllById(faltantes).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
        List<Long> inexistentes = faltantes.stream().filter(id -> !productos.containsKey(id)).toList();
        if (!inexistentes.isEmpty()) {
            throw new RecursoNoEncontradoException("Productos no encontrados: " + inexistentes);
        }
        // Revisión Lote 2 (concurrencia): si dos operaciones llegan a la vez para un producto sin fila, las dos
        // intentarían crearla y la segunda chocaría con UNIQUE(product_id) -> error 500 y movimiento perdido.
        // Ahora la fila se crea en una transacción aparte; si otra operación la creó primero, se usa esa.
        for (Long id : faltantes) {
            crearFilaSiFalta(productos.get(id));
        }
        // Se vuelve a bloquear: ahora todas las filas existen y quedan tomadas por esta operación.
        Map<Long, Inventario> completo = bloquear(productIds);
        if (completo.size() != productIds.size()) {
            throw new IllegalStateException("No fue posible preparar el inventario de " + faltantes);
        }
        return completo;
    }

    private Map<Long, Inventario> bloquear(Set<Long> productIds) {
        Map<Long, Inventario> mapa = new HashMap<>();
        for (Inventario inv : inventarioRepository.findForUpdate(productIds)) {
            mapa.put(inv.getProductId(), inv);
        }
        return mapa;
    }

    /** Crea la fila (stock 0) en su propia transacción. Si otra operación la creó al mismo tiempo, no es error. */
    private void crearFilaSiFalta(Producto producto) {
        try {
            transaccionNueva.executeWithoutResult(estado -> {
                if (inventarioRepository.findByProductId(producto.getId()).isEmpty()) {
                    inventarioRepository.saveAndFlush(new Inventario(producto, umbralPorDefecto));
                }
            });
        } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
            // UNIQUE(product_id) o espera de bloqueo: otra operación la está creando. No se pierde nada:
            // el segundo bloqueo de bloquearOCrear comprueba que la fila exista antes de seguir.
            log.info("Inventario del producto {} creado por otra operación simultánea; se usa ese", producto.getId());
        }
    }

    /** DOC-08: 400 (ApiError con errores.operacionId) si no empieza con ORDERS- o POSTSALE- seguido de algo. */
    private static void validarPrefijo(String operacionId) {
        boolean valido = operacionId != null && PREFIJOS_PERMITIDOS.stream()
                .anyMatch(p -> operacionId.startsWith(p) && operacionId.length() > p.length());
        if (!valido) {
            throw new ValidacionNegocioException("operacionId no válido",
                    Map.of("operacionId", "Debe comenzar con ORDERS- (Orders) o POSTSALE- (PostSale)"));
        }
    }

    /** DOC-08 (Lote 6): el prefijo identifica al responsable y debe coincidir con el servicio del token → 403. */
    static void validarOrigen(String servicioEmisor, String operacionId) {
        String esperado = operacionId.startsWith("ORDERS-") ? Servicios.ORDERS
                : operacionId.startsWith("POSTSALE-") ? Servicios.POSTSALE : null;
        if (esperado == null || !esperado.equals(servicioEmisor)) {
            throw new AccesoDenegadoException("El servicio autenticado no corresponde al prefijo del operacionId");
        }
    }

    private Producto buscarProducto(Long productId) {
        return productoRepository.findById(productId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
    }

    /** Resultado simulado; null si una entrada supera el máximo de int (revisión Lote 2). */
    private static Integer calcular(int actual, TipoMovimiento tipo, int cantidad) {
        return switch (tipo) {
            case ENTRADA -> {
                long suma = (long) actual + cantidad;
                yield suma > Inventario.STOCK_MAXIMO ? null : (int) suma;
            }
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
