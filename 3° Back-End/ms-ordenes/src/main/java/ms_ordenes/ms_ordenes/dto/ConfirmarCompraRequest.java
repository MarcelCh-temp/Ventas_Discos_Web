package ms_ordenes.ms_ordenes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import ms_ordenes.ms_ordenes.model.MetodoPago;
import ms_ordenes.ms_ordenes.model.ResultadoPagoSimulado;

import java.math.BigDecimal;

/**
 * HU-25 (RF-COM-07/08). Productos, cantidades y precios salen del carrito y de Catalog, NUNCA del frontend.
 * totalEsperado = el total que el Cliente vio en el resumen: solo se COMPARA con el recalculado; si difiere,
 * la compra no se confirma (RN-13: "si algo cambió debe informarlo y exigir corregir").
 * resultadoSimulado: simulación académica de pago; si no se envía, APROBADO. Sin datos de tarjeta.
 */
public record ConfirmarCompraRequest(
        @NotNull(message = "Los datos de entrega son obligatorios") @Valid DatosEntregaRequest datosEntrega,
        @NotNull(message = "Selecciona un método de pago") MetodoPago metodoPago,
        ResultadoPagoSimulado resultadoSimulado,
        @NotNull(message = "Envía el total que viste en el resumen")
        @PositiveOrZero(message = "El total no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El total no es válido") BigDecimal totalEsperado) {

    public ResultadoPagoSimulado resultadoEfectivo() {
        return resultadoSimulado == null ? ResultadoPagoSimulado.APROBADO : resultadoSimulado;
    }
}
