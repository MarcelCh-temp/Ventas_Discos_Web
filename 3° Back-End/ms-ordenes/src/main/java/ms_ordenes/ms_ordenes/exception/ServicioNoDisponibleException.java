package ms_ordenes.ms_ordenes.exception;

import org.springframework.http.HttpStatus;

/** Lote 3: Catalog no respondió; sin él no se puede validar ni completar una compra (ERS 11.5) -> 503. */
public class ServicioNoDisponibleException extends ApiException {
    public ServicioNoDisponibleException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }
}
