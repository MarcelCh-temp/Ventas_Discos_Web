package com.ventasdiscos.mspostsale.dto;

import com.ventasdiscos.mspostsale.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ItemSolicitud(
@NotNull
@Positive
Long productoId, @NotNull @Positive Integer cantidadSolicitada
) {
}
