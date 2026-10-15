
package com.ventasdiscos.msnotifications.controller;

import com.ventasdiscos.msnotifications.dto.EventoNotificacionRequest;
import com.ventasdiscos.msnotifications.service.EventoNotificacionService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/notifications")
public class EventoNotificacionController {

    private final EventoNotificacionService eventoNotificacionService;
    private final String claveInterna;

    public EventoNotificacionController(
            EventoNotificacionService eventoNotificacionService,
            @Value("${notifications.internal.api-key:}")
            String claveInterna
    ) {
        this.eventoNotificacionService = eventoNotificacionService;
        this.claveInterna = claveInterna;
    }

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> recibirEvento(
            @RequestHeader(
                    value = "X-Internal-Api-Key",
                    required = false
            )
            String claveRecibida,

            @Valid @RequestBody
            EventoNotificacionRequest request
    ) {
        validarClave(claveRecibida);

        boolean creado =
                eventoNotificacionService.procesarEvento(request);

        if (!creado) {
            return ResponseEntity.ok(
                    Map.<String, Object>of(
                            "processed", false,
                            "message", "Evento procesado anteriormente"
                    )
            );
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        Map.<String, Object>of(
                                "processed", true,
                                "message", "Notificacion registrada"
                        )
                );
    }

    private void validarClave(String claveRecibida) {

        if (claveInterna == null
                || claveInterna.isBlank()
                || claveRecibida == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Acceso interno no autorizado"
            );
        }

        byte[] esperada = claveInterna.getBytes(
                StandardCharsets.UTF_8
        );

        byte[] recibida = claveRecibida.getBytes(
                StandardCharsets.UTF_8
        );

        if (!MessageDigest.isEqual(esperada, recibida)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Acceso interno no autorizado"
            );
        }
    }
}
