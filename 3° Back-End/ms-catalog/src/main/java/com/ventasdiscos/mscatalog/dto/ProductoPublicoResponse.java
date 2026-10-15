package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.DisponibilidadCatalogo;

import java.math.BigDecimal;

/**
 * HU-13..17 y HU-52: lo que ve el Cliente. Sin stock exacto (no se necesita), sí disponibilidad.
 * precioOferta/promocion son null si no hay promoción vigente sin condiciones.
 * comprable = ACTIVO y con stock confirmado (RN-07, RN-08).
 */
public record ProductoPublicoResponse(
        Long id,
        String nombre,
        String descripcion,
        Categoria categoria,
        String genero,
        String formato,
        String plataforma,
        String imagenUrl,
        BigDecimal precioBase,
        BigDecimal precioOferta,
        String promocion,
        DisponibilidadCatalogo disponibilidad,
        boolean comprable
) {
}
