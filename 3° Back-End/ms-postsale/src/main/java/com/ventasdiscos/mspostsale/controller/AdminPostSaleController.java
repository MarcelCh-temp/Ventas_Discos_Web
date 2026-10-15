package com.ventasdiscos.mspostsale.controller;

import com.ventasdiscos.mspostsale.dto.*;
import com.ventasdiscos.mspostsale.service.PostSaleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/post-sale")
@Validated
public class AdminPostSaleController {
    private final PostSaleService service;

    public AdminPostSaleController(PostSaleService service) {
        this.service = service;
    }
    @GetMapping("/requests")
    public List<Map<String, Object>> todas(@RequestParam(required = false)
    @Positive
    Long pedidoId,
    @RequestParam(required = false)
    @Positive
    Long usuarioId,
    @RequestParam(required = false)
    String tipo,
    @RequestParam(required = false)
    String estado) {
        return service.todas(pedidoId, usuarioId, tipo, estado);
    }
    @PostMapping("/cancellations/{id}/resolve")
    public Map<String, Object> cancelar(@PathVariable @Positive Long id,
    @Valid
    @RequestBody
    Resolver request) {
        return service.resolverCancelacion(id, request);
    }
    @PostMapping("/returns/{id}/resolve")
    public Map<String, Object> devolver(@PathVariable @Positive Long id,
    @Valid
    @RequestBody
    Resolver request) {
        return service.resolverDevolucion(id, request);
    }
    @PostMapping("/returns/{id}/reception")
    public Map<String, Object> recibir(@PathVariable @Positive Long id,
    @Valid
    @RequestBody
    Recibir request) {
        return service.recibir(id, request);
    }
}
