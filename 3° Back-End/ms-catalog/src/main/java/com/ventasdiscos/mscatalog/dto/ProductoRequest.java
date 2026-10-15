package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.model.Categoria;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * HU-08/HU-09: datos comerciales que el Admin puede ingresar/editar.
 * NO trae id (RN-05), ni estado (se retira con HU-10), ni stock (RN-06: es de Inventory).
 */
public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150, message = "Máximo 150 caracteres") String nombre,
        @Size(max = 2000, message = "Máximo 2000 caracteres") String descripcion,
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "El precio debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "Precio inválido (máximo 2 decimales)") BigDecimal precioBase,
        @NotNull(message = "La categoría es obligatoria") Categoria categoria,
        @NotBlank(message = "El género es obligatorio") @Size(max = 60) String genero,
        @NotBlank(message = "El formato es obligatorio") @Size(max = 60) String formato,
        @Size(max = 60) String plataforma,
        @Size(max = 500)
        @Pattern(regexp = "^https?://\\S+$", message = "La imagen debe ser una URL http(s) válida") String imagenUrl
) {
}
