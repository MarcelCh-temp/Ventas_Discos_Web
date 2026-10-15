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
public class PostSaleService {
    private final CancelacionRepository cancelaciones;
    private final DevolucionRepository devoluciones;
    private final PostSaleSupport support;
    private final CancelacionService cancelacionService;
    private final DevolucionService devolucionService;
    private final ReembolsoService reembolsoService;
    public PostSaleService(CancelacionRepository cancelaciones,
    DevolucionRepository devoluciones,
    PostSaleSupport support,
    CancelacionService cancelacionService,
    DevolucionService devolucionService,
    ReembolsoService reembolsoService) {
        this.cancelaciones = cancelaciones;
        this.devoluciones = devoluciones;
        this.support = support;
        this.cancelacionService = cancelacionService;
        this.devolucionService = devolucionService;
        this.reembolsoService = reembolsoService;
    }

    public Map<String,Object> solicitarCancelacion(Cancelar request) {
        return cancelacionService.solicitarCancelacion(request);
    }

    public Map<String,Object> solicitarDevolucion(Devolver request) {
        return devolucionService.solicitarDevolucion(request);
    }

    public Map<String,Object> resolverCancelacion(Long id, Resolver request) {
        return cancelacionService.resolverCancelacion(id, request);
    }

    public Map<String,Object> resolverDevolucion(Long id, Resolver request) {
        return devolucionService.resolverDevolucion(id, request);
    }

    public Map<String,Object> recibir(Long id, Recibir request) {
        return devolucionService.recibir(id, request);
    }

    public Map<String,Object> registrarReembolso(Reembolsar request) {
        return reembolsoService.registrarReembolso(request);
    }

    private Map<String,Object> detalleCancelacion(SolicitudCancelacion c) {
        Map<String,Object> m=support.resumen(c,"CANCELACION",c.getEstado().name());
        m.put("motivoSolicitud",c.getMotivoSolicitud());
        m.put("motivoResolucion",c.getMotivoResolucion());
        m.put("fechaResolucion",c.getFechaResolucion());
        return m;
    }

    private Map<String,Object> detalleDevolucion(SolicitudDevolucion d) {
        Map<String,Object> m=support.resumen(d,"DEVOLUCION",d.getEstado().name());
        m.put("motivoSolicitud",d.getMotivoSolicitud());
        m.put("motivoResolucion",d.getMotivoResolucion());
        m.put("fechaResolucion",d.getFechaResolucion());
        m.put("items",d.getItems().stream().map(item -> {
            Map<String,Object> linea=new LinkedHashMap<>();
            linea.put("id",item.getId());
            linea.put("productoId",item.getProductoId());
            linea.put("cantidadSolicitada",item.getCantidadSolicitada());
            return linea;
        }
        ).toList());
        m.put("recepciones",d.getRecepciones().stream().map(r -> {
            Map<String,Object> recepcion=new LinkedHashMap<>();
            recepcion.put("id",r.getId());
            recepcion.put("fechaRecepcion",r.getFechaRecepcion());
            recepcion.put("observaciones",r.getObservaciones());
            recepcion.put("items",r.getItems().stream().map(item -> {
                Map<String,Object> linea=new LinkedHashMap<>();
                linea.put("itemDevolucionId",item.getItemDevolucion().getId());
                linea.put("cantidadRecibida",item.getCantidadRecibida());
                linea.put("condicion",item.getCondicion());
                return linea;
            }
            ).toList());
            return recepcion;
        }
        ).toList());
        return m;
    }
    @Transactional(readOnly=true)

    public List<Map<String,Object>> misSolicitudes(Long usuarioId) {
        List<Map<String,Object>> resultado=new ArrayList<>();
        cancelaciones.findByUsuarioIdOrderByFechaSolicitudDesc(usuarioId).forEach(c->resultado.add(support.resumen(c,"CANCELACION",c.getEstado().name())));
        devoluciones.findByUsuarioIdOrderByFechaSolicitudDesc(usuarioId).forEach(d->resultado.add(support.resumen(d,"DEVOLUCION",d.getEstado().name())));
        resultado.sort((a,b)->b.get("fechaSolicitud").toString().compareTo(a.get("fechaSolicitud").toString()));
        return resultado;
    }
    @Transactional(readOnly=true)

    public Map<String,Object> detalle(Long id, Long usuarioId, String tipo) {
        if(tipo==null || tipo.isBlank()) {
            boolean hayCancelacion=cancelaciones.findById(id).filter(c->c.perteneceA(usuarioId)).isPresent();
            boolean hayDevolucion=devoluciones.findById(id).filter(d->d.perteneceA(usuarioId)).isPresent();
            if(hayCancelacion && hayDevolucion)
            throw new IllegalStateException("ID compartido por cancelacion y devolucion; indique tipo");
            if(hayCancelacion) tipo="CANCELACION";
            else if(hayDevolucion) tipo="DEVOLUCION";
            else throw new NoSuchElementException("Solicitud no encontrada");
        }
        if("CANCELACION".equalsIgnoreCase(tipo)) {
            SolicitudCancelacion c=cancelaciones.findById(id)
            .filter(s->s.perteneceA(usuarioId))
            .orElseThrow(()->new NoSuchElementException("Cancelacion no encontrada"));
            return detalleCancelacion(c);
        }
        if("DEVOLUCION".equalsIgnoreCase(tipo)) {
            SolicitudDevolucion d=devoluciones.findById(id)
            .filter(s->s.perteneceA(usuarioId))
            .orElseThrow(()->new NoSuchElementException("Devolucion no encontrada"));
            return detalleDevolucion(d);
        }
        throw new IllegalArgumentException("Tipo de solicitud no valido");
    }
    @Transactional(readOnly=true)

    public List<Map<String,Object>> todas(Long pedidoId, Long usuarioId, String tipo, String estado) {
        List<Map<String,Object>> salida=new ArrayList<>();
        if(tipo==null || tipo.isBlank() || "CANCELACION".equalsIgnoreCase(tipo))
        cancelaciones.findAll().forEach(c->salida.add(detalleCancelacion(c)));
        if(tipo==null || tipo.isBlank() || "DEVOLUCION".equalsIgnoreCase(tipo))
        devoluciones.findAll().forEach(d->salida.add(detalleDevolucion(d)));
        if(tipo!=null && !tipo.isBlank() && !Set.of("CANCELACION","DEVOLUCION").contains(tipo.toUpperCase(Locale.ROOT)))
        throw new IllegalArgumentException("Tipo de solicitud no valido");
        return salida.stream()
        .filter(m->pedidoId==null || pedidoId.equals(m.get("pedidoId")))
        .filter(m->usuarioId==null || usuarioId.equals(m.get("usuarioId")))
        .filter(m->estado==null || estado.isBlank() || estado.equalsIgnoreCase((String)m.get("estado")))
        .sorted((a,b)->b.get("fechaSolicitud").toString().compareTo(a.get("fechaSolicitud").toString()))
        .toList();
    }
}
