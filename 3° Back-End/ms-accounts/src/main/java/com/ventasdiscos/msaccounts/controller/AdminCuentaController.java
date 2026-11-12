package com.ventasdiscos.msaccounts.controller;

import com.ventasdiscos.msaccounts.dto.CuentaAdminResponse;
import com.ventasdiscos.msaccounts.security.AuthUser;
import com.ventasdiscos.msaccounts.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cierre del backend · RF-PED-01, RF-PED-02 y RF-SOP-05 (decisión de cierre 3/4).
 * GET /api/v1/admin/accounts?ids=1,2,3 (WebConfig agrega /api/v1): consulta AGRUPADA de clientes para que el frontend
 * administrativo muestre nombre y correo junto al userId que entregan Orders y Support. Solo ADMINISTRADOR.
 * Las reglas (máximo 100, ids válidos, administrador ACTIVO) están en CuentaService (CSR).
 */
@Tag(name = "Administración de cuentas")
@RestController
@RequestMapping("/admin/accounts")
@RequiredArgsConstructor
public class AdminCuentaController {

    private final CuentaService cuentaService;

    @Operation(summary = "RF-PED-01/02, RF-SOP-05: datos para identificar a varios clientes por su id (máx. 100)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<List<CuentaAdminResponse>> buscarPorIds(@Parameter(hidden = true) AuthUser user,
                                                                  @RequestParam List<Long> ids) {
        user.requireAdmin();
        return ResponseEntity.ok(cuentaService.cuentasParaAdmin(user.userId(), ids));
    }
}
