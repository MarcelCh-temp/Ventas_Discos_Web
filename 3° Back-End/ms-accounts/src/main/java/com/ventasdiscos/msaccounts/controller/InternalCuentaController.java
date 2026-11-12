package com.ventasdiscos.msaccounts.controller;

import com.ventasdiscos.msaccounts.dto.EstadoOperacionResponse;
import com.ventasdiscos.msaccounts.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ventasdiscos.msaccounts.security.Servicios;
import com.ventasdiscos.msaccounts.security.ServiciosPermitidos;

/**
 * Lote 5 · Contrato INTERNO (ERS 13.8; RN-37, RN-44): Orders, Support, PostSale y Catalog consultan si la cuenta puede
 * hacer nuevas operaciones justo antes de crear un pedido, ticket, solicitud o favorito.
 * Cierre (RN-44, RF-CTA-08 CA-06): esos MS y Notifications la consultan en TODA petición de un Cliente.
 * Ruta HTTP definitiva: GET /api/v1/internal/accounts/{userId}/operation-status (WebConfig agrega /api/v1).
 * Lote 6 · DOC-05: protegida con JWT de servicio (ServicioInternoInterceptor). Solo los servicios de
 * @ServiciosPermitidos pueden llamarla; sin token o con un JWT de usuario → 401; otro servicio → 403. El API
 * Gateway NO la expone. Se mantiene server.address=127.0.0.1 como medida complementaria. Sin X-Internal-Key.
 */
@Tag(name = "Interno (MS a MS)")
@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class InternalCuentaController {

    private final CuentaService cuentaService;

    @Operation(summary = "Estado de la cuenta: puedeOperar = true solo si está ACTIVA")
    // Cierre (RN-44): + Notifications, que ahora también confirma la cuenta antes de atender al Cliente
    @ServiciosPermitidos({Servicios.ORDERS, Servicios.SUPPORT, Servicios.POSTSALE, Servicios.CATALOG,
            Servicios.NOTIFICATIONS})
    @GetMapping("/{userId}/operation-status")
    public ResponseEntity<EstadoOperacionResponse> estado(@PathVariable Long userId) {
        return ResponseEntity.ok(cuentaService.estadoOperacion(userId));
    }
}
