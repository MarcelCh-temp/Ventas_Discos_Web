package ms_ordenes.ms_ordenes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ms_ordenes.ms_ordenes.dto.PageResponse;
import ms_ordenes.ms_ordenes.dto.PedidoDetalleResponse;
import ms_ordenes.ms_ordenes.dto.PedidoResumenResponse;
import ms_ordenes.ms_ordenes.dto.SeguimientoResponse;
import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.security.AuthUser;
import ms_ordenes.ms_ordenes.service.OrdenService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Mis pedidos (EP-06, HU-34 a HU-37). Clase original conservada.
 * Lote 3: se reemplazaron las rutas antiguas /api/ordenes (sin token: cualquiera podía crear con un total
 * inventado, listar todo o cambiar estados) por las del ERS 12. El dueño sale SIEMPRE del token; un pedido ajeno
 * responde 404 (T-09). Crear un pedido ahora es POST /api/v1/checkout/confirm.
 */
@Tag(name = "Mis pedidos")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/orders/me")
public class OrdenController {

    private final OrdenService ordenService;

    public OrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @Operation(summary = "HU-34 / HU-37: mis pedidos, con búsqueda por número y filtros por estado y fechas")
    @GetMapping
    public ResponseEntity<PageResponse<PedidoResumenResponse>> listar(
            @Parameter(hidden = true) AuthUser user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoOrden estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ordenService.listarMios(user.userId(), q, estado, desde, hasta, page, size));
    }

    @Operation(summary = "HU-35: detalle de un pedido propio (precios históricos)")
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDetalleResponse> detalle(@Parameter(hidden = true) AuthUser user, @PathVariable Long id) {
        return ResponseEntity.ok(ordenService.detalleMio(user.userId(), id));
    }

    @Operation(summary = "HU-36 / RN-40: seguimiento por id (15) o número de pedido (PED-000015)")
    @GetMapping("/{referencia}/tracking")
    public ResponseEntity<SeguimientoResponse> seguimiento(@Parameter(hidden = true) AuthUser user,
                                                           @PathVariable String referencia) {
        return ResponseEntity.ok(ordenService.seguimiento(user.userId(), referencia));
    }
}
