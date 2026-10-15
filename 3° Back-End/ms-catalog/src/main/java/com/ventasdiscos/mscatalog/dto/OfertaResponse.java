package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.TipoDescuento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** HU-52: promoción vigente con sus productos y el precio promocional de cada uno. */
public record OfertaResponse(
        Long promocionId,
        String nombre,
        TipoDescuento tipoDescuento,
        BigDecimal valor,
        BigDecimal montoMinimoCompra,
        LocalDate fechaFin,
        List<ProductoEnOferta> productos
) {
    public record ProductoEnOferta(Long id, String nombre, String imagenUrl, BigDecimal precioBase,
                                   BigDecimal precioOferta) {
    }
}
