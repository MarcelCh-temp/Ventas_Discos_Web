package ms_ordenes.ms_ordenes.service;

import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.Orden;
import ms_ordenes.ms_ordenes.repository.OrdenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrdenService {

    @Autowired
    private OrdenRepository ordenRepository;

    public Orden crearOrden(Orden orden) {
        orden.setId(null); // Evitar inyección de ID
        orden.setEstado(EstadoOrden.PENDIENTE);
        orden.setFechaCreacion(LocalDateTime.now());
        return ordenRepository.save(orden);
    }

    public List<Orden> obtenerTodas() {
        return ordenRepository.findAll();
    }

    public Orden obtenerOrdenPorId(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + id));
    }

    public List<Orden> obtenerOrdenesPorUsuario(Long idUsuario) {
        return ordenRepository.findByIdUsuario(idUsuario);
    }

    public Orden actualizarEstado(Long id, EstadoOrden nuevoEstado) {
        Orden orden = obtenerOrdenPorId(id);
        if (!orden.getEstado().esTransicionValida(nuevoEstado)) {
            throw new IllegalArgumentException("Transición no permitida de " + orden.getEstado() + " a " + nuevoEstado);
        }
        orden.setEstado(nuevoEstado);
        return ordenRepository.save(orden);
    }
}