package ms_soporte.ms_soporte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Lote 1 (HU-59): datos que envía el Cliente para crear una solicitud.
 * Solo validaciones de ENTRADA (obligatorios, largo, positivo). Las reglas de negocio están en TicketService.
 * No trae idUsuario ni estado: el dueño sale del token y el estado inicial lo pone el Service.
 */
public record CrearTicketRequest(
        @Positive(message = "El pedido debe ser un número positivo")
        Long orderId,

        @NotBlank(message = "El asunto es obligatorio")
        @Size(max = 150, message = "El asunto no puede superar 150 caracteres")
        String asunto,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 50, message = "La categoría no puede superar 50 caracteres")
        String categoria,

        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 2000, message = "El mensaje no puede superar 2000 caracteres")
        String mensaje
) {
}
