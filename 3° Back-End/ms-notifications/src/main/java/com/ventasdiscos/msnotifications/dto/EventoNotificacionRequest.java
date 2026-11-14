
package com.ventasdiscos.msnotifications.dto;

import com.ventasdiscos.msnotifications.model.TipoNotificacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// DTO: define los datos que entran o salen de una API; evita exponer directamente toda la entidad. Clase EventoNotificacionRequest.
public class EventoNotificacionRequest {

    @NotBlank(message = "El identificador del evento es obligatorio")
    @Size(max = 120)
    // Dato evento id del registro; validación: texto obligatorio y no vacío, longitud o tamaño permitido.
    private String eventoId;

    @NotBlank(message = "El origen del evento es obligatorio")
    @Size(max = 40)
    // Dato origen del registro; validación: texto obligatorio y no vacío, longitud o tamaño permitido.
    private String origen;

    @NotNull(message = "El usuario destinatario es obligatorio")
    @Positive(message = "El usuario debe ser positivo")
    // Dato usuario id del registro; validación: valor obligatorio (no nulo), número mayor que cero.
    private Long usuarioId;

    @NotBlank(message = "El titulo es obligatorio")
    // Dato titulo del registro; validación: texto obligatorio y no vacío.
    private String titulo;

    @NotBlank(message = "El mensaje es obligatorio")
    // Dato mensaje del registro; validación: texto obligatorio y no vacío.
    private String mensaje;

    @NotNull(message = "El tipo es obligatorio")
    // Dato tipo del registro; validación: valor obligatorio (no nulo).
    private TipoNotificacion tipo;

    @Positive(message = "La referencia debe ser positiva")
    // Dato referencia id del registro; validación: número mayor que cero.
    private Long referenciaId;
}
