
package com.ventasdiscos.msnotifications.controller;

import com.ventasdiscos.msnotifications.dto.EventoNotificacionRequest;
import com.ventasdiscos.msnotifications.service.EventoNotificacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/notifications")
public class EventoNotificacionController {

    private final EventoNotificacionService eventoNotificacionService;

    public EventoNotificacionController(
            EventoNotificacionService eventoNotificacionService) {
        this.eventoNotificacionService = eventoNotificacionService;
    }

    // POST /api/v1/internal/notifications/events
    // Registra una notificacion a partir de un evento.
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
