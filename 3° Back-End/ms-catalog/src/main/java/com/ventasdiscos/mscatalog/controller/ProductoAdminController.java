package com.ventasdiscos.mscatalog.controller;

import com.ventasdiscos.mscatalog.dto.FiltroProducto;
import com.ventasdiscos.mscatalog.dto.PageResponse;
import com.ventasdiscos.mscatalog.dto.ProductoAdminResponse;
import com.ventasdiscos.mscatalog.dto.ProductoRequest;
import com.ventasdiscos.mscatalog.dto.RetiroRequest;
import com.ventasdiscos.mscatalog.dto.RetiroResponse;
import com.ventasdiscos.mscatalog.security.AuthUser;
import com.ventasdiscos.mscatalog.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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

/** Administración del catálogo (EP-02). Todo exige rol Administrador. */
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProductoAdminController {

    private final ProductoService service;

    @Operation(summary = "HU-11/12 Catálogo administrativo con búsqueda y filtros")
    @GetMapping
    public ResponseEntity<PageResponse<ProductoAdminResponse>> listar(
            @Parameter(hidden = true) AuthUser user,
            @ParameterObject FiltroProducto filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        user.requireAdmin();
        return ResponseEntity.ok(service.listarAdmin(filtro, page, size));
    }

    @Operation(summary = "HU-11 Detalle administrativo (incluye retirados)")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoAdminResponse> detalle(@Parameter(hidden = true) AuthUser user,
                                                         @PathVariable Long id) {
        user.requireAdmin();
        return ResponseEntity.ok(service.obtenerAdmin(id));
    }

    @Operation(summary = "HU-08 Registrar producto (id generado, stock en Inventory)")
    @PostMapping
    public ResponseEntity<ProductoAdminResponse> crear(@Parameter(hidden = true) AuthUser user,
                                                       @Valid @RequestBody ProductoRequest req) {
        user.requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @Operation(summary = "HU-09 Editar datos comerciales")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoAdminResponse> editar(@Parameter(hidden = true) AuthUser user,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody ProductoRequest req) {
        user.requireAdmin();
        return ResponseEntity.ok(service.actualizar(id, req));
    }

    @Operation(summary = "HU-10 Retirar uno o varios productos")
    @PostMapping("/retire")
    public ResponseEntity<RetiroResponse> retirar(@Parameter(hidden = true) AuthUser user,
                                                  @Valid @RequestBody RetiroRequest req) {
        user.requireAdmin();
        return ResponseEntity.ok(service.retirar(req));
    }
}
