package ms_ordenes.ms_ordenes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ms_ordenes.ms_ordenes.dto.CambioEstadoRequest;
import ms_ordenes.ms_ordenes.dto.CorreccionEntregaRequest;
import ms_ordenes.ms_ordenes.dto.DespachoRequest;
import ms_ordenes.ms_ordenes.dto.PageResponse;
import ms_ordenes.ms_ordenes.dto.PedidoDetalleResponse;
import ms_ordenes.ms_ordenes.dto.PedidoResumenResponse;
import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.security.AuthUser;
import ms_ordenes.ms_ordenes.service.OrdenService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Lote 3 · EP-05 (HU-26 a HU-33). Solo ADMINISTRADOR; el responsable del historial sale del token. */
@Tag(name = "Pedidos (Administrador)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrdenController {

    private final OrdenService ordenService;

    public AdminOrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @Operation(summary = "HU-26 / HU-27: pedidos paginados; q = número, id o nombre del receptor")
    @GetMapping
    public ResponseEntity<PageResponse<PedidoResumenResponse>> listar(
            @Parameter(hidden = true) AuthUser user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoOrden estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        user.requireAdmin();
        return ResponseEntity.ok(ordenService.listarAdmin(q, estado, desde, hasta, userId, page, size));
    }

    @Operation(summary = "HU-28 / HU-32: detalle completo con historial")
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDetalleResponse> detalle(@Parameter(hidden = true) AuthUser user, @PathVariable Long id) {
        user.requireAdmin();
        return ResponseEntity.ok(ordenService.detalleAdmin(id));
    }

    @Operation(summary = "HU-29 / HU-31: cambiar estado (CANCELADO exige motivo y repone stock una vez)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<PedidoDetalleResponse> cambiarEstado(@Parameter(hidden = true) AuthUser user,
                                                               @PathVariable Long id,
                                                               @Valid @RequestBody CambioEstadoRequest req) {
        user.requireAdmin();
        return ResponseEntity.ok(ordenService.cambiarEstadoAdmin(id, req, user.userId()));
    }

    @Operation(summary = "HU-30: corregir datos de entrega permitidos (antes de DESPACHADO)")
    @PatchMapping("/{id}/delivery-data")
    public ResponseEntity<PedidoDetalleResponse> corregirEntrega(@Parameter(hidden = true) AuthUser user,
                                                                 @PathVariable Long id,
                                                                 @Valid @RequestBody CorreccionEntregaRequest req) {
        user.requireAdmin();
        return ResponseEntity.ok(ordenService.corregirEntrega(id, req, user.userId()));
    }

    @Operation(summary = "HU-33: registrar o actualizar el despacho (PREPARANDO pasa a DESPACHADO)")
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<PedidoDetalleResponse> despacho(@Parameter(hidden = true) AuthUser user,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody DespachoRequest req) {
        user.requireAdmin();
        return ResponseEntity.ok(ordenService.registrarDespacho(id, req, user.userId()));
    }
}
