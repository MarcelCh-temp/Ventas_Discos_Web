package com.ventasdiscos.msinventory.controller;

import com.ventasdiscos.msinventory.dto.AjusteRequest;
import com.ventasdiscos.msinventory.dto.InventarioResponse;
import com.ventasdiscos.msinventory.dto.MovimientoRequest;
import com.ventasdiscos.msinventory.dto.MovimientoResponse;
import com.ventasdiscos.msinventory.dto.OperacionMasivaRequest;
import com.ventasdiscos.msinventory.dto.PageResponse;
import com.ventasdiscos.msinventory.dto.UmbralRequest;
import com.ventasdiscos.msinventory.model.Disponibilidad;
import com.ventasdiscos.msinventory.model.TipoMovimiento;
import com.ventasdiscos.msinventory.security.AuthUser;
import com.ventasdiscos.msinventory.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Endpoints administrativos de inventario (EP-07). Todos exigen rol Administrador. */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class InventarioController {

    private final InventarioService service;

    @Operation(summary = "HU-38/39 Listar y filtrar existencias (paginado)")
    @GetMapping
    public ResponseEntity<PageResponse<InventarioResponse>> listar(
            @Parameter(hidden = true) AuthUser user,
            @RequestParam(required = false) Long productId,
            @Parameter(description = "Ids de producto (ej. los de una categoría obtenidos desde Catalog)")
            @RequestParam(required = false) List<Long> productIds,
            @RequestParam(required = false) Disponibilidad disponibilidad,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        user.requireAdmin();
        return ResponseEntity.ok(service.listar(productId, productIds, disponibilidad, page, size));
    }

    @Operation(summary = "HU-38 Existencias de un producto")
    @GetMapping("/{productId}")
    public ResponseEntity<InventarioResponse> obtener(@Parameter(hidden = true) AuthUser user,
                                                      @PathVariable Long productId) {
        user.requireAdmin();
        return ResponseEntity.ok(service.obtener(productId));
    }

    @Operation(summary = "HU-41 Registrar entrada o salida de stock")
    @PostMapping("/{productId}/movements")
    public ResponseEntity<MovimientoResponse> movimiento(@Parameter(hidden = true) AuthUser user,
                                                         @PathVariable Long productId,
                                                         @Valid @RequestBody MovimientoRequest req) {
        user.requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarMovimiento(productId, req, user.userId()));
    }

    @Operation(summary = "HU-40 Ajuste por diferencia de inventario físico")
    @PostMapping("/{productId}/adjustments")
    public ResponseEntity<MovimientoResponse> ajuste(@Parameter(hidden = true) AuthUser user,
                                                     @PathVariable Long productId,
                                                     @Valid @RequestBody AjusteRequest req) {
        user.requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajustar(productId, req, user.userId()));
    }

    @Operation(summary = "HU-42 Configurar umbral de stock crítico")
    @PutMapping("/{productId}/critical-level")
    public ResponseEntity<InventarioResponse> umbral(@Parameter(hidden = true) AuthUser user,
                                                     @PathVariable Long productId,
                                                     @Valid @RequestBody UmbralRequest req) {
        user.requireAdmin();
        return ResponseEntity.ok(service.configurarUmbral(productId, req));
    }

    @Operation(summary = "HU-43 Historial de movimientos (filtrable)")
    @GetMapping("/movements")
    public ResponseEntity<PageResponse<MovimientoResponse>> historial(
            @Parameter(hidden = true) AuthUser user,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        user.requireAdmin();
        return ResponseEntity.ok(service.historial(productId, tipo, desde, hasta, page, size));
    }

    @Operation(summary = "HU-44 Operación masiva (todo o nada)")
    @PostMapping("/bulk")
    public ResponseEntity<List<MovimientoResponse>> masivo(@Parameter(hidden = true) AuthUser user,
                                                           @Valid @RequestBody OperacionMasivaRequest req) {
        user.requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.aplicarMasivo(req, user.userId()));
    }
}
