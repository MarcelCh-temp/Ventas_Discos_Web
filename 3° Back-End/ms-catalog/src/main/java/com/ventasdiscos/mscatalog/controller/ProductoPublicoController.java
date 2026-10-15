package com.ventasdiscos.mscatalog.controller;

import com.ventasdiscos.mscatalog.dto.FiltroProducto;
import com.ventasdiscos.mscatalog.dto.PageResponse;
import com.ventasdiscos.mscatalog.dto.ProductoPublicoResponse;
import com.ventasdiscos.mscatalog.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exploración pública del catálogo (EP-03). No requiere sesión. */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductoPublicoController {

    private final ProductoService service;

    @Operation(summary = "HU-13..16 Catálogo público: categoría, búsqueda y filtros combinables")
    @GetMapping
    public ResponseEntity<PageResponse<ProductoPublicoResponse>> listar(
            @ParameterObject FiltroProducto filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listarPublico(filtro, page, size));
    }

    @Operation(summary = "HU-17 Detalle de producto (410 si fue retirado)")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoPublicoResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPublico(id));
    }
}
