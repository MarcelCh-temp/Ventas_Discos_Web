package ms_ordenes.ms_ordenes.model;

/**
 * Lote 3 (RF-COM-08, decisión del equipo): métodos de pago de la SIMULACIÓN ACADÉMICA.
 * No hay pasarela real ni se piden o guardan datos de tarjeta. El UML 02 no guarda el método en el pedido.
 */
public enum MetodoPago {
    // Opción WEBPAY: representa el estado o tipo «webpay» permitido en este dominio.
    WEBPAY("Webpay (simulación académica)"),
    // Opción TRANSFERENCIA: representa el estado o tipo «transferencia» permitido en este dominio.
    TRANSFERENCIA("Transferencia (simulación académica)");

    // Atributo nombre: propiedad que se guarda o utiliza en esta entidad.
    private final String nombre;

    MetodoPago(String nombre) {
        this.nombre = nombre;
    }

    // Ejecuta get nombre como parte de las responsabilidades de esta clase.
    public String getNombre() {
        return nombre;
    }
}
