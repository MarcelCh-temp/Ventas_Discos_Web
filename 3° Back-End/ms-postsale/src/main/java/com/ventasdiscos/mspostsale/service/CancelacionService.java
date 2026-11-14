package com.ventasdiscos.mspostsale.service;

import com.ventasdiscos.mspostsale.client.AccountsClient;
import com.ventasdiscos.mspostsale.client.OrdersClient;
import com.ventasdiscos.mspostsale.client.OrdersDtos;
import com.ventasdiscos.mspostsale.client.OrdersDtos.CancelacionOrders;
import com.ventasdiscos.mspostsale.client.OrdersDtos.PedidoOrders;
import com.ventasdiscos.mspostsale.dto.CancelacionResueltaResponse;
import com.ventasdiscos.mspostsale.dto.Cancelar;
import com.ventasdiscos.mspostsale.dto.Resolver;
import com.ventasdiscos.mspostsale.dto.SolicitudDetalleResponse;
import com.ventasdiscos.mspostsale.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mspostsale.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mspostsale.model.SolicitudPostVenta;
import com.ventasdiscos.mspostsale.model.TipoSolicitud;
import com.ventasdiscos.mspostsale.repository.SolicitudPostVentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Clase original. HU-68 (solicitar) y HU-74 (resolver) · RF-POS-01, RF-POS-07, RN-18, RN-19, RN-23, RN-24, RN-26.
 * Lote 4: la aprobación ya no está bloqueada: usa el contrato interno de Orders del Lote 3.
 * <p>
 * Stock: al aprobar, PostSale NO repone stock. Lo hace Orders, una sola vez y de forma idempotente
 * (ORDERS-PEDIDO-{id}-CANCELACION, contrato aprobado en el Lote 3 y flujo "Orders coordina reposición idempotente en
 * Catalog" del ERS §13). Así una cancelación nunca repone dos veces. Las devoluciones reponen por otro camino
 * (POSTSALE-RECEPCION-{idItem}) y un pedido cancelado nunca llega a ENTREGADO: no se cruzan.
 * <p>
 * Consistencia al aprobar: la solicitud queda BLOQUEADA mientras Orders cancela; si Orders confirma, se guarda
 * APROBADA; si Orders responde 409/404/503, la solicitud sigue PENDIENTE y nada cambia. Si Orders alcanzó a cancelar
 * pero PostSale no pudo guardar, repetir la aprobación es seguro (Orders responde yaCancelado) y un rechazo posterior
 * se impide (el pedido ya está CANCELADO), para no dejar un pedido cancelado con su solicitud rechazada.
 */
@Service
public class CancelacionService {

    // Atributo solicitudes: dato o dependencia utilizada por esta clase.
    private final SolicitudPostVentaRepository solicitudes;
    // Atributo orders: dato o dependencia utilizada por esta clase.
    private final OrdersClient orders;
    // Atributo accounts: dato o dependencia utilizada por esta clase.
    private final AccountsClient accounts;
    // Atributo support: dato o dependencia utilizada por esta clase.
    private final PostSaleSupport support;
    // Atributo transaccion: dato o dependencia utilizada por esta clase.
    private final TransactionTemplate transaccion;

    public CancelacionService(SolicitudPostVentaRepository solicitudes, OrdersClient orders, AccountsClient accounts,
                              PostSaleSupport support, PlatformTransactionManager transactionManager) {
        this.solicitudes = solicitudes;
        this.orders = orders;
        this.accounts = accounts;
        this.support = support;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    /** HU-68. 201 con la solicitud PENDIENTE. */
    // Procesa solicitar aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public SolicitudDetalleResponse solicitar(Long usuarioId, Cancelar req) {
        PedidoOrders p = support.pedidoDelCliente(req.pedidoId(), usuarioId);
        if (!OrdersDtos.CANCELABLES.contains(p.estado())) {
            throw new OperacionNoPermitidaException("El pedido " + p.referencia() + " está " + p.estado()
                    + ": solo se puede solicitar la cancelación de un pedido PENDIENTE, CONFIRMADO o PREPARANDO"
                    + " (aún no despachado)");
        }
        return transaccion.execute(st -> {
            List<SolicitudPostVenta> previas = solicitudes.bloquearPorPedido(p.id(), TipoSolicitud.CANCELACION);
            if (previas.stream().anyMatch(SolicitudPostVenta::estaPendiente)) {
                throw new OperacionNoPermitidaException("Ya existe una solicitud de cancelación pendiente para el pedido "
                        + p.referencia());
            }
            if (previas.stream().anyMatch(SolicitudPostVenta::estaAprobada)) {
                throw new OperacionNoPermitidaException("La cancelación del pedido " + p.referencia() + " ya fue aprobada");
            }
            SolicitudPostVenta nueva = SolicitudPostVenta.cancelacion(p.id(), usuarioId, req.motivoSolicitud());
            // Lote 5 (HU-78, RN-37, RN-44): justo antes de guardar, Accounts confirma que la cuenta está ACTIVA
            // (403 si no; 503 si no responde). La excepción deshace la transacción: no se crea la solicitud.
            accounts.exigirCuentaActiva(usuarioId);
            SolicitudPostVenta s = solicitudes.saveAndFlush(nueva);
            support.notificarTrasCommit("cancelacion-" + s.getId() + "-creada", usuarioId, "CANCELACION_CREADA",
                    "Solicitud de cancelación registrada",
                    "Recibimos tu solicitud de cancelación del pedido " + p.referencia() + " (solicitud " + s.getId()
                            + "). Te avisaremos cuando sea revisada.", s.getId());
            return SolicitudDetalleResponse.from(s, false);
        });
    }

    /** HU-74. adminId sale del token del Administrador (DOC-09). */
    // Procesa resolver aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public CancelacionResueltaResponse resolver(Long id, Resolver req, Long adminId) {
        boolean aprobar = Boolean.TRUE.equals(req.aprobar());
        return transaccion.execute(st -> {
            SolicitudPostVenta s = solicitudes.bloquear(id)
                    .filter(x -> x.esDeTipo(TipoSolicitud.CANCELACION))
                    .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de cancelación no encontrada"));
            if (!s.estaPendiente()) {
                if (aprobar && s.estaAprobada()) {
                    // Reintento de una aprobación ya guardada: solo completa en Orders una reposición que hubiera
                    // quedado pendiente. No cambia la decisión, el motivo ni al responsable original (DOC-09).
                    CancelacionOrders r = orders.cancelar(s.getPedidoId(), s.getId(), s.getMotivoResolucion());
                    return new CancelacionResueltaResponse(SolicitudDetalleResponse.from(s, true), r.estado(),
                            r.reposicionStockPendiente());
                }
                throw new OperacionNoPermitidaException("La solicitud ya fue resuelta (" + s.getEstado() + ")");
            }
            if (aprobar) {
                // RN-24 / RF-POS-07: Orders revalida el estado ACTUAL con el pedido bloqueado: si ya fue DESPACHADO o
                // ENTREGADO responde 409 y la solicitud sigue PENDIENTE (el Administrador debe rechazarla).
                CancelacionOrders r = orders.cancelar(s.getPedidoId(), s.getId(), req.motivo());
                s.aprobar(req.motivo(), adminId);
                support.notificarTrasCommit("cancelacion-" + id + "-resuelta", s.getUsuarioId(), "CANCELACION_RESUELTA",
                        "Solicitud de cancelación aprobada",
                        "Tu solicitud de cancelación del pedido " + r.referencia() + " fue aprobada: el pedido quedó"
                                + " cancelado.", id);
                return new CancelacionResueltaResponse(SolicitudDetalleResponse.from(s, true), r.estado(),
                        r.reposicionStockPendiente());
            }
            // RF-POS-07: también se revalida antes de rechazar.
            PedidoOrders p = orders.obtener(s.getPedidoId());
            if (OrdersDtos.CANCELADO.equals(p.estado())) {
                throw new OperacionNoPermitidaException("El pedido " + p.referencia() + " ya está CANCELADO en Orders:"
                        + " la solicitud no puede rechazarse. Apruébala para dejarla coherente con el pedido"
                        + " (no se repone stock dos veces)");
            }
            s.rechazar(req.motivo(), adminId);
            support.notificarTrasCommit("cancelacion-" + id + "-resuelta", s.getUsuarioId(), "CANCELACION_RESUELTA",
                    "Solicitud de cancelación rechazada",
                    "Tu solicitud de cancelación del pedido " + p.referencia() + " fue rechazada. Motivo: "
                            + s.getMotivoResolucion(), id);
            return new CancelacionResueltaResponse(SolicitudDetalleResponse.from(s, true), p.estado(), null);
        });
    }
}
