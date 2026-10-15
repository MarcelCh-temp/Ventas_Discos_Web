package com.ventasdiscos.mspostsale.service;

import com.ventasdiscos.mspostsale.client.*;
import com.ventasdiscos.mspostsale.dto.*;
import com.ventasdiscos.mspostsale.model.*;
import com.ventasdiscos.mspostsale.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.TransactionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DevolucionService {
    private static final Logger log = LoggerFactory.getLogger(DevolucionService.class);
    private final DevolucionRepository devoluciones;
    private final OperacionReposicionRepository operaciones;
    private final InventoryClient inventory;
    private final PostSaleSupport support;
    private final TransactionTemplate transaccionNueva;
    public DevolucionService(DevolucionRepository devoluciones,
    OperacionReposicionRepository operaciones,
    InventoryClient inventory,
    PostSaleSupport support,
    PlatformTransactionManager transactionManager) {
        this.devoluciones = devoluciones;
        this.operaciones = operaciones;
        this.inventory = inventory;
        this.support = support;
        this.transaccionNueva = new TransactionTemplate(transactionManager);
        this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    @Transactional

    public Map<String,Object> solicitarDevolucion(Devolver req) {
        JsonNode p=support.verificarPedido(req.pedidoId(),req.usuarioId());
        if (!"ENTREGADA".equals(support.estado(p))) throw new IllegalArgumentException("Solo se permite devolucion de pedidos entregados");
        if (devoluciones.existsByPedidoIdAndEstado(req.pedidoId(),EstadoDevolucion.PENDIENTE))
        throw new IllegalStateException("Ya existe una devolucion pendiente");
        if (req.items().stream().map(ItemSolicitud::productoId).distinct().count()!=req.items().size())
        throw new IllegalArgumentException("El mismo producto aparece dos veces");
        JsonNode lineas=p.path("items");
        if (!lineas.isArray()) throw new DependenciaIncompletaException("Orders no expone productos y cantidades del pedido; falta el contrato necesario para RN-22");
        for (ItemSolicitud item:req.items()) {
            long comprado=0;
            for(JsonNode linea:lineas) if(linea.path("productoId").asLong(-1)==item.productoId())
            comprado+=linea.path("cantidad").asLong(0);
            if(comprado<item.cantidadSolicitada()) throw new IllegalArgumentException("La cantidad supera lo comprado para el producto "+item.productoId());
        }
        SolicitudDevolucion s=new SolicitudDevolucion();
        s.setPedidoId(req.pedidoId());
        s.setUsuarioId(req.usuarioId());
        s.setMotivoSolicitud(req.motivoSolicitud());
        for (ItemSolicitud item:req.items()) {
            ItemDevolucion i=new ItemDevolucion();
            i.setSolicitud(s);
            i.setProductoId(item.productoId());
            i.setCantidadSolicitada(item.cantidadSolicitada());
            s.getItems().add(i);
        }
        s=devoluciones.saveAndFlush(s);
        support.notificarTrasCommit("devolucion-"+s.getId()+"-creada",s.getUsuarioId(),"DEVOLUCION_CREADA","Devolucion solicitada","Se registro tu solicitud",s.getId());
        return support.resumen(s,"DEVOLUCION",s.getEstado().name());
    }
    @Transactional

    public Map<String,Object> resolverDevolucion(Long id, Resolver req) {
        SolicitudDevolucion d=devoluciones.findById(id).orElseThrow(()->new NoSuchElementException("Devolucion no encontrada"));
        if(d.getEstado()!=EstadoDevolucion.PENDIENTE)throw new IllegalStateException("Devolucion ya resuelta");
        if(req.aprobar())d.aprobar(req.motivo());
        else d.rechazar(req.motivo());
        support.notificarTrasCommit("devolucion-"+id+"-resuelta",d.getUsuarioId(),"DEVOLUCION_RESUELTA","Devolucion resuelta","Estado: "+d.getEstado(),id);
        return support.resumen(d,"DEVOLUCION",d.getEstado().name());
    }
    @Transactional

    public Map<String,Object> recibir(Long id, Recibir req) {
        SolicitudDevolucion d=devoluciones.buscarParaRecepcion(id).orElseThrow(()->new NoSuchElementException("Devolucion no encontrada"));
        if(!d.estaAprobada())throw new IllegalStateException("La devolucion no esta aprobada");
        RecepcionDevolucion r=new RecepcionDevolucion();
        r.setSolicitud(d);
        r.setObservaciones(req.observaciones());
        Set<Long> usados=new HashSet<>();
        for(ItemRecibido recibido:req.items()) {
            if(!usados.add(recibido.itemDevolucionId()))throw new IllegalArgumentException("Item repetido en la recepcion");
            ItemDevolucion original=d.getItems().stream().filter(x->x.getId().equals(recibido.itemDevolucionId()))
            .findFirst().orElseThrow(()->new IllegalArgumentException("Item ajeno a esta devolucion"));
            long anteriores=d.getRecepciones().stream().flatMap(recep->recep.getItems().stream())
            .filter(item->item.getItemDevolucion().getId().equals(original.getId()))
            .mapToLong(ItemRecepcionDevolucion::getCantidadRecibida).sum();
            if(anteriores+recibido.cantidadRecibida()>original.getCantidadSolicitada())
            throw new IllegalArgumentException("La suma de recepciones supera la cantidad solicitada");
            ItemRecepcionDevolucion item=new ItemRecepcionDevolucion();
            item.setRecepcion(r);
            item.setItemDevolucion(original);
            item.setCantidadRecibida(recibido.cantidadRecibida());
            item.setCondicion(recibido.condicion());
            r.getItems().add(item);
        }
        d.getRecepciones().add(r);
        devoluciones.saveAndFlush(d);
        // Registrar reposiciones pendientes y comunicarlas despues del commit local.
        for(ItemRecepcionDevolucion item:r.getItems()) {
            if(item.esAptaParaReposicion()) {
                OperacionReposicion op=new OperacionReposicion();
                op.setOperacionId("POSTSALE-RECEPCION-"+item.getId());
                op.setCantidad(item.getCantidadRecibida());
                item.setReposicion(op);
            }
        }
        devoluciones.saveAndFlush(d);
        List<ReposicionPendiente> reposiciones=new ArrayList<>();
        for(ItemRecepcionDevolucion item:r.getItems()) {
            if(item.getReposicion()!=null)
            reposiciones.add(new ReposicionPendiente(item.getReposicion().getOperacionId(), item.getItemDevolucion().getProductoId(),item.getCantidadRecibida()));
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for(ReposicionPendiente pendiente:reposiciones) confirmarReposicion(pendiente);
            }
        }
        );
        return Map.of("recepcionId",r.getId(),"solicitudId",id,"itemsRecibidos",r.getItems().size(),"reposicionesPendientes",reposiciones.size());
    }

    private void confirmarReposicion(ReposicionPendiente pendiente) {
        try {
            inventory.reponer(pendiente.operacionId(),"Recepcion de devolucion apta",pendiente.productoId(),pendiente.cantidad());
            transaccionNueva.executeWithoutResult(status -> {
                OperacionReposicion op=operaciones.findByOperacionId(pendiente.operacionId())
                .orElseThrow(()->new IllegalStateException("Operacion de reposicion no encontrada"));
                op.confirmar();
                operaciones.save(op);
            }
            );
        }
        catch (RuntimeException ex) {
            log.error("Reposicion {} no confirmada en Inventory; requiere conciliacion",pendiente.operacionId(),ex);
            // No marcar FALLIDA si el remoto pudo haber aplicado la operacion pero fallo la respuesta.
            // PENDIENTE conserva el operacionId idempotente para conciliar sin duplicar stock.
        }
    }

    private record ReposicionPendiente(String operacionId, Long productoId, Integer cantidad) {
    }
}
