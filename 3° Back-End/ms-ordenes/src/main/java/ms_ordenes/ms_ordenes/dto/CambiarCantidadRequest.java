package ms_ordenes.ms_ordenes.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** HU-20 / RN-11 / RN-12: entero >= 1 (1.5 se rechaza); para quitar el producto se usa DELETE. */
public record CambiarCantidadRequest(
        @NotNull(message = "La cantidad es obligatoria")
        @Digits(integer = 6, fraction = 0, message = "La cantidad debe ser un número entero")
        @Positive(message = "La cantidad debe ser mayor que cero; para quitar el producto usa eliminar")
        BigDecimal cantidad) {

    // Ejecuta cantidad entera como parte de las responsabilidades de esta clase.
    public int cantidadEntera() {
        return cantidad.intValueExact();
    }
}
