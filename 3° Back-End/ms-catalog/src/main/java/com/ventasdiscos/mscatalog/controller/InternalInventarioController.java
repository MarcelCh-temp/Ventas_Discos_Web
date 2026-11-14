package com.ventasdiscos.mscatalog.controller;

import com.ventasdiscos.mscatalog.dto.OperacionInternaRequest;
import com.ventasdiscos.mscatalog.dto.OperacionInternaResponse;
import com.ventasdiscos.mscatalog.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ventasdiscos.mscatalog.security.Servicios;
import com.ventasdiscos.mscatalog.security.ServiciosPermitidos;
import com.ventasdiscos.mscatalog.security.ServicioInternoInterceptor;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Contrato MS -> MS (Orders, PostSale). NO se expone por el API Gateway (/internal/**).
 * Lote 2: trasladado desde ms-inventory con la MISMA ruta y el MISMO JSON; para Orders y PostSale solo cambia
 * la URL base (Catalog en vez de Inventory). Las rutas internas que usaba Catalog para hablar con Inventory
 * (availability, product-ids, products/{id}) ya no hacen falta: ahora es una consulta local.
 * Lote 6 · DOC-05: protegida con JWT de servicio (ServicioInternoInterceptor). Solo los servicios de
 * @ServiciosPermitidos pueden llamarla; sin token o con un JWT de usuario → 401; otro servicio → 403. El API
 * Gateway NO la expone. Se mantiene server.address=127.0.0.1 como medida complementaria. Sin X-Internal-Key.
 * DOC-08 (Lote 6): el Service comprueba además que el prefijo del operacionId corresponda al servicio del token.
 */
@Tag(name = "Interno (MS a MS)")
@RestController
@RequestMapping("/internal/inventory")
@RequiredArgsConstructor
public class InternalInventarioController {

    // Dependencia inyectada para acceder a service sin crearla manualmente.
    private final InventarioService service;

    @Operation(summary = "Orders/PostSale: descontar o reponer stock (idempotente por operacionId)")
    @ServiciosPermitidos({Servicios.ORDERS, Servicios.POSTSALE})
    @PostMapping("/operations")
    public ResponseEntity<OperacionInternaResponse> operacion(@Valid @RequestBody OperacionInternaRequest req,
                                                              HttpServletRequest request) {
        // El emisor sale del JWT de servicio ya validado por ServicioInternoInterceptor (nunca del cuerpo).
        String servicioEmisor = (String) request.getAttribute(ServicioInternoInterceptor.ATRIBUTO_EMISOR);
        OperacionInternaResponse resp = service.aplicarOperacionInterna(servicioEmisor, req);
        return ResponseEntity.status(resp.yaAplicada() ? HttpStatus.OK : HttpStatus.CREATED).body(resp);
    }
}
