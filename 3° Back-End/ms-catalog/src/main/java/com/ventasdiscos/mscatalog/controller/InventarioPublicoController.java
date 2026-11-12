package com.ventasdiscos.mscatalog.controller;

import com.ventasdiscos.mscatalog.dto.ConsultaDisponibilidadRequest;
import com.ventasdiscos.mscatalog.dto.DisponibilidadItemResponse;
import com.ventasdiscos.mscatalog.dto.DisponibilidadResponse;
import com.ventasdiscos.mscatalog.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lote 2 (ERS 12): disponibilidad pública de stock, igual que el catálogo público (sin token).
 * Solo informa Disponible / Stock crítico / Agotado y si se puede comprar; nunca la cantidad exacta.
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventarioPublicoController {

    private final InventarioService service;

    @Operation(summary = "Disponibilidad de un producto (410 si fue retirado)")
    @GetMapping("/{productId}/availability")
    public ResponseEntity<DisponibilidadResponse> disponibilidad(@PathVariable Long productId) {
        return ResponseEntity.ok(service.disponibilidadPublica(productId));
    }

    @Operation(summary = "¿Hay stock suficiente para estas cantidades? (carrito / revalidación; no descuenta)")
    @PostMapping("/availability/query")
    public ResponseEntity<List<DisponibilidadItemResponse>> consultar(@Valid @RequestBody ConsultaDisponibilidadRequest req) {
        return ResponseEntity.ok(service.consultarDisponibilidad(req));
    }
}
