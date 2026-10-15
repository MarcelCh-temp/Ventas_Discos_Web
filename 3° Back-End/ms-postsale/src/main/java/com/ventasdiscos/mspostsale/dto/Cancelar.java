package com.ventasdiscos.mspostsale.dto;

import com.ventasdiscos.mspostsale.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record Cancelar(
@NotNull
@Positive
Long pedidoId, @NotNull @Positive Long usuarioId, @NotBlank String motivoSolicitud
) {
}
