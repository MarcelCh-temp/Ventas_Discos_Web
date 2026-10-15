package com.ventasdiscos.msinventory.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** RN-09: disponibilidad derivada del stock y del umbral. */
class InventarioTest {

    private Inventario con(int stock, int umbral) {
        Inventario i = new Inventario(1L, umbral);
        i.setStockActual(stock);
        return i;
    }

    @Test
    void stockCeroEsAgotado() {
        assertEquals(Disponibilidad.AGOTADO, con(0, 5).getDisponibilidad());
    }

    @Test
    void stockIgualAlUmbralEsCritico() {
        assertEquals(Disponibilidad.STOCK_CRITICO, con(5, 5).getDisponibilidad());
        assertEquals(Disponibilidad.STOCK_CRITICO, con(1, 5).getDisponibilidad());
    }

    @Test
    void stockSobreUmbralEsDisponible() {
        assertEquals(Disponibilidad.DISPONIBLE, con(6, 5).getDisponibilidad());
    }

    @Test
    void umbralCeroNuncaEsCritico() {
        assertEquals(Disponibilidad.DISPONIBLE, con(1, 0).getDisponibilidad());
    }
}
