package ms_ordenes.ms_ordenes.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Lote 3: dato que no cumple una regla de negocio (por ejemplo, falta la dirección en DESPACHO_DOMICILIO) -> 400. */
public class ValidacionNegocioException extends ApiException {
    public ValidacionNegocioException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }

    public ValidacionNegocioException(String mensaje, Map<String, String> errores) {
        super(HttpStatus.BAD_REQUEST, mensaje, errores);
    }
}
