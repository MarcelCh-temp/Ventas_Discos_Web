
package com.ventasdiscos.msnotifications.controller;

import com.ventasdiscos.msnotifications.dto.NotificacionResponse;
import com.ventasdiscos.msnotifications.model.TipoNotificacion;
import com.ventasdiscos.msnotifications.service.NotificacionService;

import jakarta.validation.constraints.Positive;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Validated
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(
            NotificacionService notificacionService
    ) {
        this.notificacionService = notificacionService;
    }

    // HU-54 y HU-55: Listar y filtrar notificaciones
    @GetMapping("/me")
    public ResponseEntity<List<NotificacionResponse>> listar(
            Principal principal,

            @RequestParam(required = false)
            TipoNotificacion type,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        Long usuarioId = obtenerUsuarioId(principal);

        List<NotificacionResponse> respuesta =
                notificacionService.buscarNotificaciones(
                        usuarioId,
                        type,
                        from,
                        to
                )
                .stream()
                .map(NotificacionResponse::desdeEntidad)
                .toList();

        return ResponseEntity.ok(respuesta);
    }

    // HU-54: Contar notificaciones no leidas
    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(
            Principal principal
    ) {
        Long usuarioId = obtenerUsuarioId(principal);

        long cantidad = notificacionService
                .contarNoLeidas(usuarioId);

        return ResponseEntity.ok(
                Map.of("unreadCount", cantidad)
        );
    }

    // HU-54: Marcar una notificacion como leida
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> marcarComoLeida(
            @PathVariable
            @Positive(message = "El identificador debe ser positivo")
            Long id,

            Principal principal
    ) {
        Long usuarioId = obtenerUsuarioId(principal);

        notificacionService.marcarComoLeida(
                id,
                usuarioId
        );

        return ResponseEntity.noContent().build();
    }

    // HU-54: Marcar todas como leidas
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> marcarTodasComoLeidas(
            Principal principal
    ) {
        Long usuarioId = obtenerUsuarioId(principal);

        int cantidad = notificacionService
                .marcarTodasComoLeidas(usuarioId);

        return ResponseEntity.ok(
                Map.of("updatedCount", cantidad)
        );
    }

    private Long obtenerUsuarioId(Principal principal) {

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado"
            );
        }

        try {
            Long usuarioId = Long.valueOf(
                    principal.getName()
            );

            if (usuarioId <= 0) {
                throw new NumberFormatException();
            }

            return usuarioId;

        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo identificar al usuario"
            );
        }
    }
}
