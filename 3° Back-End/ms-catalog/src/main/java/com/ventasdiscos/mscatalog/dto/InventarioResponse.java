package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.Disponibilidad;
import com.ventasdiscos.mscatalog.model.Inventario;

/**
 * HU-38 / RF-INV-01: existencias de un producto con su disponibilidad derivada (RN-09).
 * Lote 2: trasladado desde ms-inventory. Se agregan nombre y categoria del producto
 * (RF-INV-01: "producto/identificador"), posibles ahora que Producto e Inventario viven en el mismo MS.
 */
public record InventarioResponse(
        Long productId,
        String nombre,
        Categoria categoria,
        int stockActual,
        int umbralCritico,
        Disponibilidad disponibilidad
) {
    // Ejecuta from como parte de las responsabilidades de esta clase.
    public static InventarioResponse from(Inventario i) {
        return new InventarioResponse(i.getProductId(), i.getProducto().getNombre(), i.getProducto().getCategoria(),
                i.getStockActual(), i.getUmbralCritico(), i.getDisponibilidad());
    }
}
