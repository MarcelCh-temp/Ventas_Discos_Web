package com.ventasdiscos.mscatalog.controller;

import com.ventasdiscos.mscatalog.dto.OfertaResponse;
import com.ventasdiscos.mscatalog.service.PromocionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Ofertas vigentes para el Cliente (HU-52). Público. */
@RestController
@RequestMapping("/offers")
@RequiredArgsConstructor
public class OfertaController {

    private final PromocionService service;

    @Operation(summary = "HU-52 Productos con promociones vigentes")
    @GetMapping
    public ResponseEntity<List<OfertaResponse>> ofertas() {
        return ResponseEntity.ok(service.ofertasVigentes());
    }
}
