package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.DisponibilidadCatalogo;
import com.ventasdiscos.mscatalog.model.EstadoComercial;

import java.math.BigDecimal;

/**
 * HU-12, HU-14, HU-15, HU-16: criterios combinables de búsqueda (query params).
 * q busca por nombre o por id exacto. "estado" solo se respeta en la vista administrativa.
 */
public record FiltroProducto(
        String q,
        Categoria categoria,
        String genero,
        String formato,
        String plataforma,
        EstadoComercial estado,
        BigDecimal precioMin,
        BigDecimal precioMax,
        DisponibilidadCatalogo disponibilidad
) {
}
