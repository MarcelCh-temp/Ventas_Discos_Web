
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
// DTO: define los datos que entran o salen de una API; evita exponer directamente toda la entidad. Clase NotificacionResponse.
public class NotificacionResponse {

    // Identificador para identificar el registro.
    private Long id;
    // Dato usuario id del registro.
    private Long usuarioId;
    // Dato titulo del registro.
    private String titulo;
    // Dato mensaje del registro.
    private String mensaje;
    // Dato tipo del registro.
    private TipoNotificacion tipo;
    // Fecha u hora utilizada para registrar o controlar el proceso.
    private LocalDateTime fechaCreacion;
    // Dato leida del registro.
    private Boolean leida;
    // Dato referencia id del registro.
    private Long referenciaId;

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método desdeEntidad (desde entidad).
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
