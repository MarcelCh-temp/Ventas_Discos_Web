package com.ventasdiscos.mspostsale.controller;

import com.ventasdiscos.mspostsale.dto.Cancelar;
import com.ventasdiscos.mspostsale.dto.Devolver;
import com.ventasdiscos.mspostsale.dto.PageResponse;
import com.ventasdiscos.mspostsale.dto.Reembolsar;
import com.ventasdiscos.mspostsale.dto.SolicitudDetalleResponse;
import com.ventasdiscos.mspostsale.dto.SolicitudResumenResponse;
import com.ventasdiscos.mspostsale.model.EstadoSolicitud;
import com.ventasdiscos.mspostsale.model.TipoSolicitud;
import com.ventasdiscos.mspostsale.security.AuthUser;
import com.ventasdiscos.mspostsale.service.PostSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Clase original. Postventa del Cliente (EP-13: HU-68 a HU-71 y HU-77). Rutas definitivas del ERS 12.11.
 * Lote 4: antes /post-sale/** sin token y con ?usuarioId= (cualquiera podía ver o crear a nombre de otro). Ahora el
 * Cliente sale SIEMPRE del token (AuthUser) y una solicitud o pedido ajeno responde 404. Solo autenticación aquí;
 * las reglas están en los Services (CSR).
 */
@Tag(name = "Postventa del Cliente")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/post-sale")
public class PostSaleController {

    // Dependencia inyectada para acceder a service sin crearla manualmente.
    private final PostSaleService service;

    // Endpoint HTTP; el controlador delega la operación y devuelve la respuesta correspondiente.
    public PostSaleController(PostSaleService service) {
        this.service = service;
    }

    @Operation(summary = "HU-68: solicitar la cancelación de un pedido propio PENDIENTE, CONFIRMADO o PREPARANDO")
    @PostMapping("/cancellations")
    public ResponseEntity<SolicitudDetalleResponse> cancelar(@Parameter(hidden = true) AuthUser user,
                                                             @Valid @RequestBody Cancelar request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.solicitarCancelacion(user.userId(), request));
    }

    @Operation(summary = "HU-69: solicitar la devolución de productos de un pedido propio ENTREGADO")
    @PostMapping("/returns")
    // POST: recibe una solicitud y ejecuta una operación. Método devolver (devolver).
    public ResponseEntity<SolicitudDetalleResponse> devolver(@Parameter(hidden = true) AuthUser user,
                                                             @Valid @RequestBody Devolver request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.solicitarDevolucion(user.userId(), request));
    }

    @Operation(summary = "HU-70: mis solicitudes (cancelación, devolución y reembolso), más recientes primero")
    @GetMapping("/requests/me")
    // GET: consulta información sin modificar registros. Método mias (mias).
    public ResponseEntity<PageResponse<SolicitudResumenResponse>> mias(@Parameter(hidden = true) AuthUser user,
                                                                       @RequestParam(required = false) TipoSolicitud tipo,
                                                                       @RequestParam(required = false) EstadoSolicitud estado,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.misSolicitudes(user.userId(), tipo, estado, page, size));
    }

    @Operation(summary = "HU-71: detalle de una solicitud propia (404 si no existe o no es tuya)")
    @GetMapping("/requests/{id}")
    // GET: consulta información sin modificar registros. Método detalle (detalle).
    public ResponseEntity<SolicitudDetalleResponse> detalle(@Parameter(hidden = true) AuthUser user,
                                                            @PathVariable Long id) {
        return ResponseEntity.ok(service.detalleMio(user.userId(), id));
    }

    /** Ruta del ERS 12.11 (definitiva). La lista de nivel de recurso del ERS 11.7 la nombraba /post-sale/refunds. */
    @Operation(summary = "HU-77: solicitar el reembolso de una cancelación o devolución APROBADA (no procesa dinero)")
    @PostMapping("/refund-requests")
    public ResponseEntity<SolicitudDetalleResponse> reembolso(@Parameter(hidden = true) AuthUser user,
                                                              @Valid @RequestBody Reembolsar request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarReembolso(user.userId(), request));
    }
}
