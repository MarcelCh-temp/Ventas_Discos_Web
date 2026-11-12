package com.ventasdiscos.mspostsale.dto;

import com.ventasdiscos.mspostsale.model.CondicionProducto;
import com.ventasdiscos.mspostsale.model.EstadoReposicion;
import com.ventasdiscos.mspostsale.model.EstadoSolicitud;
import com.ventasdiscos.mspostsale.model.ItemDevolucion;
import com.ventasdiscos.mspostsale.model.ItemRecepcionDevolucion;
import com.ventasdiscos.mspostsale.model.RecepcionDevolucion;
import com.ventasdiscos.mspostsale.model.SolicitudPostVenta;
import com.ventasdiscos.mspostsale.model.TipoSolicitud;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lote 4 · HU-71 / RF-POS-04 (Cliente) y HU-72 a HU-76 (Administrador): detalle de una solicitud.
 * Mismo criterio que el detalle de pedido del Lote 3: los datos internos van {@code null} en la vista del Cliente y
 * solo el Administrador los ve: usuarioResolucionId y usuarioRecepcionId (DOC-09) y el estado de cada reposición en
 * Catalog. usuarioId (dueño) se muestra en ambas vistas: el Cliente solo ve las suyas.
 */
public record SolicitudDetalleResponse(Long id, TipoSolicitud tipo, EstadoSolicitud estado, Long pedidoId,
                                       Long usuarioId, Long solicitudOrigenId, String motivoSolicitud,
                                       String motivoResolucion, LocalDateTime fechaSolicitud,
                                       LocalDateTime fechaResolucion, Long usuarioResolucionId,
                                       List<Producto> items, List<Recepcion> recepciones) {

    /** ItemDevolucion: lo solicitado y lo ya recibido (para saber cuánto falta por recibir). */
    public record Producto(Long id, Long productoId, int cantidadSolicitada, int cantidadRecibida) {
    }

    public record Recepcion(Long id, LocalDateTime fechaRecepcion, String observaciones, Long usuarioRecepcionId,
                            List<Recibido> items) {
    }

    /** operacionId y estadoReposicion: solo Administrador (null si no era apta o en la vista del Cliente). */
    public record Recibido(Long id, Long itemDevolucionId, Long productoId, int cantidadRecibida,
                           CondicionProducto condicion, String operacionId, EstadoReposicion estadoReposicion) {
    }

    public static SolicitudDetalleResponse from(SolicitudPostVenta s, boolean vistaAdmin) {
        List<Producto> productos = s.getItems().stream()
                .map(i -> new Producto(i.getId(), i.getProductoId(), i.getCantidadSolicitada(), s.cantidadRecibida(i)))
                .toList();
        List<Recepcion> recepciones = s.getRecepciones().stream().map(r -> recepcion(r, vistaAdmin)).toList();
        return new SolicitudDetalleResponse(s.getId(), s.getTipo(), s.getEstado(), s.getPedidoId(), s.getUsuarioId(),
                s.getSolicitudOrigenId(), s.getMotivoSolicitud(), s.getMotivoResolucion(), s.getFechaSolicitud(),
                s.getFechaResolucion(), vistaAdmin ? s.getUsuarioResolucionId() : null, productos, recepciones);
    }

    private static Recepcion recepcion(RecepcionDevolucion r, boolean vistaAdmin) {
        return new Recepcion(r.getId(), r.getFechaRecepcion(), r.getObservaciones(),
                vistaAdmin ? r.getUsuarioRecepcionId() : null,
                r.getItems().stream().map(i -> recibido(i, vistaAdmin)).toList());
    }

    private static Recibido recibido(ItemRecepcionDevolucion i, boolean vistaAdmin) {
        ItemDevolucion d = i.getItemDevolucion();
        boolean conReposicion = vistaAdmin && i.getReposicion() != null;
        return new Recibido(i.getId(), d.getId(), d.getProductoId(), i.getCantidadRecibida(), i.getCondicion(),
                conReposicion ? i.getReposicion().getOperacionId() : null,
                conReposicion ? i.getReposicion().getEstado() : null);
    }
}
