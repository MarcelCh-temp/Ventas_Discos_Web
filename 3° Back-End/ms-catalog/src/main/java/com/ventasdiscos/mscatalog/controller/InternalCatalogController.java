package com.ventasdiscos.mscatalog.controller;

import com.ventasdiscos.mscatalog.dto.DepuracionResponse;
import com.ventasdiscos.mscatalog.dto.PricingRequest;
import com.ventasdiscos.mscatalog.dto.PricingResponse;
import com.ventasdiscos.mscatalog.service.FavoritoService;
import com.ventasdiscos.mscatalog.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ventasdiscos.mscatalog.security.Servicios;
import com.ventasdiscos.mscatalog.security.ServiciosPermitidos;

/**
 * Contrato MS -> MS para Orders y, desde el Lote 5, para Accounts (HU-78).
 * Lote 6 · DOC-05: protegida con JWT de servicio (ServicioInternoInterceptor). Solo los servicios de
 * @ServiciosPermitidos pueden llamarla; sin token o con un JWT de usuario → 401; otro servicio → 403. El API
 * Gateway NO la expone. Se mantiene server.address=127.0.0.1 como medida complementaria. Sin X-Internal-Key.
 */
@Tag(name = "Interno (MS a MS)")
@RestController
@RequestMapping("/internal/catalog")
@RequiredArgsConstructor
public class InternalCatalogController {

    // Dependencia inyectada para acceder a pricing service sin crearla manualmente.
    private final PricingService pricingService;
    // Dependencia inyectada para acceder a favorito service sin crearla manualmente.
    private final FavoritoService favoritoService;

    @Operation(summary = "Orders: precio comercial + promociones vigentes de un carrito")
    @ServiciosPermitidos(Servicios.ORDERS)
    @PostMapping("/pricing")
    // POST: crea o procesa una solicitud; el controlador delega la operación y devuelve la respuesta correspondiente.
    public ResponseEntity<PricingResponse> cotizar(@Valid @RequestBody PricingRequest req) {
        return ResponseEntity.ok(pricingService.cotizar(req));
    }

    /**
     * Lote 5 · HU-78 / RN-43 (ERS 13.8): Accounts depura los favoritos de una cuenta que se está eliminando.
     * Idempotente. Ruta HTTP definitiva: POST /api/v1/internal/catalog/users/{userId}/favorites/purge.
     */
    @Operation(summary = "Accounts: borrar todos los favoritos de un usuario (idempotente)")
    @ServiciosPermitidos(Servicios.ACCOUNTS)
    @PostMapping("/users/{userId}/favorites/purge")
    public ResponseEntity<DepuracionResponse> depurarFavoritos(@PathVariable Long userId) {
        return ResponseEntity.ok(favoritoService.depurarUsuario(userId));
    }
}
