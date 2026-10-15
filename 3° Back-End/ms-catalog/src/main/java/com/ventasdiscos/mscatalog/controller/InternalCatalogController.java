package com.ventasdiscos.mscatalog.controller;

import com.ventasdiscos.mscatalog.dto.PricingRequest;
import com.ventasdiscos.mscatalog.dto.PricingResponse;
import com.ventasdiscos.mscatalog.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Contrato MS -> MS para Orders. El Gateway NO debe enrutar /internal/**. */
@Tag(name = "Interno (MS a MS)")
@RestController
@RequestMapping("/internal/catalog")
@RequiredArgsConstructor
public class InternalCatalogController {

    private final PricingService pricingService;

    @Operation(summary = "Orders: precio comercial + promociones vigentes de un carrito")
    @PostMapping("/pricing")
    public ResponseEntity<PricingResponse> cotizar(@Valid @RequestBody PricingRequest req) {
        return ResponseEntity.ok(pricingService.cotizar(req));
    }
}
