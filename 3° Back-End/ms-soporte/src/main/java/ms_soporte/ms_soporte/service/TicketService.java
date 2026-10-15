package ms_soporte.ms_soporte.service;

import ms_soporte.ms_soporte.exception.RecursoNoEncontradoException;
import ms_soporte.ms_soporte.model.EstadoTicket;
import ms_soporte.ms_soporte.model.Ticket;
import ms_soporte.ms_soporte.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    public Ticket crearTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    public List<Ticket> obtenerTodos() {
        return ticketRepository.findAll();
    }

    public List<Ticket> obtenerTicketsPorUsuario(Long idUsuario) {
        return ticketRepository.findByIdUsuario(idUsuario);
    }

    public Ticket obtenerTicketPorId(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado con ID: " + id));
    }

    public Ticket actualizarEstado(Long id, EstadoTicket nuevoEstado) {
        Ticket ticket = obtenerTicketPorId(id);
        ticket.setEstado(nuevoEstado);
        return ticketRepository.save(ticket);
    }
}