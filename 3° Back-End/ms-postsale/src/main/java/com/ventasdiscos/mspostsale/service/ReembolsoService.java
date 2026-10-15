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
public class ReembolsoService {
    private final CancelacionRepository cancelaciones;
    private final DevolucionRepository devoluciones;
    private final ReembolsoRepository reembolsos;
    private final PostSaleSupport support;
    public ReembolsoService(CancelacionRepository cancelaciones,
    DevolucionRepository devoluciones,
    ReembolsoRepository reembolsos,
    PostSaleSupport support) {
        this.cancelaciones = cancelaciones;
        this.devoluciones = devoluciones;
        this.reembolsos = reembolsos;
        this.support = support;
    }
    @Transactional

    public Map<String,Object> registrarReembolso(Reembolsar req) {
        Long pedidoId;
        if(req.tipoOrigen()==TipoOrigenReembolso.CANCELACION) {
            var c=cancelaciones.findById(req.solicitudOrigenId()).orElseThrow(()->new NoSuchElementException("Cancelacion inexistente"));
            if(!c.perteneceA(req.usuarioId())||c.getEstado()!=EstadoCancelacion.APROBADA)throw new IllegalArgumentException("Cancelacion no aprobada para este usuario");
            pedidoId=c.getPedidoId();
        }
        else {
            var d=devoluciones.findById(req.solicitudOrigenId()).orElseThrow(()->new NoSuchElementException("Devolucion inexistente"));
            if(!d.perteneceA(req.usuarioId())||!d.estaAprobada())throw new IllegalArgumentException("Devolucion no aprobada para este usuario");
            pedidoId=d.getPedidoId();
        }
        if(reembolsos.existsByTipoOrigenAndSolicitudOrigenId(req.tipoOrigen(),req.solicitudOrigenId()))throw new IllegalStateException("Ya existe un reembolso para este origen");
        SolicitudReembolso s=new SolicitudReembolso();
        s.setTipoOrigen(req.tipoOrigen());
        s.setSolicitudOrigenId(req.solicitudOrigenId());
        s.setUsuarioId(req.usuarioId());
        s.setPedidoId(pedidoId);
        s=reembolsos.saveAndFlush(s);
        support.notificarTrasCommit("reembolso-"+s.getId()+"-registrado",s.getUsuarioId(),"REEMBOLSO_REGISTRADO","Reembolso registrado","Se registro la solicitud de reembolso",s.getId());
        return Map.of("id",s.getId(),"estado",s.getEstado().name(),"tipoOrigen",s.getTipoOrigen().name(),"solicitudOrigenId",s.getSolicitudOrigenId());
    }
}
