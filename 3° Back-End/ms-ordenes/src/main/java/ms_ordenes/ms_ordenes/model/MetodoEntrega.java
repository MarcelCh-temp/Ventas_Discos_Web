package ms_ordenes.ms_ordenes.model;

import java.util.List;

/**
 * Lote 3 (RF-COM-05/06, decisión del equipo): métodos de entrega disponibles.
 * UML 02 DatosEntrega.metodoEntrega es texto; aquí se guarda este código como texto.
 * Sin costos de envío ni direcciones de tienda (no están definidos en el ERS).
 */
public enum MetodoEntrega {
    RETIRO_TIENDA("Retiro en tienda", false),
    DESPACHO_DOMICILIO("Despacho a domicilio", true);

    private final String nombre;
    private final boolean requiereDireccion;

    MetodoEntrega(String nombre, boolean requiereDireccion) {
        this.nombre = nombre;
        this.requiereDireccion = requiereDireccion;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean requiereDireccion() {
        return requiereDireccion;
    }

    /** Lo que publica GET /api/v1/checkout/delivery-methods y lo que valida DatosEntrega. */
    public List<String> camposObligatorios() {
        return requiereDireccion
                ? List.of("nombreReceptor", "telefono", "direccion", "comuna")
                : List.of("nombreReceptor", "telefono");
    }

    public List<String> camposOpcionales() {
        return List.of("referencia");
    }
}
