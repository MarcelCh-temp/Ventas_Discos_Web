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
public class CancelacionService {
    private final CancelacionRepository cancelaciones;
    private final PostSaleSupport support;

    public CancelacionService(CancelacionRepository cancelaciones, PostSaleSupport support) {
        this.cancelaciones = cancelaciones;
        this.support = support;
    }
    @Transactional

    public Map<String,Object> solicitarCancelacion(Cancelar req) {
        support.validarCancelable(support.estado(support.verificarPedido(req.pedidoId(),req.usuarioId())));
        if (cancelaciones.existsByPedidoIdAndEstado(req.pedidoId(),EstadoCancelacion.PENDIENTE))
        throw new IllegalStateException("Ya existe una cancelacion pendiente");
        SolicitudCancelacion s=new SolicitudCancelacion();
        s.setPedidoId(req.pedidoId());
        s.setUsuarioId(req.usuarioId());
        s.setMotivoSolicitud(req.motivoSolicitud());
        s=cancelaciones.saveAndFlush(s);
        support.notificarTrasCommit("cancelacion-"+s.getId()+"-creada",s.getUsuarioId(),"CANCELACION_CREADA", "Cancelacion solicitada", "Se registro tu solicitud",s.getId());
        return support.resumen(s,"CANCELACION",s.getEstado().name());
    }
    @Transactional

    public Map<String,Object> resolverCancelacion(Long id, Resolver req) {
        SolicitudCancelacion c=cancelaciones.findById(id).orElseThrow(()->new NoSuchElementException("Cancelacion no encontrada"));
        if(!c.estaPendiente())throw new IllegalStateException("Cancelacion ya resuelta");
        if(req.aprobar()) {
            support.validarCancelable(support.estado(support.verificarPedido(c.getPedidoId(),c.getUsuarioId())));
            // RN-26 requiere confirmar si se desconto inventario y reponer las lineas reales.
            // El contrato actual de Orders no ofrece esos datos; evitar cancelaciones inconsistentes.
            throw new DependenciaIncompletaException("Orders no expone las lineas ni el descuento de stock necesarios para aprobar con RN-26");
        }
        c.rechazar(req.motivo());
        support.notificarTrasCommit("cancelacion-"+id+"-resuelta",c.getUsuarioId(),"CANCELACION_RESUELTA","Cancelacion resuelta","La cancelacion fue rechazada",id);
        return support.resumen(c,"CANCELACION",c.getEstado().name());
    }
}
