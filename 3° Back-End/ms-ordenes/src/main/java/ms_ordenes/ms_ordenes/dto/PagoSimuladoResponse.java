package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.MetodoPago;
import ms_ordenes.ms_ordenes.model.ResultadoPagoSimulado;

/** Lote 3: resultado de la simulación académica de pago (no hay pasarela real). */
// Ejecuta pago simulado response como parte de las responsabilidades de esta clase.
public record PagoSimuladoResponse(MetodoPago metodo, ResultadoPagoSimulado resultado, boolean simulacionAcademica) {
}
