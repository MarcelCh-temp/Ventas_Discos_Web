package com.ventasdiscos.mscatalog.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** HU-10: uno o varios productos a retirar (la confirmación previa la pide el frontend). */
public record RetiroRequest(
        @NotEmpty(message = "Debes seleccionar al menos un producto")
        @Size(max = 200, message = "Máximo 200 productos por operación")
        List<@NotNull Long> ids
) {
}
