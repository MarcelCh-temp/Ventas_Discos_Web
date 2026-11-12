package com.ventasdiscos.mspostsale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Clase original. HU-76 / RF-POS-09. La fecha de recepción la pone el servidor; el Administrador responsable sale de
 * su token (DOC-09) y no se acepta desde el frontend.
 */
public record Recibir(
        @Size(max = 255, message = "Máximo 255 caracteres") String observaciones,
        @NotEmpty(message = "Indica al menos un producto recibido") @Size(max = 100, message = "Máximo 100 productos")
        List<@Valid @NotNull(message = "Producto inválido") ItemRecibido> items
) {
}
