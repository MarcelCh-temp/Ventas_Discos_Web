package ms_soporte.ms_soporte.controller;

import ms_soporte.ms_soporte.model.EstadoTicket;
import ms_soporte.ms_soporte.model.Ticket;
import ms_soporte.ms_soporte.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/soporte/tickets")
@CrossOrigin(origins = "*")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping
    public ResponseEntity<Ticket> crearTicket(@Valid @RequestBody Ticket ticket) {
        Ticket nuevoTicket = ticketService.crearTicket(ticket);
        return new ResponseEntity<>(nuevoTicket, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> listarTodos() {
        return ResponseEntity.ok(ticketService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.obtenerTicketPorId(id));
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Ticket>> obtenerPorUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(ticketService.obtenerTicketsPorUsuario(idUsuario));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Ticket> actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoTicket estado) {
        Ticket ticketActualizado = ticketService.actualizarEstado(id, estado);
        return ResponseEntity.ok(ticketActualizado);
    }
}