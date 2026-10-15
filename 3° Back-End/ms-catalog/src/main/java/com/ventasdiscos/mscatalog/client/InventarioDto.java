package com.ventasdiscos.mscatalog.client;

/** Contrato de respuesta de MS-Inventory (/internal/inventory/availability). No es una entidad. */
public record InventarioDto(
        Long productId,
        int stockActual,
        int umbralCritico,
        String disponibilidad
) {
}
