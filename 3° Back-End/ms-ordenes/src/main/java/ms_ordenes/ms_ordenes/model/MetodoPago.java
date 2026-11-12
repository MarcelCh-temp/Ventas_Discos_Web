package ms_ordenes.ms_ordenes.model;

/**
 * Lote 3 (RF-COM-08, decisión del equipo): métodos de pago de la SIMULACIÓN ACADÉMICA.
 * No hay pasarela real ni se piden o guardan datos de tarjeta. El UML 02 no guarda el método en el pedido.
 */
public enum MetodoPago {
    WEBPAY("Webpay (simulación académica)"),
    TRANSFERENCIA("Transferencia (simulación académica)");

    private final String nombre;

    MetodoPago(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
