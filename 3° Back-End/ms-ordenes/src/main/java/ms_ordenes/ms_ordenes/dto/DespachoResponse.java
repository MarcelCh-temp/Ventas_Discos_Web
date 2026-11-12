package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.Despacho;

import java.time.LocalDateTime;

public record DespachoResponse(LocalDateTime fechaDespacho, String transportista, String numeroSeguimiento) {

    public static DespachoResponse from(Despacho d) {
        return d == null ? null : new DespachoResponse(d.getFechaDespacho(), d.getTransportista(), d.getNumeroSeguimiento());
    }
}
