package ms_ordenes.ms_ordenes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ms_ordenes.ms_ordenes.dto.CancelacionInternaRequest;
import ms_ordenes.ms_ordenes.dto.CancelacionInternaResponse;
import ms_ordenes.ms_ordenes.dto.ExisteResponse;
import ms_ordenes.ms_ordenes.dto.PedidoInternoResponse;
import ms_ordenes.ms_ordenes.service.OrdenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ms_ordenes.ms_ordenes.security.Servicios;
import ms_ordenes.ms_ordenes.security.ServiciosPermitidos;

/**
 * Lote 3 · Contrato INTERNO para MS-PostSale (lo adoptará en el Lote 4). Lote 5: + consulta de pedidos activos para
 * MS-Accounts (HU-78). No es para el frontend.
 * Lote 6 · DOC-05: protegida con JWT de servicio (ServicioInternoInterceptor). Solo los servicios de
 * @ServiciosPermitidos pueden llamarla; sin token o con un JWT de usuario → 401; otro servicio → 403. El API
 * Gateway NO la expone. Se mantiene server.address=127.0.0.1 como medida complementaria. Sin X-Internal-Key.
 */
@Tag(name = "Interno (MS a MS)")
@RestController
@RequestMapping("/api/v1/internal/orders")
public class InternalOrdenController {

    private final OrdenService ordenService;

    public InternalOrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @Operation(summary = "PostSale: dueño, estado y productos comprados de un pedido")
    // Cierre (RF-SOP-01): + Support, que confirma que el pedido asociado a un ticket es del Cliente
    @ServiciosPermitidos({Servicios.POSTSALE, Servicios.SUPPORT})
    @GetMapping("/{id}")
    public ResponseEntity<PedidoInternoResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.consultaInterna(id));
    }

    /** Lote 5 · HU-78 (ERS 13.8): Accounts verifica pedidos activos antes de eliminar una cuenta. Solo lectura. */
    @Operation(summary = "Accounts: ¿el usuario tiene pedidos PENDIENTE, CONFIRMADO, PREPARANDO o DESPACHADO?")
    @ServiciosPermitidos(Servicios.ACCOUNTS)
    @GetMapping("/users/{userId}/active-exists")
    public ResponseEntity<ExisteResponse> pedidosActivos(@PathVariable Long userId) {
        return ResponseEntity.ok(ordenService.tienePedidosActivos(userId));
    }

    @Operation(summary = "PostSale: aplicar una cancelación aprobada (idempotente; repone stock una vez)")
    @ServiciosPermitidos(Servicios.POSTSALE)
    @PostMapping("/{id}/cancel")
    public ResponseEntity<CancelacionInternaResponse> cancelar(@PathVariable Long id,
                                                               @Valid @RequestBody CancelacionInternaRequest req) {
        return ResponseEntity.ok(ordenService.cancelarDesdePostVenta(id, req));
    }
}
