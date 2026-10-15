package com.ventasdiscos.msinventory.controller;

import com.ventasdiscos.msinventory.dto.InventarioResponse;
import com.ventasdiscos.msinventory.dto.OperacionInternaRequest;
import com.ventasdiscos.msinventory.dto.OperacionInternaResponse;
import com.ventasdiscos.msinventory.model.Disponibilidad;
import com.ventasdiscos.msinventory.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contratos MS -> MS (Catalog, Orders, PostSale). NO se exponen por el API Gateway:
 * el Gateway no debe enrutar /internal/** (ver README). Por eso no piden JWT de usuario.
 */
@Tag(name = "Interno (MS a MS)")
@RestController
@RequestMapping("/internal/inventory")
@RequiredArgsConstructor
public class InternalInventarioController {

    private final InventarioService service;

    @Operation(summary = "Catalog: disponibilidad de varios productos en una llamada")
    @GetMapping("/availability")
    public ResponseEntity<List<InventarioResponse>> disponibilidad(@RequestParam List<Long> productIds) {
        return ResponseEntity.ok(service.disponibilidadLote(productIds));
    }

    @Operation(summary = "Catalog: ids de productos con una disponibilidad (para filtros)")
    @GetMapping("/product-ids")
    public ResponseEntity<List<Long>> productIds(@RequestParam Disponibilidad disponibilidad) {
        return ResponseEntity.ok(service.productIdsPorDisponibilidad(disponibilidad));
    }

    @Operation(summary = "Catalog: registrar producto nuevo con stock 0 (idempotente)")
    @PostMapping("/products/{productId}")
    public ResponseEntity<InventarioResponse> registrarProducto(@PathVariable Long productId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarProducto(productId));
    }

    @Operation(summary = "Orders/PostSale: descontar o reponer stock (idempotente por operacionId)")
    @PostMapping("/operations")
    public ResponseEntity<OperacionInternaResponse> operacion(@Valid @RequestBody OperacionInternaRequest req) {
        OperacionInternaResponse resp = service.aplicarOperacionInterna(req);
        return ResponseEntity.status(resp.yaAplicada() ? HttpStatus.OK : HttpStatus.CREATED).body(resp);
    }
}
