package com.ventasdiscos.mscatalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Contrato PROVISIONAL Orders -> Catalog (HU-18, HU-24, HU-25): cotizar un carrito. */
public record PricingRequest(
        @NotEmpty @Size(max = 100) List<@Valid Item> items
) {
    public record Item(@NotNull Long productId, @NotNull @Positive Integer cantidad) {
    }
}
