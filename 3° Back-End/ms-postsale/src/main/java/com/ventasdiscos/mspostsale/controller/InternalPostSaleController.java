package com.ventasdiscos.mspostsale.controller;

import com.ventasdiscos.mspostsale.dto.ExisteResponse;
import com.ventasdiscos.mspostsale.service.PostSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ventasdiscos.mspostsale.security.Servicios;
import com.ventasdiscos.mspostsale.security.ServiciosPermitidos;

/**
 * Lote 5 · Contrato INTERNO para MS-Accounts (HU-78, RF-CTA-08, ERS 13.8). Integración necesaria de HU-78; no cambia
 * las funcionalidades de PostSale del Lote 4. Solo lectura.
 * Lote 6 · DOC-05: protegida con JWT de servicio (ServicioInternoInterceptor). Solo los servicios de
 * @ServiciosPermitidos pueden llamarla; sin token o con un JWT de usuario → 401; otro servicio → 403. El API
 * Gateway NO la expone. Se mantiene server.address=127.0.0.1 como medida complementaria. Sin mecanismo de autenticacion anterior.
 */
@Tag(name = "Interno (MS a MS)")
@RestController
@RequestMapping("/api/v1/internal/post-sale")
public class InternalPostSaleController {

    // Dependencia inyectada para acceder a service sin crearla manualmente.
    private final PostSaleService service;

    // Endpoint HTTP; el controlador delega la operación y devuelve la respuesta correspondiente.
    public InternalPostSaleController(PostSaleService service) {
        this.service = service;
    }

    @Operation(summary = "Accounts: ¿el usuario tiene solicitudes de postventa PENDIENTE?")
    @ServiciosPermitidos(Servicios.ACCOUNTS)
    @GetMapping("/users/{userId}/pending-exists")
    // GET: consulta información; el controlador delega la operación y devuelve la respuesta correspondiente.
    public ResponseEntity<ExisteResponse> pendientes(@PathVariable Long userId) {
        return ResponseEntity.ok(service.tieneSolicitudesPendientes(userId));
    }
}
