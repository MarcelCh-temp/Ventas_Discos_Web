package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.Disponibilidad;

/**
 * Lote 2: resultado de una línea de POST /api/v1/inventory/availability/query.
 * haySuficiente = stock >= cantidad (UML 03: Inventario.hayStockSuficiente).
 * permiteCompra = producto ACTIVO y haySuficiente (RN-07, RN-08, RN-13). No expone el stock exacto.
 */
public record DisponibilidadItemResponse(
        Long productId,
        int cantidadSolicitada,
        Disponibilidad disponibilidad,
        boolean haySuficiente,
        boolean permiteCompra
) {
}
