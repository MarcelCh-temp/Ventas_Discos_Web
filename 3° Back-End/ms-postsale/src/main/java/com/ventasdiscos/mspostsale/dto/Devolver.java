package com.ventasdiscos.mspostsale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Clase original. HU-69 / RF-POS-02. Lote 4: sin usuarioId (sale del token). Al menos un producto (RN-41). */
public record Devolver(
        @NotNull(message = "El pedido es obligatorio") @Positive(message = "El pedido debe ser positivo") Long pedidoId,
        @NotBlank(message = "El motivo es obligatorio") @Size(max = 255, message = "Máximo 255 caracteres") String motivoSolicitud,
        @NotEmpty(message = "Indica al menos un producto a devolver") @Size(max = 100, message = "Máximo 100 productos")
        List<@Valid @NotNull(message = "Producto inválido") ItemSolicitud> items
) {
}
