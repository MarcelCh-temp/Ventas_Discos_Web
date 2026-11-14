package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.MetodoPago;
import ms_ordenes.ms_ordenes.model.ResultadoPagoSimulado;

import java.util.List;

/** RF-COM-08: métodos permitidos y resultados posibles de la SIMULACIÓN ACADÉMICA de pago. */
public record OpcionesPagoResponse(boolean simulacionAcademica, String aviso, List<MetodoPagoResponse> metodos,
                                   List<ResultadoPagoSimulado> resultadosSimulados,
                                   ResultadoPagoSimulado resultadoPorDefecto) {

    // Ejecuta metodo pago response como parte de las responsabilidades de esta clase.
    public record MetodoPagoResponse(MetodoPago codigo, String nombre) {
    }
}
