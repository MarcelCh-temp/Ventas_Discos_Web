package com.ventasdiscos.msinventory.dto;

import com.ventasdiscos.msinventory.model.MovimientoStock;
import com.ventasdiscos.msinventory.model.TipoMovimiento;

import java.time.LocalDateTime;

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
