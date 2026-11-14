package ms_ordenes.ms_ordenes.model;

import java.util.List;

/**
 * Lote 3 (RF-COM-05/06, decisión del equipo): métodos de entrega disponibles.
 * UML 02 DatosEntrega.metodoEntrega es texto; aquí se guarda este código como texto.
 * Sin costos de envío ni direcciones de tienda (no están definidos en el ERS).
 */
public enum MetodoEntrega {
    // Opción RETIRO_TIENDA: representa el estado o tipo «retiro tienda» permitido en este dominio.
    RETIRO_TIENDA("Retiro en tienda", false),
    // Opción DESPACHO_DOMICILIO: representa el estado o tipo «despacho domicilio» permitido en este dominio.
    DESPACHO_DOMICILIO("Despacho a domicilio", true);

    // Atributo nombre: propiedad que se guarda o utiliza en esta entidad.
    private final String nombre;
    // Atributo requiereDireccion: propiedad que se guarda o utiliza en esta entidad.
    private final boolean requiereDireccion;

    MetodoEntrega(String nombre, boolean requiereDireccion) {
        this.nombre = nombre;
        this.requiereDireccion = requiereDireccion;
    }

    // Ejecuta get nombre como parte de las responsabilidades de esta clase.
    public String getNombre() {
        return nombre;
    }

    // Ejecuta requiere direccion como parte de las responsabilidades de esta clase.
    public boolean requiereDireccion() {
        return requiereDireccion;
    }

    /** Lo que publica GET /api/v1/checkout/delivery-methods y lo que valida DatosEntrega. */
    // Ejecuta campos obligatorios como parte de las responsabilidades de esta clase.
    public List<String> camposObligatorios() {
        return requiereDireccion
                ? List.of("nombreReceptor", "telefono", "direccion", "comuna")
                : List.of("nombreReceptor", "telefono");
    }

    // Ejecuta campos opcionales como parte de las responsabilidades de esta clase.
    public List<String> camposOpcionales() {
        return List.of("referencia");
    }
}
