package com.ventasdiscos.mscatalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// Lote 2: trasladado desde ms-inventory (ERS 11.4) sin cambios de contrato.
/**
 * Contrato Orders/PostSale -> Catalog (antes -> Inventory; misma ruta y JSON) (HU-25, HU-31, HU-74, HU-76).
 * operacionId = clave de idempotencia (ej. "ORDERS-PEDIDO-123-VENTA"): reintentar no descuenta dos veces.
 */
public record OperacionInternaRequest(
        @NotBlank @Size(max = 100) String operacionId,
        @NotBlank @Size(max = 255) String motivo,
        @NotEmpty @Size(max = 200) List<@Valid OperacionInternaItem> items
) {
}
