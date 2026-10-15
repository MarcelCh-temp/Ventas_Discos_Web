package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.TipoDescuento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * HU-47/HU-50. Destino: productIds y/o categoria (grupo). Al menos uno es obligatorio (lo valida el Service).
 * El estado NO viene aquí: se deriva de las fechas (RN-27).
 */
public record PromocionRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        @NotNull(message = "El tipo de descuento es obligatorio") TipoDescuento tipoDescuento,
        @NotNull(message = "El valor es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "El valor debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "Valor inválido (máximo 2 decimales)") BigDecimal valor,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "La fecha de término es obligatoria") LocalDate fechaFin,
        @DecimalMin(value = "0", inclusive = false, message = "La compra mínima debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2) BigDecimal montoMinimoCompra,
        @Size(max = 500) List<Long> productIds,
        Categoria categoria
) {
}
