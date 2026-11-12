package ms_ordenes.ms_ordenes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ms_ordenes.ms_ordenes.dto.ConfirmacionCompraResponse;
import ms_ordenes.ms_ordenes.dto.ConfirmarCompraRequest;
import ms_ordenes.ms_ordenes.dto.MetodoEntregaResponse;
import ms_ordenes.ms_ordenes.dto.OpcionesPagoResponse;
import ms_ordenes.ms_ordenes.dto.ResumenCompraResponse;
import ms_ordenes.ms_ordenes.dto.ValidarCheckoutRequest;
import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.security.AuthUser;
import ms_ordenes.ms_ordenes.service.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lote 3 · HU-22 a HU-25 (ERS 12: /api/v1/checkout/validate y /confirm). Las listas de métodos son públicas;
 * validar y confirmar requieren token. Pago: SIMULACIÓN ACADÉMICA (sin pasarela real ni datos de tarjeta).
 */
@Tag(name = "Checkout")
@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @Operation(summary = "HU-22: métodos de entrega disponibles y sus campos obligatorios")
    @GetMapping("/delivery-methods")
    public ResponseEntity<List<MetodoEntregaResponse>> metodosEntrega() {
        return ResponseEntity.ok(checkoutService.metodosEntrega());
    }

    @Operation(summary = "HU-25: métodos de pago permitidos (simulación académica)")
    @GetMapping("/payment-methods")
    public ResponseEntity<OpcionesPagoResponse> metodosPago() {
        return ResponseEntity.ok(checkoutService.opcionesPago());
    }

    @Operation(summary = "HU-24: resumen recalculado antes de pagar")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/validate")
    public ResponseEntity<ResumenCompraResponse> validar(@Parameter(hidden = true) AuthUser user,
                                                         @Valid @RequestBody ValidarCheckoutRequest req) {
        return ResponseEntity.ok(checkoutService.validar(user.userId(), req));
    }

    @Operation(summary = "HU-25: pago simulado y confirmación (201 confirmado, 202 pendiente de stock)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/confirm")
    public ResponseEntity<ConfirmacionCompraResponse> confirmar(@Parameter(hidden = true) AuthUser user,
                                                                @Valid @RequestBody ConfirmarCompraRequest req) {
        ConfirmacionCompraResponse r = checkoutService.confirmar(user.userId(), req);
        HttpStatus status = r.pedido().estado() == EstadoOrden.CONFIRMADO ? HttpStatus.CREATED : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(r);
    }
}
