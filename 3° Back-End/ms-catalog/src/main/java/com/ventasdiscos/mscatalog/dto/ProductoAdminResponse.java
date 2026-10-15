package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.DisponibilidadCatalogo;
import com.ventasdiscos.mscatalog.model.EstadoComercial;

import java.math.BigDecimal;

/**
 * HU-11: vista administrativa. Muestra por separado estado comercial y disponibilidad.
 * stockActual es null si Inventory no respondió (no se inventa el dato).
 */
public record ProductoAdminResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precioBase,
        Categoria categoria,
        String genero,
        String formato,
        String plataforma,
        String imagenUrl,
        EstadoComercial estado,
        Integer stockActual,
        DisponibilidadCatalogo disponibilidad
) {
}
