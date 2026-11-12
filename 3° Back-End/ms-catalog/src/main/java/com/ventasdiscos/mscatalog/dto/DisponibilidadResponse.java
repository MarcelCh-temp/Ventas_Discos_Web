package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.Disponibilidad;

/**
 * Lote 2 (ERS 12, GET /api/v1/inventory/{productId}/availability): disponibilidad PÚBLICA de un producto.
 * No expone la cantidad exacta de stock (eso es solo para el Administrador, HU-38).
 * permiteCompra = producto ACTIVO y con stock (RN-07, RN-08).
 */
public record DisponibilidadResponse(
        Long productId,
        Disponibilidad disponibilidad,
        boolean permiteCompra
) {
}
