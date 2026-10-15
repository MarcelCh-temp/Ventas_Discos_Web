package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.EstadoPromocion;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.model.Promocion;
import com.ventasdiscos.mscatalog.model.TipoDescuento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** HU-48: promoción con estado calculado. */
public record PromocionResponse(
        Long id,
        String nombre,
        TipoDescuento tipoDescuento,
        BigDecimal valor,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        BigDecimal montoMinimoCompra,
        Categoria categoria,
        List<Long> productIds,
        EstadoPromocion estado,
        LocalDateTime fechaDesactivacion
) {
    public static PromocionResponse from(Promocion p, LocalDate hoy) {
        return new PromocionResponse(p.getId(), p.getNombre(), p.getTipoDescuento(), p.getValor(),
                p.getFechaInicio(), p.getFechaFin(), p.getMontoMinimoCompra(), p.getCategoria(),
                p.getProductos().stream().map(Producto::getId).sorted().toList(),
                p.estado(hoy), p.getFechaDesactivacion());
    }
}
