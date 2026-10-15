
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
public class EventoNotificacionRequest {

    @NotBlank(message = "El identificador del evento es obligatorio")
    @Size(max = 120)
    private String eventoId;

    @NotBlank(message = "El origen del evento es obligatorio")
    @Size(max = 40)
    private String origen;

    @NotNull(message = "El usuario destinatario es obligatorio")
    @Positive(message = "El usuario debe ser positivo")
    private Long usuarioId;

    @NotBlank(message = "El titulo es obligatorio")
    private String titulo;

    @NotBlank(message = "El mensaje es obligatorio")
    private String mensaje;

    @NotNull(message = "El tipo es obligatorio")
    private TipoNotificacion tipo;

    @Positive(message = "La referencia debe ser positiva")
    private Long referenciaId;
}
