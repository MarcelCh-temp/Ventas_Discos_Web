package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.MovimientoStock;
import com.ventasdiscos.mscatalog.model.TipoMovimiento;

import java.time.LocalDateTime;

// Lote 2: trasladado desde ms-inventory (ERS 11.4) sin cambios de contrato.
/** HU-43: una fila del historial de inventario. */
public record MovimientoResponse(
        Long id,
        Long productId,
        TipoMovimiento tipo,
        int cantidad,
        int stockAnterior,
        int stockResultante,
        LocalDateTime fechaHora,
        String motivo,
        Long usuarioId,
        String operacionId
) {
    public static MovimientoResponse from(MovimientoStock m) {
        return new MovimientoResponse(m.getId(), m.getInventario().getProductId(), m.getTipo(), m.getCantidad(),
                m.getStockAnterior(), m.getStockResultante(), m.getFechaHora(), m.getMotivo(),
                m.getUsuarioId(), m.getOperacionId());
    }
}
