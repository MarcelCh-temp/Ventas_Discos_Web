package com.ventasdiscos.mscatalog.controller;

import com.ventasdiscos.mscatalog.dto.FavoritoResponse;
import com.ventasdiscos.mscatalog.security.AuthUser;
import com.ventasdiscos.mscatalog.service.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Favoritos del usuario autenticado (EP-11). El userId sale del token, no de la URL. */
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FavoritoController {

    // Dependencia inyectada para acceder a service sin crearla manualmente.
    private final FavoritoService service;

    @Operation(summary = "HU-58 Mis favoritos")
    @GetMapping
    // GET: consulta información; el controlador delega la operación y devuelve la respuesta correspondiente.
    public ResponseEntity<List<FavoritoResponse>> listar(@Parameter(hidden = true) AuthUser user) {
        return ResponseEntity.ok(service.listar(user.userId()));
    }

    @Operation(summary = "HU-56 Agregar a favoritos")
    @PostMapping("/{productId}")
    public ResponseEntity<FavoritoResponse> agregar(@Parameter(hidden = true) AuthUser user,
                                                    @PathVariable Long productId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregar(user.userId(), productId));
    }

    @Operation(summary = "HU-57 Quitar de favoritos")
    @DeleteMapping("/{productId}")
    // DELETE: solicita la eliminación o desactivación de un recurso. Método eliminar (eliminar).
    public ResponseEntity<Void> eliminar(@Parameter(hidden = true) AuthUser user, @PathVariable Long productId) {
        service.eliminar(user.userId(), productId);
        return ResponseEntity.noContent().build();
    }
}
