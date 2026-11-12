package com.ventasdiscos.mscatalog.model;

import com.ventasdiscos.mscatalog.exception.StockInsuficienteException;
import com.ventasdiscos.mscatalog.exception.ValidacionNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lote 2 (UML 03): métodos de Inventario y RN-09 (disponibilidad derivada).
 * Portado de ms-inventory/InventarioTest; ya no hay setStockActual: el stock se fija con los métodos del UML.
 */
class InventarioTest {

    private static Producto producto() {
        Producto p = new Producto();
        p.setId(1L);
        p.setNombre("Disco");
        p.setCategoria(Categoria.MUSICA);
        return p;
    }

    private Inventario con(int stock, int umbral) {
        Inventario i = new Inventario(producto(), umbral);
        if (stock > 0) {
            i.aumentarStock(stock);
        }
        return i;
    }

    // ---------- RN-09 (tests originales) ----------

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

    // ---------- Lote 2: métodos del UML 03 ----------

    @Test
    void productoNuevoNaceAgotadoYConElIdDelProducto() {
        Inventario i = new Inventario(producto(), 5);
        assertEquals(1L, i.getProductId());
        assertEquals(0, i.getStockActual());
        assertTrue(i.estaAgotado());
        assertFalse(i.estaEnNivelCritico());
    }

    @Test
    void aumentarYDescontarStock() {
        Inventario i = con(10, 5);
        i.descontarStock(4);
        assertEquals(6, i.getStockActual());
        i.aumentarStock(2);
        assertEquals(8, i.getStockActual());
    }

    @Test
    void descontarMasQueElStockSeRechazaYNoCambiaNada() {
        Inventario i = con(3, 5);
        assertThrows(StockInsuficienteException.class, () -> i.descontarStock(4));
        assertEquals(3, i.getStockActual());
    }

    /** Revisión Lote 2: antes la suma se "daba vuelta" y el stock quedaba negativo. */
    @Test
    void aumentarSobreElMaximoDeIntSeRechazaYNoCambiaNada() {
        Inventario i = con(0, 5);
        i.ajustarStock(Integer.MAX_VALUE - 1);
        assertThrows(ValidacionNegocioException.class, () -> i.aumentarStock(2));
        assertEquals(Integer.MAX_VALUE - 1, i.getStockActual());
        i.aumentarStock(1);
        assertEquals(Inventario.STOCK_MAXIMO, i.getStockActual());
        assertThrows(ValidacionNegocioException.class, () -> i.aplicar(TipoMovimiento.ENTRADA, 1));
    }

    @Test
    void hayStockSuficiente() {
        Inventario i = con(3, 5);
        assertTrue(i.hayStockSuficiente(3));
        assertFalse(i.hayStockSuficiente(4));
        assertFalse(i.hayStockSuficiente(-1));
    }

    @Test
    void cantidadesCeroONegativasSeRechazan() {
        Inventario i = con(3, 5);
        assertThrows(ValidacionNegocioException.class, () -> i.aumentarStock(0));
        assertThrows(ValidacionNegocioException.class, () -> i.descontarStock(-1));
        assertEquals(3, i.getStockActual());
    }

    @Test
    void ajustarStockAceptaCeroPeroNoNegativos() {
        Inventario i = con(3, 5);
        i.ajustarStock(0);
        assertTrue(i.estaAgotado());
        assertThrows(ValidacionNegocioException.class, () -> i.ajustarStock(-1));
        assertEquals(0, i.getStockActual());
    }

    @Test
    void umbralNegativoSeRechazaYElCambioRecalculaLaDisponibilidad() {
        Inventario i = con(8, 5);
        assertEquals(Disponibilidad.DISPONIBLE, i.getDisponibilidad());
        i.configurarUmbral(10);
        assertEquals(Disponibilidad.STOCK_CRITICO, i.getDisponibilidad());
        assertThrows(ValidacionNegocioException.class, () -> i.configurarUmbral(-1));
        assertEquals(10, i.getUmbralCritico());
    }

    @Test
    void aplicarUsaElMetodoSegunElTipo() {
        Inventario i = con(5, 5);
        i.aplicar(TipoMovimiento.ENTRADA, 3);
        assertEquals(8, i.getStockActual());
        i.aplicar(TipoMovimiento.SALIDA, 2);
        assertEquals(6, i.getStockActual());
        i.aplicar(TipoMovimiento.AJUSTE, 1);
        assertEquals(1, i.getStockActual());
        assertThrows(StockInsuficienteException.class, () -> i.aplicar(TipoMovimiento.SALIDA, 2));
    }
}
