package ms_ordenes.ms_ordenes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ms_ordenes.ms_ordenes.dto.AgregarItemRequest;
import ms_ordenes.ms_ordenes.dto.CambiarCantidadRequest;
import ms_ordenes.ms_ordenes.dto.CarritoResponse;
import ms_ordenes.ms_ordenes.security.AuthUser;
import ms_ordenes.ms_ordenes.service.CarritoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lote 3 · HU-18 a HU-21 (ERS 12: /api/v1/cart). El carrito es siempre el del usuario del token. */
@Tag(name = "Carrito")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/cart")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @Operation(summary = "HU-19: ver mi carrito con precios, subtotales y total calculados por el backend")
    @GetMapping
    public ResponseEntity<CarritoResponse> obtener(@Parameter(hidden = true) AuthUser user) {
        return ResponseEntity.ok(carritoService.obtener(user.userId()));
    }

    @Operation(summary = "HU-18: agregar un producto (activo y con stock)")
    @PostMapping("/items")
    public ResponseEntity<CarritoResponse> agregar(@Parameter(hidden = true) AuthUser user,
                                                   @Valid @RequestBody AgregarItemRequest req) {
        return ResponseEntity.ok(carritoService.agregar(user.userId(), req));
    }

    @Operation(summary = "HU-20: cambiar la cantidad (entero >= 1, sin superar el stock)")
    @PatchMapping("/items/{productId}")
    public ResponseEntity<CarritoResponse> cambiarCantidad(@Parameter(hidden = true) AuthUser user,
                                                           @PathVariable Long productId,
                                                           @Valid @RequestBody CambiarCantidadRequest req) {
        return ResponseEntity.ok(carritoService.cambiarCantidad(user.userId(), productId, req.cantidadEntera()));
    }

    @Operation(summary = "HU-21: quitar un producto del carrito")
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CarritoResponse> quitar(@Parameter(hidden = true) AuthUser user, @PathVariable Long productId) {
        return ResponseEntity.ok(carritoService.quitar(user.userId(), productId));
    }

    @Operation(summary = "RF-COM-02/04: vaciar el carrito (el frontend pide confirmación antes)")
    @DeleteMapping
    public ResponseEntity<CarritoResponse> vaciar(@Parameter(hidden = true) AuthUser user) {
        return ResponseEntity.ok(carritoService.vaciar(user.userId()));
    }
}
