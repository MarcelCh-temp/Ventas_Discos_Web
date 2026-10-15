package com.ventasdiscos.mscatalog.model;

/**
 * Disponibilidad que Catalog MUESTRA (viene de MS-Inventory, RN-09).
 * NO_CONFIRMADA = Inventory no respondió: se muestra el catálogo sin afirmar stock (degradación).
 */
public enum DisponibilidadCatalogo {
    DISPONIBLE,
    STOCK_CRITICO,
    AGOTADO,
    NO_CONFIRMADA;

    public boolean permiteCompra() {
        return this == DISPONIBLE || this == STOCK_CRITICO;
    }
}
