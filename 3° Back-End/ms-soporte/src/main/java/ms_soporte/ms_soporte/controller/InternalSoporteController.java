package ms_soporte.ms_soporte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ms_soporte.ms_soporte.dto.ExisteResponse;
import ms_soporte.ms_soporte.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ms_soporte.ms_soporte.security.Servicios;
import ms_soporte.ms_soporte.security.ServiciosPermitidos;

/**
 * Lote 5 · Contrato INTERNO para MS-Accounts (HU-78, RF-CTA-08, ERS 13.8). Integración necesaria de HU-78; no cambia
 * las funcionalidades de Soporte del Lote 1. Solo lectura: no modifica tickets.
 * Lote 6 · DOC-05: protegida con JWT de servicio (ServicioInternoInterceptor). Solo los servicios de
 * @ServiciosPermitidos pueden llamarla; sin token o con un JWT de usuario → 401; otro servicio → 403. El API
 * Gateway NO la expone. Se mantiene server.address=127.0.0.1 como medida complementaria. Sin X-Internal-Key.
 */
@Tag(name = "Interno (MS a MS)")
@RestController
@RequestMapping("/api/v1/internal/support")
public class InternalSoporteController {

    @Autowired
    private TicketService ticketService;

    @Operation(summary = "Accounts: ¿el usuario tiene tickets ABIERTA o EN_PROCESO?")
    @ServiciosPermitidos(Servicios.ACCOUNTS)
    @GetMapping("/users/{userId}/open-exists")
    public ResponseEntity<ExisteResponse> ticketsAbiertos(@PathVariable Long userId) {
        return ResponseEntity.ok(ticketService.tieneTicketsAbiertos(userId));
    }
}
