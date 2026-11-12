package com.ventasdiscos.mspostsale.controller;

import com.ventasdiscos.mspostsale.dto.CancelacionResueltaResponse;
import com.ventasdiscos.mspostsale.dto.PageResponse;
import com.ventasdiscos.mspostsale.dto.Recibir;
import com.ventasdiscos.mspostsale.dto.Resolver;
import com.ventasdiscos.mspostsale.dto.SolicitudDetalleResponse;
import com.ventasdiscos.mspostsale.model.EstadoSolicitud;
import com.ventasdiscos.mspostsale.model.TipoSolicitud;
import com.ventasdiscos.mspostsale.security.AuthUser;
import com.ventasdiscos.mspostsale.service.PostSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Clase original. Postventa del Administrador (EP-13: HU-72 a HU-76). Rutas definitivas del ERS 12.11.
 * Lote 4: antes /admin/post-sale/** sin token (cualquiera podía resolver). Ahora exige rol ADMINISTRADOR (403 si
 * no) y el responsable (usuarioResolucionId / usuarioRecepcionId, DOC-09) sale SIEMPRE del token, nunca del cuerpo.
 */
@Tag(name = "Postventa del Administrador")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/post-sale")
public class AdminPostSaleController {

    private final PostSaleService service;

    public AdminPostSaleController(PostSaleService service) {
        this.service = service;
    }

    @Operation(summary = "HU-72 / HU-73: listar, buscar (id, pedido, Cliente) y filtrar (tipo, estado, período)")
    @GetMapping("/requests")
    public ResponseEntity<PageResponse<SolicitudDetalleResponse>> todas(
            @Parameter(hidden = true) AuthUser user,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long pedidoId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) TipoSolicitud tipo,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        user.requireAdmin();
        return ResponseEntity.ok(service.listarAdmin(id, pedidoId, usuarioId, tipo, estado, desde, hasta, page, size));
    }

    @Operation(summary = "HU-74: aprobar o rechazar una cancelación (revalida el pedido en Orders)")
    @PostMapping("/cancellations/{id}/resolve")
    public ResponseEntity<CancelacionResueltaResponse> cancelar(@Parameter(hidden = true) AuthUser user,
                                                                @PathVariable Long id,
                                                                @Valid @RequestBody Resolver request) {
        user.requireAdmin();
        return ResponseEntity.ok(service.resolverCancelacion(id, request, user.userId()));
    }

    @Operation(summary = "HU-75: aprobar o rechazar una devolución (revalida el pedido en Orders)")
    @PostMapping("/returns/{id}/resolve")
    public ResponseEntity<SolicitudDetalleResponse> devolver(@Parameter(hidden = true) AuthUser user,
                                                             @PathVariable Long id,
                                                             @Valid @RequestBody Resolver request) {
        user.requireAdmin();
        return ResponseEntity.ok(service.resolverDevolucion(id, request, user.userId()));
    }

    @Operation(summary = "HU-76: registrar la recepción de una devolución APROBADA (repone en Catalog solo lo apto)")
    @PostMapping("/returns/{id}/reception")
    public ResponseEntity<SolicitudDetalleResponse> recibir(@Parameter(hidden = true) AuthUser user,
                                                            @PathVariable Long id,
                                                            @Valid @RequestBody Recibir request) {
        user.requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recibir(id, request, user.userId()));
    }
}
