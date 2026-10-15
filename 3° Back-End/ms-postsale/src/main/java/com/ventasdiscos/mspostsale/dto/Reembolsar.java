package com.ventasdiscos.mspostsale.dto;

import com.ventasdiscos.mspostsale.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record Reembolsar(
@NotNull
@Positive
Long usuarioId, @NotNull TipoOrigenReembolso tipoOrigen, @NotNull @Positive Long solicitudOrigenId
) {
}
