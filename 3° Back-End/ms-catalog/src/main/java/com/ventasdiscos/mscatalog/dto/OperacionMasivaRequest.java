package com.ventasdiscos.mscatalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// Lote 2: trasladado desde ms-inventory (ERS 11.4) sin cambios de contrato.
/** HU-44: lote completo. Debe haber al menos un producto seleccionado. */
public record OperacionMasivaRequest(
        @NotEmpty(message = "Debes seleccionar al menos un producto")
        @Size(max = 500, message = "Máximo 500 operaciones por lote")
        List<@Valid OperacionMasivaItem> operaciones
) {
}
