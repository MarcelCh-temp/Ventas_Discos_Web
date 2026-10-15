package com.ventasdiscos.mspostsale.controller;

import com.ventasdiscos.mspostsale.dto.*;
import com.ventasdiscos.mspostsale.service.PostSaleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/post-sale")
@Validated
public class PostSaleController {
    private final PostSaleService service;

    public PostSaleController(PostSaleService service) {
        this.service = service;
    }
    @PostMapping("/cancellations")

    public ResponseEntity<?> cancelar(@Valid @RequestBody Cancelar request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.solicitarCancelacion(request));
    }
    @PostMapping("/returns")

    public ResponseEntity<?> devolver(@Valid @RequestBody Devolver request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.solicitarDevolucion(request));
    }
    @GetMapping("/requests/me")

    public List<Map<String, Object>> mias(@RequestParam @Positive Long usuarioId) {
        return service.misSolicitudes(usuarioId);
    }
    @GetMapping("/requests/{id}")
    public Map<String, Object> detalle(@PathVariable @Positive Long id,
    @RequestParam
    @Positive
    Long usuarioId,
    @RequestParam(required = false)
    String tipo) {
        return service.detalle(id, usuarioId, tipo);
    }
    @PostMapping("/refunds")

    public ResponseEntity<?> reembolso(@Valid @RequestBody Reembolsar request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarReembolso(request));
    }
}
