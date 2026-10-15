package com.ventasdiscos.mscatalog.controller;

import com.ventasdiscos.mscatalog.dto.PageResponse;
import com.ventasdiscos.mscatalog.dto.PromocionRequest;
import com.ventasdiscos.mscatalog.dto.PromocionResponse;
import com.ventasdiscos.mscatalog.model.EstadoPromocion;
import com.ventasdiscos.mscatalog.security.AuthUser;
import com.ventasdiscos.mscatalog.service.PromocionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Administración de promociones (EP-09). Todo exige rol Administrador. */
@RestController
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PromocionAdminController {

    private final PromocionService service;

    @Operation(summary = "HU-48/49 Listar y filtrar promociones")
    @GetMapping
    public ResponseEntity<PageResponse<PromocionResponse>> listar(
            @Parameter(hidden = true) AuthUser user,
            @RequestParam(required = false) EstadoPromocion estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        user.requireAdmin();
        return ResponseEntity.ok(service.listar(estado, desde, hasta, productId, q, page, size));
    }

    @Operation(summary = "HU-48 Detalle de promoción")
    @GetMapping("/{id}")
    public ResponseEntity<PromocionResponse> detalle(@Parameter(hidden = true) AuthUser user, @PathVariable Long id) {
        user.requireAdmin();
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "HU-47 Crear promoción")
    @PostMapping
    public ResponseEntity<PromocionResponse> crear(@Parameter(hidden = true) AuthUser user,
                                                   @Valid @RequestBody PromocionRequest req) {
        user.requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @Operation(summary = "HU-50 Editar promoción (solo Programada/Activa)")
    @PutMapping("/{id}")
    public ResponseEntity<PromocionResponse> editar(@Parameter(hidden = true) AuthUser user,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody PromocionRequest req) {
        user.requireAdmin();
        return ResponseEntity.ok(service.actualizar(id, req));
    }

    @Operation(summary = "HU-51 Desactivar anticipadamente")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<PromocionResponse> desactivar(@Parameter(hidden = true) AuthUser user,
                                                        @PathVariable Long id) {
        user.requireAdmin();
        return ResponseEntity.ok(service.desactivar(id));
    }
}
