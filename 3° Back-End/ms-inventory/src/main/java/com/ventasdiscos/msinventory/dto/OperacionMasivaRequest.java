package com.ventasdiscos.msinventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** HU-44: lote completo. Debe haber al menos un producto seleccionado. */
public record OperacionMasivaRequest(
        @NotEmpty(message = "Debes seleccionar al menos un producto")
        @Size(max = 500, message = "Máximo 500 operaciones por lote")
        List<@Valid OperacionMasivaItem> operaciones
) {
}
