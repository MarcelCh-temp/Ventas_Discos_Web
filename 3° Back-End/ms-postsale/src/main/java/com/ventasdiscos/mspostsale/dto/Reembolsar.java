package com.ventasdiscos.mspostsale.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Clase original. HU-77 / RF-POS-10. Lote 4: sin usuarioId (sale del token) y sin tipoOrigen: el UML 04 no lo tiene;
 * el tipo de la solicitud de origen (CANCELACION o DEVOLUCION) ya está guardado en ella. motivoSolicitud es opcional
 * (RF-POS-10 no exige motivo).
 */
public record Reembolsar(
        @NotNull(message = "La solicitud de origen es obligatoria") @Positive Long solicitudOrigenId,
        @Size(max = 255, message = "Máximo 255 caracteres") String motivoSolicitud
) {
}
