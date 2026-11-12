package com.ventasdiscos.msaccounts.controller;

import com.ventasdiscos.msaccounts.dto.LoginRequest;
import com.ventasdiscos.msaccounts.dto.LoginResponse;
import com.ventasdiscos.msaccounts.dto.MensajeResponse;
import com.ventasdiscos.msaccounts.dto.RecuperarCorreoRequest;
import com.ventasdiscos.msaccounts.dto.RecuperarPasswordRequest;
import com.ventasdiscos.msaccounts.security.AuthUser;
import com.ventasdiscos.msaccounts.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación y recuperación de acceso (públicos, salvo logout).
 * Lote 5: rutas del ERS 12.11 (decisión del equipo): /auth/recuperacion/password y /auth/recuperacion/correo pasan a
 * /auth/recovery/password y /auth/recovery/email, sin alias antiguos. + POST /auth/logout.
 * HU-03 (logout, decisión del equipo): el JWT sigue SIN estado. El endpoint confirma el cierre (200) y el frontend
 * descarta el token. NO revoca criptográficamente un JWT ya emitido (no hay lista negra ni sesiones guardadas): hasta
 * que expire, el token sigue siendo válido en su firma. Lo que bloquea a una cuenta EN_ELIMINACION o ELIMINADA es la
 * validación de estado (RN-44), no el logout.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CuentaService cuentaService;

    /** HU-03 / RF-CTA-03: requiere un token válido (401 si falta). Ver la nota de la clase: no revoca el JWT. */
    @Operation(summary = "HU-03 Cerrar sesión (el frontend descarta el token)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<MensajeResponse> logout(@Parameter(hidden = true) AuthUser user) {
        return ResponseEntity.ok(new MensajeResponse("Sesión cerrada. El token debe descartarse en este dispositivo"));
    }

    @Operation(summary = "HU-02 Login único Cliente/Admin")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(cuentaService.login(req));
    }

    @Operation(summary = "HU-07 Recuperar contraseña (simulado: correo + RUT)")
    @PostMapping("/recovery/password")
    public ResponseEntity<MensajeResponse> recuperarPassword(@Valid @RequestBody RecuperarPasswordRequest req) {
        cuentaService.recuperarPassword(req);
        return ResponseEntity.ok(new MensajeResponse("Contraseña restablecida. Ya puedes iniciar sesión"));
    }

    @Operation(summary = "HU-07 Recuperar/actualizar correo identificando la cuenta por RUT")
    @PostMapping("/recovery/email")
    public ResponseEntity<MensajeResponse> recuperarCorreo(@Valid @RequestBody RecuperarCorreoRequest req) {
        cuentaService.recuperarCorreo(req);
        return ResponseEntity.ok(new MensajeResponse("Correo actualizado. Ya puedes iniciar sesión con el nuevo correo"));
    }
}
