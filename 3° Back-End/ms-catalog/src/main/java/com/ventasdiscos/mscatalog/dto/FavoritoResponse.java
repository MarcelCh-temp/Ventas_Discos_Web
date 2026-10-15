package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.DisponibilidadCatalogo;
import com.ventasdiscos.mscatalog.model.EstadoComercial;

import java.math.BigDecimal;

/** HU-58: favorito con el estado ACTUAL del producto (RN-29: agotados/retirados siguen visibles). */
public record FavoritoResponse(
        Long productId,
        String nombre,
        String imagenUrl,
        BigDecimal precioBase,
        EstadoComercial estadoComercial,
        DisponibilidadCatalogo disponibilidad,
        boolean comprable,
        String mensaje
) {
}
