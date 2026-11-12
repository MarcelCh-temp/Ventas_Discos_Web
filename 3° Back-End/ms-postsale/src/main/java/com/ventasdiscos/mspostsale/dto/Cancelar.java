package com.ventasdiscos.mspostsale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Clase original. HU-68 / RF-POS-01. Lote 4: se quitó usuarioId: el Cliente sale SIEMPRE del token (antes cualquiera
 * podía escribir otro usuarioId). Si el frontend lo sigue enviando, se ignora.
 */
public record Cancelar(
        @NotNull(message = "El pedido es obligatorio") @Positive(message = "El pedido debe ser positivo") Long pedidoId,
        @NotBlank(message = "El motivo es obligatorio") @Size(max = 255, message = "Máximo 255 caracteres") String motivoSolicitud
) {
}
