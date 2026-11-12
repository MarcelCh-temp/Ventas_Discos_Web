
package com.ventasdiscos.msnotifications.controller;

import com.ventasdiscos.msnotifications.dto.NotificacionResponse;
import com.ventasdiscos.msnotifications.model.TipoNotificacion;
import com.ventasdiscos.msnotifications.security.AuthUser;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.ventasdiscos.msnotifications.service.NotificacionService;

import jakarta.validation.constraints.Positive;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Lote 1 (HU-54, HU-55): el usuario sale del token. Antes venía en ?usuarioId= y cualquiera
// podía leer o marcar como leídas las notificaciones de otra persona.
@RestController
@RequestMapping("/api/v1/notifications")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    // GET /api/v1/notifications/me
    // Consulta las notificaciones de un usuario.
    // Permite filtros opcionales por tipo y fechas.
    @GetMapping("/me")
    public ResponseEntity<List<NotificacionResponse>> listar(
            @Parameter(hidden = true) AuthUser user,
            @RequestParam(required = false) TipoNotificacion type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {

        List<NotificacionResponse> resultado =
                notificacionService.buscarNotificaciones(
                        user.userId(), type, from, to
                )
                .stream()
                .map(NotificacionResponse::desdeEntidad)
                .toList();

        return ResponseEntity.ok(resultado);
    }

    // GET /api/v1/notifications/me/unread-count
    // Cuenta las notificaciones no leidas.
    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(
            @Parameter(hidden = true) AuthUser user) {

        long cantidad = notificacionService.contarNoLeidas(user.userId());

        return ResponseEntity.ok(
                Map.of("unreadCount", cantidad)
        );
    }

    // PATCH /api/v1/notifications/1/read (solo si la notificación es del usuario del token)
    // Marca una notificacion como leida.
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> marcarComoLeida(
            @Parameter(hidden = true) AuthUser user,
            @PathVariable @Positive Long id) {

        notificacionService.marcarComoLeida(id, user.userId());

        return ResponseEntity.noContent().build();
    }

    // PATCH /api/v1/notifications/read-all
    // Marca todas las notificaciones pendientes como leidas.
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> marcarTodasComoLeidas(
            @Parameter(hidden = true) AuthUser user) {

        int actualizadas =
                notificacionService.marcarTodasComoLeidas(user.userId());

        return ResponseEntity.ok(
                Map.of("updatedCount", actualizadas)
        );
    }
}
