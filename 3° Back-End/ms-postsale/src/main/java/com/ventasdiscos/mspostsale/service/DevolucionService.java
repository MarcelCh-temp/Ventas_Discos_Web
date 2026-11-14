package com.ventasdiscos.mspostsale.service;

import com.ventasdiscos.mspostsale.client.AccountsClient;
import com.ventasdiscos.mspostsale.client.InventoryClient;
import com.ventasdiscos.mspostsale.client.OrdersClient;
import com.ventasdiscos.mspostsale.client.OrdersDtos;
import com.ventasdiscos.mspostsale.client.OrdersDtos.PedidoOrders;
import com.ventasdiscos.mspostsale.dto.Devolver;
import com.ventasdiscos.mspostsale.dto.ItemRecibido;
import com.ventasdiscos.mspostsale.dto.ItemSolicitud;
import com.ventasdiscos.mspostsale.dto.Recibir;
import com.ventasdiscos.mspostsale.dto.Resolver;
import com.ventasdiscos.mspostsale.dto.SolicitudDetalleResponse;
import com.ventasdiscos.mspostsale.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mspostsale.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mspostsale.exception.ValidacionNegocioException;
import com.ventasdiscos.mspostsale.model.EstadoReposicion;
import com.ventasdiscos.mspostsale.model.ItemDevolucion;
import com.ventasdiscos.mspostsale.model.ItemRecepcionDevolucion;
import com.ventasdiscos.mspostsale.model.OperacionReposicion;
import com.ventasdiscos.mspostsale.model.RecepcionDevolucion;
import com.ventasdiscos.mspostsale.model.SolicitudPostVenta;
import com.ventasdiscos.mspostsale.model.TipoSolicitud;
import com.ventasdiscos.mspostsale.repository.ItemRecepcionDevolucionRepository;
import com.ventasdiscos.mspostsale.repository.OperacionReposicionRepository;
import com.ventasdiscos.mspostsale.repository.SolicitudPostVentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clase original. HU-69 (solicitar), HU-75 (resolver) y HU-76 (recepción) · RF-POS-02, RF-POS-08, RF-POS-09,
 * RN-20, RN-22, RN-23, RN-25. Lote 4: valida con los datos REALES de Orders (estado ENTREGADO, items[].productId,
 * cantidades) y repone en MS-Catalog. Antes exigía "ENTREGADA" (Orders usa ENTREGADO) y estaba bloqueada.
 * <p>
 * Reposición (RN-25, RF-POS-09): solo las unidades recibidas en condición BUENA. Cada ítem recibido apto tiene UNA
 * OperacionReposicion con operacionId fija "POSTSALE-RECEPCION-{idItem}" (formato original, DOC-08). Primero se
 * guarda la recepción con la operación PENDIENTE y después se llama a Catalog (fuera de la transacción): si Catalog no
 * responde, queda PENDIENTE y se reintenta con la MISMA operacionId (Catalog no repone dos veces). Nunca se pierde el
 * registro de la recepción ni se repone sin registro.
 */
@Service
public class DevolucionService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private static final Logger log = LoggerFactory.getLogger(DevolucionService.class);
    // Constante LOTE_REINTENTO: valor compartido que no cambia durante la ejecución.
    static final int LOTE_REINTENTO = 50;

    // Atributo solicitudes: dato o dependencia utilizada por esta clase.
    private final SolicitudPostVentaRepository solicitudes;
    // Atributo operaciones: dato o dependencia utilizada por esta clase.
    private final OperacionReposicionRepository operaciones;
    // Atributo itemsRecibidos: dato o dependencia utilizada por esta clase.
    private final ItemRecepcionDevolucionRepository itemsRecibidos;
    // Atributo orders: dato o dependencia utilizada por esta clase.
    private final OrdersClient orders;
    // Atributo inventory: dato o dependencia utilizada por esta clase.
    private final InventoryClient inventory;
    // Atributo accounts: dato o dependencia utilizada por esta clase.
    private final AccountsClient accounts;
    // Atributo support: dato o dependencia utilizada por esta clase.
    private final PostSaleSupport support;
    // Atributo transaccion: dato o dependencia utilizada por esta clase.
    private final TransactionTemplate transaccion;

    public DevolucionService(SolicitudPostVentaRepository solicitudes, OperacionReposicionRepository operaciones,
                             ItemRecepcionDevolucionRepository itemsRecibidos, OrdersClient orders,
                             InventoryClient inventory, AccountsClient accounts, PostSaleSupport support,
                             PlatformTransactionManager transactionManager) {
        this.solicitudes = solicitudes;
        this.operaciones = operaciones;
        this.itemsRecibidos = itemsRecibidos;
        this.orders = orders;
        this.inventory = inventory;
        this.accounts = accounts;
        this.support = support;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    // ===================== HU-69: solicitar =====================

    public SolicitudDetalleResponse solicitar(Long usuarioId, Devolver req) {
        Map<Long, Integer> productos = cantidadesSolicitadas(req.items());
        PedidoOrders p = support.pedidoDelCliente(req.pedidoId(), usuarioId);
        if (!OrdersDtos.ENTREGADO.equals(p.estado())) {
            throw new OperacionNoPermitidaException("El pedido " + p.referencia() + " está " + p.estado()
                    + ": solo se puede solicitar la devolución de un pedido ENTREGADO");
        }
        validarContraPedido(p, productos);
        return transaccion.execute(st -> {
            List<SolicitudPostVenta> previas = solicitudes.bloquearPorPedido(p.id(), TipoSolicitud.DEVOLUCION);
            validarSinDuplicados(p, productos, previas, null);
            SolicitudPostVenta nueva = SolicitudPostVenta.devolucion(p.id(), usuarioId, req.motivoSolicitud(), productos);
            // Lote 5 (HU-78, RN-37, RN-44): justo antes de guardar, Accounts confirma que la cuenta está ACTIVA.
            accounts.exigirCuentaActiva(usuarioId);
            SolicitudPostVenta s = solicitudes.saveAndFlush(nueva);
            support.notificarTrasCommit("devolucion-" + s.getId() + "-creada", usuarioId, "DEVOLUCION_CREADA",
                    "Solicitud de devolución registrada",
                    "Recibimos tu solicitud de devolución del pedido " + p.referencia() + " (solicitud " + s.getId()
                            + "). Te avisaremos cuando sea revisada.", s.getId());
            return SolicitudDetalleResponse.from(s, false);
        });
    }

    // ===================== HU-75: resolver =====================

    /** adminId sale del token del Administrador (DOC-09). */
    // Procesa resolver aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public SolicitudDetalleResponse resolver(Long id, Resolver req, Long adminId) {
        boolean aprobar = Boolean.TRUE.equals(req.aprobar());
        return transaccion.execute(st -> {
            SolicitudPostVenta s = bloquearDevolucion(id);
            if (!s.estaPendiente()) {
                if (aprobar && s.estaAprobada()) {
                    // Doble clic o reintento: ya está aprobada; no se cambia ni se vuelve a avisar (DOC-09).
                    return SolicitudDetalleResponse.from(s, true);
                }
                throw new OperacionNoPermitidaException("La solicitud ya fue resuelta (" + s.getEstado() + ")");
            }
            // RF-POS-08: antes de aprobar o rechazar se consulta el pedido real en Orders.
            PedidoOrders p = orders.obtener(s.getPedidoId());
            if (aprobar) {
                if (p.userId() == null || !p.userId().equals(s.getUsuarioId())) {
                    throw new OperacionNoPermitidaException("El pedido ya no corresponde al Cliente de la solicitud:"
                            + " no se puede aprobar");
                }
                if (!OrdersDtos.ENTREGADO.equals(p.estado())) {
                    throw new OperacionNoPermitidaException("El pedido " + p.referencia() + " está " + p.estado()
                            + ": solo se aprueba la devolución de un pedido ENTREGADO");
                }
                Map<Long, Integer> productos = new LinkedHashMap<>();
                s.getItems().forEach(i -> productos.merge(i.getProductoId(), i.getCantidadSolicitada(), Integer::sum));
                try {
                    validarContraPedido(p, productos);
                } catch (ValidacionNegocioException e) {
                    throw new OperacionNoPermitidaException(e.getMessage() + ": no se puede aprobar", e.getErrores());
                }
                validarSinDuplicados(p, productos, solicitudes.bloquearPorPedido(p.id(), TipoSolicitud.DEVOLUCION), s);
                s.aprobar(req.motivo(), adminId);
            } else {
                s.rechazar(req.motivo(), adminId);
            }
            String resultado = aprobar ? "aprobada" : "rechazada";
            String mensaje = "Tu solicitud de devolución del pedido " + p.referencia() + " fue " + resultado
                    + (aprobar ? ". Te indicaremos cómo entregar los productos." : ". Motivo: " + s.getMotivoResolucion());
            support.notificarTrasCommit("devolucion-" + id + "-resuelta", s.getUsuarioId(), "DEVOLUCION_RESUELTA",
                    "Solicitud de devolución " + resultado, mensaje, id);
            return SolicitudDetalleResponse.from(s, true);
        });
    }

    // ===================== HU-76: recepción =====================

    /** adminId sale del token del Administrador (DOC-09: usuarioRecepcionId). */
    // Procesa recibir aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public SolicitudDetalleResponse recibir(Long id, Recibir req, Long adminId) {
        List<Reposicion> reposiciones = transaccion.execute(st -> {
            SolicitudPostVenta s = bloquearDevolucion(id);
            RecepcionDevolucion r = s.registrarRecepcion(req.observaciones(), adminId);
            Map<String, String> errores = new LinkedHashMap<>();
            Set<Long> usados = new HashSet<>();
            for (int i = 0; i < req.items().size(); i++) {
                ItemRecibido recibido = req.items().get(i);
                String campo = "items[" + i + "]";
                if (!usados.add(recibido.itemDevolucionId())) {
                    errores.put(campo + ".itemDevolucionId", "Producto repetido en la recepción");
                    continue;
                }
                ItemDevolucion d = s.item(recibido.itemDevolucionId()).orElse(null);
                if (d == null) {
                    errores.put(campo + ".itemDevolucionId", "No pertenece a esta devolución");
                    continue;
                }
                int cantidad = entero(recibido.cantidadRecibida());
                int pendiente = s.pendienteDeRecibir(d);
                if (cantidad > pendiente) {
                    errores.put(campo + ".cantidadRecibida", pendiente == 0
                            ? "Ya se recibió todo lo autorizado de este producto"
                            : "Máximo " + pendiente + " (autorizadas: " + d.getCantidadSolicitada() + ", ya recibidas: "
                            + s.cantidadRecibida(d) + ")");
                    continue;
                }
                r.agregarItem(d, cantidad, recibido.condicion());
            }
            if (!errores.isEmpty()) {
                throw new ValidacionNegocioException("Hay productos o cantidades inválidos en la recepción", errores);
            }
            entityManager.persist(r);
            entityManager.flush();
            List<Reposicion> pendientes = new ArrayList<>();
            for (ItemRecepcionDevolucion item : r.getItems()) {
                OperacionReposicion op = item.prepararReposicion();
                if (op != null) {
                    pendientes.add(new Reposicion(op.getOperacionId(), item.getItemDevolucion().getProductoId(),
                            op.getCantidad(), s.getId()));
                }
            }
            solicitudes.flush();
            log.info("Recepción {} de la devolución {} registrada por el Administrador {} ({} reposiciones)",
                    r.getId(), s.getId(), adminId, pendientes.size());
            return pendientes;
        });
        reposiciones.forEach(this::aplicarReposicion);
        return transaccion.execute(st -> SolicitudDetalleResponse.from(solicitudes.findById(id).orElseThrow(), true));
    }

    /**
     * ERS 11.7: reintenta las reposiciones que Catalog no confirmó, con la MISMA operacionId (idempotente). Lo llama
     * ReposicionReintentoConfig cada cierto tiempo. Devuelve cuántas se intentaron.
     */
    public int reintentarReposicionesPendientes() {
        List<Reposicion> pendientes = transaccion.execute(st -> itemsRecibidos
                .conReposicionEn(EstadoReposicion.PENDIENTE, PageRequest.of(0, LOTE_REINTENTO)).stream()
                .map(i -> new Reposicion(i.getReposicion().getOperacionId(), i.getItemDevolucion().getProductoId(),
                        i.getReposicion().getCantidad(), i.getItemDevolucion().getSolicitud().getId()))
                .toList());
        pendientes.forEach(this::aplicarReposicion);
        return pendientes.size();
    }

    // ===================== Apoyo =====================

    private record Reposicion(String operacionId, Long productoId, Integer cantidad, Long solicitudId) {
    }

    /** Llama a Catalog FUERA de la transacción y registra el resultado en una transacción corta. */
    // Procesa aplicar reposicion aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private void aplicarReposicion(Reposicion rep) {
        try {
            InventoryClient.Respuesta r = inventory.reponer(rep.operacionId(),
                    "Devolución recibida apta (solicitud " + rep.solicitudId() + ")", rep.productoId(), rep.cantidad());
            if (r.resultado() == InventoryClient.Resultado.NO_CONFIRMADA) {
                log.warn("Reposición {} sigue PENDIENTE: Catalog no respondió; se reintentará", rep.operacionId());
                return;
            }
            transaccion.executeWithoutResult(st -> operaciones.findByOperacionId(rep.operacionId()).ifPresent(op -> {
                if (r.resultado() == InventoryClient.Resultado.APLICADA) {
                    op.confirmar();
                } else {
                    op.marcarFallida();
                    log.error("Reposición {} FALLIDA: Catalog respondió {} ({}). Requiere revisión del stock en Catalog",
                            rep.operacionId(), r.status(), r.detalle());
                }
            }));
        } catch (RuntimeException ex) {
            // La recepción ya quedó guardada; la operación sigue PENDIENTE y se reintentará con la misma operacionId.
            log.error("No se pudo registrar el resultado de la reposición {}; sigue PENDIENTE", rep.operacionId(), ex);
        }
    }

    // Procesa bloquear devolucion aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private SolicitudPostVenta bloquearDevolucion(Long id) {
        return solicitudes.bloquear(id)
                .filter(x -> x.esDeTipo(TipoSolicitud.DEVOLUCION))
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de devolución no encontrada"));
    }

    /** Productos sin repetir y cantidades enteras (RF-POS-02). Devuelve productoId -> cantidad. */
    // Procesa cantidades solicitadas aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private static Map<Long, Integer> cantidadesSolicitadas(List<ItemSolicitud> items) {
        Map<Long, Integer> productos = new LinkedHashMap<>();
        Map<String, String> errores = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            ItemSolicitud item = items.get(i);
            if (productos.containsKey(item.productoId())) {
                errores.put("items[" + i + "].productoId", "El producto aparece más de una vez");
                continue;
            }
            productos.put(item.productoId(), entero(item.cantidadSolicitada()));
        }
        if (!errores.isEmpty()) {
            throw new ValidacionNegocioException("Hay productos repetidos en la solicitud", errores);
        }
        return productos;
    }

    /** RN-22: solo productos realmente comprados y sin superar la cantidad comprada (datos reales de Orders). */
    // Procesa validar contra pedido aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private static void validarContraPedido(PedidoOrders p, Map<Long, Integer> productos) {
        Map<String, String> errores = new LinkedHashMap<>();
        int i = 0;
        for (Map.Entry<Long, Integer> e : productos.entrySet()) {
            int comprada = p.cantidadComprada(e.getKey());
            if (comprada == 0) {
                errores.put("items[" + i + "].productoId", "Este producto no forma parte del pedido");
            } else if (e.getValue() > comprada) {
                errores.put("items[" + i + "].cantidadSolicitada", "No puede superar lo comprado (" + comprada + ")");
            }
            i++;
        }
        if (!errores.isEmpty()) {
            throw new ValidacionNegocioException("Los productos o cantidades no corresponden al pedido", errores);
        }
    }

    /**
     * RN-23: no puede haber otra devolución PENDIENTE del mismo producto en el mismo pedido. RN-22: lo ya APROBADO
     * para ese producto más lo nuevo no puede superar lo comprado. {@code actual}: la solicitud que se está aprobando
     * (no se cuenta a sí misma).
     */
    private static void validarSinDuplicados(PedidoOrders p, Map<Long, Integer> productos,
                                             List<SolicitudPostVenta> previas, SolicitudPostVenta actual) {
        for (Map.Entry<Long, Integer> e : productos.entrySet()) {
            Long productoId = e.getKey();
            boolean pendiente = previas.stream().filter(s -> s != actual && !s.getId().equals(idDe(actual)))
                    .anyMatch(s -> s.estaPendiente() && s.incluyeProducto(productoId));
            if (pendiente) {
                throw new OperacionNoPermitidaException("Ya existe una solicitud de devolución pendiente para el producto "
                        + productoId + " del pedido " + p.referencia());
            }
            int aprobadas = previas.stream().filter(s -> s != actual && !s.getId().equals(idDe(actual)))
                    .filter(SolicitudPostVenta::estaAprobada).mapToInt(s -> s.cantidadSolicitada(productoId)).sum();
            int comprada = p.cantidadComprada(productoId);
            if (aprobadas + e.getValue() > comprada) {
                throw new OperacionNoPermitidaException("Del producto " + productoId + " ya hay " + aprobadas
                        + " unidades con devolución aprobada; se pueden pedir como máximo " + Math.max(0, comprada - aprobadas));
            }
        }
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método idDe (id de).
    private static Long idDe(SolicitudPostVenta s) {
        return s == null ? null : s.getId();
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método entero (entero).
    private static int entero(BigDecimal valor) {
        return valor.intValueExact();
    }
}
