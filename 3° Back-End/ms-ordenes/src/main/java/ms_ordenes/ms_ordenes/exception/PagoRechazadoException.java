package ms_ordenes.ms_ordenes.exception;

import org.springframework.http.HttpStatus;

/**
 * Lote 3 (RN-14, HU-25): el pago SIMULADO fue rechazado -> 402. No se crea el pedido ni se descuenta stock;
 * el carrito queda igual para corregir y reintentar. Simulación académica: no hay pasarela real.
 */
public class PagoRechazadoException extends ApiException {
    // Ejecuta pago rechazado exception como parte de las responsabilidades de esta clase.
    public PagoRechazadoException(String mensaje) {
        super(HttpStatus.PAYMENT_REQUIRED, mensaje);
    }
}
