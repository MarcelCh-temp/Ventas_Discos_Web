package com.ventasdiscos.msinventory.dto;

import com.ventasdiscos.msinventory.model.Disponibilidad;
import com.ventasdiscos.msinventory.model.Inventario;

/** HU-38: existencias de un producto con su disponibilidad derivada (RN-09). */
public record InventarioResponse(
        Long productId,
        int stockActual,
        int umbralCritico,
        Disponibilidad disponibilidad
) {
    public static InventarioResponse from(Inventario i) {
        return new InventarioResponse(i.getProductId(), i.getStockActual(), i.getUmbralCritico(), i.getDisponibilidad());
    }
}
