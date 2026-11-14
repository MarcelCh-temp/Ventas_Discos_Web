package com.ventasdiscos.mspostsale.dto;

import com.ventasdiscos.mspostsale.model.EstadoSolicitud;
import com.ventasdiscos.mspostsale.model.SolicitudPostVenta;
import com.ventasdiscos.mspostsale.model.TipoSolicitud;

import java.time.LocalDateTime;

/**
 * Lote 4 · HU-70 / RF-POS-03: fila del listado del Cliente (identificador, tipo, pedido o proceso asociado, fecha y
 * estado). solicitudOrigenId solo en REEMBOLSO. No expone entidades JPA.
 */
public record SolicitudResumenResponse(Long id, TipoSolicitud tipo, EstadoSolicitud estado, Long pedidoId,
                                       Long solicitudOrigenId, LocalDateTime fechaSolicitud,
                                       LocalDateTime fechaResolucion) {

    // Ejecuta from como parte de las responsabilidades de esta clase.
    public static SolicitudResumenResponse from(SolicitudPostVenta s) {
        return new SolicitudResumenResponse(s.getId(), s.getTipo(), s.getEstado(), s.getPedidoId(),
                s.getSolicitudOrigenId(), s.getFechaSolicitud(), s.getFechaResolucion());
    }
}
