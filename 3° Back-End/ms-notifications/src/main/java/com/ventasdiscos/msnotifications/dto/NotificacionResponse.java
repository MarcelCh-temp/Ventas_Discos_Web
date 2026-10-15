
package com.ventasdiscos.msnotifications.dto;

import com.ventasdiscos.msnotifications.model.Notificacion;
import com.ventasdiscos.msnotifications.model.TipoNotificacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionResponse {

    private Long id;
    private Long usuarioId;
    private String titulo;
    private String mensaje;
    private TipoNotificacion tipo;
    private LocalDateTime fechaCreacion;
    private Boolean leida;
    private Long referenciaId;

    public static NotificacionResponse desdeEntidad(
            Notificacion notificacion
    ) {

        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getUsuarioId(),
                notificacion.getTitulo(),
                notificacion.getMensaje(),
                notificacion.getTipo(),
                notificacion.getFechaCreacion(),
                notificacion.getLeida(),
                notificacion.getReferenciaId()
        );
    }
}
