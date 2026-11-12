package ms_ordenes.ms_ordenes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import ms_ordenes.ms_ordenes.model.MetodoEntrega;

/**
 * HU-22 / HU-23 (RF-COM-05/06). Aquí solo el FORMATO de cada campo (vacío permitido). Qué campos son obligatorios
 * según el método lo decide DatosEntrega: RETIRO_TIENDA -> nombreReceptor y telefono;
 * DESPACHO_DOMICILIO -> además direccion y comuna. referencia siempre opcional. Sin RUT (no está en el UML 02).
 */
public record DatosEntregaRequest(
        @NotNull(message = "Selecciona un método de entrega") MetodoEntrega metodoEntrega,
        @Pattern(regexp = "^$|^[\\p{L}][\\p{L} .'-]{1,99}$",
                message = "El nombre debe tener entre 2 y 100 letras") String nombreReceptor,
        @Pattern(regexp = "^$|^\\+?[0-9]{8,12}$",
                message = "El teléfono debe tener entre 8 y 12 dígitos (puede empezar con +)") String telefono,
        @Pattern(regexp = "^$|^.{5,150}$", message = "La dirección debe tener entre 5 y 150 caracteres") String direccion,
        @Pattern(regexp = "^$|^[\\p{L}][\\p{L} .'-]{1,79}$",
                message = "La comuna debe tener entre 2 y 80 letras") String comuna,
        @Pattern(regexp = "^$|^.{1,200}$", message = "La referencia admite hasta 200 caracteres") String referencia) {
}
