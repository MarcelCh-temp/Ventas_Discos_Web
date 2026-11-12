package ms_soporte.ms_soporte.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import ms_soporte.ms_soporte.dto.MensajeRequest;
import ms_soporte.ms_soporte.dto.PageResponse;
import ms_soporte.ms_soporte.dto.TicketDetalleResponse;
import ms_soporte.ms_soporte.dto.TicketResumenResponse;
import ms_soporte.ms_soporte.model.EstadoTicket;
import ms_soporte.ms_soporte.security.AuthUser;
import ms_soporte.ms_soporte.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Lote 1: gestión de solicitudes por el ADMINISTRADOR (HU-63 a HU-67). Rutas del ERS 12.11.
 * Cada método primero comprueba el rol (requireAdmin -> 403) y luego delega en TicketService.
 */
@RestController
@RequestMapping("/api/v1/admin/support/requests")
@SecurityRequirement(name = "bearerAuth")
public class AdminTicketController {

    @Autowired
    private TicketService ticketService;

    // HU-63, HU-64: q = número de ticket o asunto; todos los filtros son opcionales y combinables
    @GetMapping
    public ResponseEntity<PageResponse<TicketResumenResponse>> listar(
            @Parameter(hidden = true) AuthUser user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoTicket estado,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        user.requireAdmin();
        return ResponseEntity.ok(ticketService.listarParaAdministrador(q, estado, categoria, desde, hasta, pagina, tamano));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDetalleResponse> obtener(@Parameter(hidden = true) AuthUser user,
                                                         @PathVariable Long id) {
        user.requireAdmin();
        return ResponseEntity.ok(ticketService.obtenerParaAdministrador(id));
    }

    // HU-65
    @PostMapping("/{id}/messages")
    public ResponseEntity<TicketDetalleResponse> responder(@Parameter(hidden = true) AuthUser user,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody MensajeRequest mensaje) {
        user.requireAdmin();
        return new ResponseEntity<>(ticketService.responderComoAdministrador(id, user.userId(), mensaje.contenido()),
                HttpStatus.CREATED);
    }

    // HU-66 (cambios intermedios: ABIERTA -> EN_PROCESO)
    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketDetalleResponse> actualizarEstado(@Parameter(hidden = true) AuthUser user,
                                                                  @PathVariable Long id,
                                                                  @RequestParam EstadoTicket estado) {
        user.requireAdmin();
        // DOC-01: el responsable es el admin del token, nunca un id enviado por el frontend
        return ResponseEntity.ok(ticketService.actualizarEstado(id, estado, user.userId()));
    }

    // HU-67
    @PostMapping("/{id}/close")
    public ResponseEntity<TicketDetalleResponse> cerrar(@Parameter(hidden = true) AuthUser user,
                                                        @PathVariable Long id) {
        user.requireAdmin();
        return ResponseEntity.ok(ticketService.cerrar(id, user.userId()));
    }
}
