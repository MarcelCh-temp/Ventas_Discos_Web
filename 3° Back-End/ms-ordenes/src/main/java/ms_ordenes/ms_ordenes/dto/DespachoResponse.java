package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.Despacho;

import java.time.LocalDateTime;

// DTO: define los datos que entran o salen de una API; evita exponer directamente toda la entidad. Clase DespachoResponse.
public record DespachoResponse(LocalDateTime fechaDespacho, String transportista, String numeroSeguimiento) {

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método from (from).
    public static DespachoResponse from(Despacho d) {
        return d == null ? null : new DespachoResponse(d.getFechaDespacho(), d.getTransportista(), d.getNumeroSeguimiento());
    }
}
