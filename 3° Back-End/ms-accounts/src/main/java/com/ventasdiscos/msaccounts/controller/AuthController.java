package com.ventasdiscos.msaccounts.controller;

import com.ventasdiscos.msaccounts.dto.LoginRequest;
import com.ventasdiscos.msaccounts.dto.LoginResponse;
import com.ventasdiscos.msaccounts.dto.MensajeResponse;
import com.ventasdiscos.msaccounts.dto.RecuperarCorreoRequest;
import com.ventasdiscos.msaccounts.dto.RecuperarPasswordRequest;
import com.ventasdiscos.msaccounts.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación y recuperación de acceso (públicos).
 * HU-03 (logout): con JWT stateless lo realiza el frontend descartando el token;
 * no se crea endpoint "por si acaso" (ver README, decisión pendiente JWT).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CuentaService cuentaService;

    @Operation(summary = "HU-02 Login único Cliente/Admin")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(cuentaService.login(req));
    }

    @Operation(summary = "HU-07 Recuperar contraseña (simulado: correo + RUT)")
    @PostMapping("/recuperacion/password")
    public ResponseEntity<MensajeResponse> recuperarPassword(@Valid @RequestBody RecuperarPasswordRequest req) {
        cuentaService.recuperarPassword(req);
        return ResponseEntity.ok(new MensajeResponse("Contraseña restablecida. Ya puedes iniciar sesión"));
    }

    @Operation(summary = "HU-07 Recuperar/actualizar correo identificando la cuenta por RUT")
    @PostMapping("/recuperacion/correo")
    public ResponseEntity<MensajeResponse> recuperarCorreo(@Valid @RequestBody RecuperarCorreoRequest req) {
        cuentaService.recuperarCorreo(req);
        return ResponseEntity.ok(new MensajeResponse("Correo actualizado. Ya puedes iniciar sesión con el nuevo correo"));
    }
}
