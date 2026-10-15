package ms_ordenes.ms_ordenes.controller;

import jakarta.validation.Valid;
import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.Orden;
import ms_ordenes.ms_ordenes.service.OrdenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
@CrossOrigin(origins = "*")
public class OrdenController {

    @Autowired
    private OrdenService ordenService;

    @PostMapping
    public ResponseEntity<Orden> crearOrden(@Valid @RequestBody Orden orden) {
        Orden nuevaOrden = ordenService.crearOrden(orden);
        return new ResponseEntity<>(nuevaOrden, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Orden>> listarOrdenes() {
        return new ResponseEntity<>(ordenService.obtenerTodas(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Orden> obtenerOrdenPorId(@PathVariable Long id) {
        return new ResponseEntity<>(ordenService.obtenerOrdenPorId(id), HttpStatus.OK);
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Orden>> obtenerOrdenesPorUsuario(@PathVariable Long idUsuario) {
        return new ResponseEntity<>(ordenService.obtenerOrdenesPorUsuario(idUsuario), HttpStatus.OK);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Orden> actualizarEstado(@PathVariable Long id, @RequestParam EstadoOrden estado) {
        return new ResponseEntity<>(ordenService.actualizarEstado(id, estado), HttpStatus.OK);
    }
}