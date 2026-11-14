package com.ventasdiscos.mscatalog.model;

/**
 * Disponibilidad que Catalog MUESTRA (viene de MS-Inventory, RN-09).
 * NO_CONFIRMADA = Inventory no respondió: se muestra el catálogo sin afirmar stock (degradación).
 */
public enum DisponibilidadCatalogo {
    // Opción DISPONIBLE: representa el estado o tipo «disponible» permitido en este dominio.
    DISPONIBLE,
    // Opción STOCK_CRITICO: representa el estado o tipo «stock critico» permitido en este dominio.
    STOCK_CRITICO,
    // Opción AGOTADO: representa el estado o tipo «agotado» permitido en este dominio.
    AGOTADO,
    // Opción NO_CONFIRMADA: representa el estado o tipo «no confirmada» permitido en este dominio.
    NO_CONFIRMADA;

    // Ejecuta permite compra como parte de las responsabilidades de esta clase.
    public boolean permiteCompra() {
        return this == DISPONIBLE || this == STOCK_CRITICO;
    }
}
