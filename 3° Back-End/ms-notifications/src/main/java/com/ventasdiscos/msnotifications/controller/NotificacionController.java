
package com.ventasdiscos.msnotifications.controller;

import com.ventasdiscos.msnotifications.dto.NotificacionResponse;
import com.ventasdiscos.msnotifications.model.TipoNotificacion;
import com.ventasdiscos.msnotifications.service.NotificacionService;

import jakarta.validation.constraints.Positive;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Validated
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    // GET /api/v1/notifications/me?usuarioId=1
    // Consulta las notificaciones de un usuario.
    // Permite filtros opcionales por tipo y fechas.
    @GetMapping("/me")
    public ResponseEntity<List<NotificacionResponse>> listar(
            @RequestParam @Positive Long usuarioId,
            @RequestParam(required = false) TipoNotificacion type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {

        List<NotificacionResponse> resultado =
                notificacionService.buscarNotificaciones(
                        usuarioId, type, from, to
                )
                .stream()
                .map(NotificacionResponse::desdeEntidad)
                .toList();

        return ResponseEntity.ok(resultado);
    }

    // GET /api/v1/notifications/me/unread-count?usuarioId=1
    // Cuenta las notificaciones no leidas.
    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(
            @RequestParam @Positive Long usuarioId) {

        long cantidad = notificacionService.contarNoLeidas(usuarioId);

        return ResponseEntity.ok(
                Map.of("unreadCount", cantidad)
        );
    }

    // PATCH /api/v1/notifications/1/read?usuarioId=1
    // Marca una notificacion como leida.
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> marcarComoLeida(
            @PathVariable @Positive Long id,
            @RequestParam @Positive Long usuarioId) {

        notificacionService.marcarComoLeida(id, usuarioId);

        return ResponseEntity.noContent().build();
    }

    // PATCH /api/v1/notifications/read-all?usuarioId=1
    // Marca todas las notificaciones pendientes como leidas.
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> marcarTodasComoLeidas(
            @RequestParam @Positive Long usuarioId) {

        int actualizadas =
                notificacionService.marcarTodasComoLeidas(usuarioId);

        return ResponseEntity.ok(
                Map.of("updatedCount", actualizadas)
        );
    }
}
