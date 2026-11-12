package ms_ordenes.ms_ordenes.dto;

import jakarta.validation.constraints.Pattern;

/**
 * HU-30 / RF-PED-05: SOLO estos campos son corregibles (no el método de entrega, el cliente, los productos,
 * cantidades, precios, descuentos ni el total). Campo ausente (null) = no cambia. Mismo formato que el checkout.
 */
public record CorreccionEntregaRequest(
        @Pattern(regexp = "^$|^[\\p{L}][\\p{L} .'-]{1,99}$",
                message = "El nombre debe tener entre 2 y 100 letras") String nombreReceptor,
        @Pattern(regexp = "^$|^\\+?[0-9]{8,12}$",
                message = "El teléfono debe tener entre 8 y 12 dígitos (puede empezar con +)") String telefono,
        @Pattern(regexp = "^$|^.{5,150}$", message = "La dirección debe tener entre 5 y 150 caracteres") String direccion,
        @Pattern(regexp = "^$|^[\\p{L}][\\p{L} .'-]{1,79}$",
                message = "La comuna debe tener entre 2 y 80 letras") String comuna,
        @Pattern(regexp = "^$|^.{1,200}$", message = "La referencia admite hasta 200 caracteres") String referencia) {
}
