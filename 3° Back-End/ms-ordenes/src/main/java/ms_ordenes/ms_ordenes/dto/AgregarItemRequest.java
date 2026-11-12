package ms_ordenes.ms_ordenes.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * HU-18: agregar al carrito. Cantidad entera > 0 (RN-11). Se recibe como BigDecimal + @Digits(fraction = 0)
 * (igual que el inventario de Catalog) para RECHAZAR 1.5 en vez de truncarlo a 1 sin avisar.
 */
public record AgregarItemRequest(
        @NotNull(message = "El producto es obligatorio") @Positive(message = "El producto no es válido") Long productId,
        @NotNull(message = "La cantidad es obligatoria")
        @Digits(integer = 6, fraction = 0, message = "La cantidad debe ser un número entero")
        @Positive(message = "La cantidad debe ser mayor que cero") BigDecimal cantidad) {

    public int cantidadEntera() {
        return cantidad.intValueExact();
    }
}
