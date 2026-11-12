package ms_ordenes.ms_ordenes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** HU-24: revisar la compra. Los productos y cantidades salen del carrito guardado, no del frontend. */
public record ValidarCheckoutRequest(
        @NotNull(message = "Los datos de entrega son obligatorios") @Valid DatosEntregaRequest datosEntrega) {
}
