package ms_soporte.ms_soporte.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import ms_soporte.ms_soporte.dto.CrearTicketRequest;
import ms_soporte.ms_soporte.dto.MensajeRequest;
import ms_soporte.ms_soporte.dto.TicketDetalleResponse;
import ms_soporte.ms_soporte.dto.TicketResumenResponse;
import ms_soporte.ms_soporte.security.AuthUser;
import ms_soporte.ms_soporte.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Solicitudes de soporte del CLIENTE (HU-59 a HU-62). Rutas del ERS 12.11.
 * Lote 1: antes era /api/soporte/tickets, recibía la entidad Ticket con idUsuario y cualquiera
 * podía ver o cambiar tickets ajenos. Ahora el usuario sale del token y las reglas están en TicketService.
 */
@RestController
@RequestMapping("/api/v1/support/requests")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    // HU-59
    @PostMapping
    public ResponseEntity<TicketDetalleResponse> crearTicket(@Parameter(hidden = true) AuthUser user,
                                                             @Valid @RequestBody CrearTicketRequest datos) {
        return new ResponseEntity<>(ticketService.crearTicket(datos, user.userId()), HttpStatus.CREATED);
    }

    // HU-60
    @GetMapping("/me")
    public ResponseEntity<List<TicketResumenResponse>> listarPropios(@Parameter(hidden = true) AuthUser user) {
        return ResponseEntity.ok(ticketService.listarPropios(user.userId()));
    }

    // HU-61
    @GetMapping("/{id}")
    public ResponseEntity<TicketDetalleResponse> obtenerPropio(@Parameter(hidden = true) AuthUser user,
                                                               @PathVariable Long id) {
        return ResponseEntity.ok(ticketService.obtenerPropio(id, user.userId()));
    }

    // HU-62
    @PostMapping("/{id}/messages")
    public ResponseEntity<TicketDetalleResponse> responder(@Parameter(hidden = true) AuthUser user,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody MensajeRequest mensaje) {
        return new ResponseEntity<>(ticketService.responderComoCliente(id, user.userId(), mensaje.contenido()),
                HttpStatus.CREATED);
    }
}
