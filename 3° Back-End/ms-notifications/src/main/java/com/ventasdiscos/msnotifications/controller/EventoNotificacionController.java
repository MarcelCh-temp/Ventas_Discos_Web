
package com.ventasdiscos.msnotifications.controller;

import com.ventasdiscos.msnotifications.dto.DepuracionResponse;
import com.ventasdiscos.msnotifications.dto.EventoNotificacionRequest;
import com.ventasdiscos.msnotifications.service.EventoNotificacionService;
import com.ventasdiscos.msnotifications.service.NotificacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ventasdiscos.msnotifications.security.Servicios;
import com.ventasdiscos.msnotifications.security.ServiciosPermitidos;

import java.util.Map;

/**
 * Contrato MS -> MS: eventos de Soporte, Orders y PostSale, y depuración de HU-78 pedida por Accounts.
 * Lote 6 · DOC-05: protegida con JWT de servicio (ServicioInternoInterceptor). Solo los servicios de
 * @ServiciosPermitidos pueden llamarla; sin token o con un JWT de usuario → 401; otro servicio → 403. El API
 * Gateway NO la expone. Se mantiene server.address=127.0.0.1 como medida complementaria. Sin X-Internal-Key.
 */
@RestController
@RequestMapping("/api/v1/internal/notifications")
public class EventoNotificacionController {

    // Dependencia inyectada para acceder a evento notificacion service sin crearla manualmente.
    private final EventoNotificacionService eventoNotificacionService;
    // Dependencia inyectada para acceder a notificacion service sin crearla manualmente.
    private final NotificacionService notificacionService;

    public EventoNotificacionController(
            EventoNotificacionService eventoNotificacionService,
            NotificacionService notificacionService) {
        this.eventoNotificacionService = eventoNotificacionService;
        this.notificacionService = notificacionService;
    }

    // Lote 5 · HU-78 / RN-43 (ERS 13.8): Accounts depura las notificaciones personales de una cuenta que se está
    // eliminando. Idempotente. Ruta: POST /api/v1/internal/notifications/users/{userId}/purge.
    @ServiciosPermitidos(Servicios.ACCOUNTS)
    @PostMapping("/users/{userId}/purge")
    public ResponseEntity<DepuracionResponse> depurarUsuario(@PathVariable Long userId) {
        return ResponseEntity.ok(notificacionService.depurarUsuario(userId));
    }

    // POST /api/v1/internal/notifications/events
    // Registra una notificacion a partir de un evento.
    @ServiciosPermitidos({Servicios.SUPPORT, Servicios.ORDERS, Servicios.POSTSALE})
    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> recibirEvento(
            @Valid @RequestBody EventoNotificacionRequest evento) {

        boolean procesado =
                eventoNotificacionService.procesarEvento(evento);

        return ResponseEntity
                .status(procesado ? HttpStatus.CREATED : HttpStatus.OK)
                .body(Map.of(
                        "processed", procesado,
                        "message", procesado
                                ? "Notificacion registrada"
                                : "Evento procesado anteriormente"
                ));
    }
}
