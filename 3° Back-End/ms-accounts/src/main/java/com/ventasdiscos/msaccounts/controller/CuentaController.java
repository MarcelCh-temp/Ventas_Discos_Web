package com.ventasdiscos.msaccounts.controller;

import com.ventasdiscos.msaccounts.dto.ActualizarPerfilRequest;
import com.ventasdiscos.msaccounts.dto.CambioPasswordRequest;
import com.ventasdiscos.msaccounts.dto.EliminacionCuentaResponse;
import com.ventasdiscos.msaccounts.dto.EliminarCuentaRequest;
import com.ventasdiscos.msaccounts.dto.EstadisticasCuentasResponse;
import com.ventasdiscos.msaccounts.dto.MensajeResponse;
import com.ventasdiscos.msaccounts.dto.PerfilResponse;
import com.ventasdiscos.msaccounts.dto.RegistroRequest;
import com.ventasdiscos.msaccounts.model.EstadoCuenta;
import com.ventasdiscos.msaccounts.security.AuthUser;
import com.ventasdiscos.msaccounts.service.CuentaService;
import com.ventasdiscos.msaccounts.service.EliminacionCuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de cuenta. Sin reglas de negocio: valida formato (@Valid) y delega.
 * Lote 5: rutas del ERS 12.11 (decisión del equipo): /accounts/registro pasa a /accounts/register, sin alias
 * antiguo (el frontend original no consume Accounts). + DELETE /accounts/me (HU-78).
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;
    private final EliminacionCuentaService eliminacionService;

    @Operation(summary = "HU-01 Registro público (crea solo Cliente)")
    @PostMapping("/register")
    public ResponseEntity<PerfilResponse> registrar(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.registrar(req));
    }

    @Operation(summary = "HU-04 Ver mi perfil", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<PerfilResponse> perfil(@Parameter(hidden = true) AuthUser user) {
        return ResponseEntity.ok(cuentaService.obtenerPerfil(user.userId()));
    }

    @Operation(summary = "HU-05 Editar mi perfil", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/me")
    public ResponseEntity<PerfilResponse> actualizar(@Parameter(hidden = true) AuthUser user,
                                                     @Valid @RequestBody ActualizarPerfilRequest req) {
        return ResponseEntity.ok(cuentaService.actualizarPerfil(user.userId(), req));
    }

    @Operation(summary = "HU-06 Cambiar mi contraseña", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/me/password")
    public ResponseEntity<MensajeResponse> cambiarPassword(@Parameter(hidden = true) AuthUser user,
                                                           @Valid @RequestBody CambioPasswordRequest req) {
        cuentaService.cambiarPassword(user.userId(), req);
        return ResponseEntity.ok(new MensajeResponse("Contraseña actualizada correctamente"));
    }

    @Operation(summary = "HU-45 Cantidad de usuarios (Dashboard, solo Admin)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/stats")
    public ResponseEntity<EstadisticasCuentasResponse> estadisticas(@Parameter(hidden = true) AuthUser user) {
        user.requireAdmin();
        return ResponseEntity.ok(cuentaService.estadisticas(user.userId()));
    }

    /**
     * HU-78 / RF-CTA-08: el Cliente confirma con su contraseña actual (cuerpo {"password": "..."}). 200 = cuenta
     * ELIMINADA; 202 = quedó EN_ELIMINACION (bloqueada) y la depuración pendiente se completará con reintentos.
     * Repetir el DELETE con la cuenta EN_ELIMINACION continúa la misma eliminación sin volver a pedir la contraseña.
     */
    @Operation(summary = "HU-78 Eliminar mi cuenta (confirmando con la contraseña actual)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/me")
    public ResponseEntity<EliminacionCuentaResponse> eliminar(@Parameter(hidden = true) AuthUser user,
                                                              @Valid @RequestBody(required = false) EliminarCuentaRequest req) {
        EliminacionCuentaResponse r = eliminacionService.eliminar(user.userId(), req == null ? null : req.password());
        return ResponseEntity.status(r.estado() == EstadoCuenta.ELIMINADA ? HttpStatus.OK : HttpStatus.ACCEPTED).body(r);
    }
}
