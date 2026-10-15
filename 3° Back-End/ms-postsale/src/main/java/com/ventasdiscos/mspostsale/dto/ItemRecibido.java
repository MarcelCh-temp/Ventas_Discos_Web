package com.ventasdiscos.mspostsale.dto;

import com.ventasdiscos.mspostsale.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ItemRecibido(
@NotNull
@Positive
Long itemDevolucionId, @NotNull @Positive Integer cantidadRecibida, @NotNull CondicionProducto condicion
) {
}
