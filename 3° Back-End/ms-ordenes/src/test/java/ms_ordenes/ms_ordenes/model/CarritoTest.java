package ms_ordenes.ms_ordenes.model;

import ms_ordenes.ms_ordenes.exception.RecursoNoEncontradoException;
import ms_ordenes.ms_ordenes.exception.ValidacionNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lote 3 (HU-18 a HU-21, RN-11, RN-12): reglas propias del carrito. */
class CarritoTest {

    @Test
    void agregarElMismoProductoSumaYRecalculaElTotal() {
        Carrito c = new Carrito(5L);
        c.agregar(1L, 2, new BigDecimal("1000"));
        c.agregar(1L, 1, new BigDecimal("900"));
        c.agregar(2L, 1, new BigDecimal("500"));
        assertEquals(3, c.buscar(1L).orElseThrow().getCantidad());
        assertEquals(new BigDecimal("3200"), c.getTotal());
        assertEquals(2, c.getItems().size());
    }

    /** RN-12: bajar a 0 no elimina; eliminar es otra acción. */
    @Test
    void cantidadCeroONegativaSeRechazaYNoEliminaElProducto() {
        Carrito c = new Carrito(5L);
        c.agregar(1L, 1, new BigDecimal("1000"));
        assertThrows(ValidacionNegocioException.class, () -> c.cambiarCantidad(1L, 0, null));
        assertThrows(ValidacionNegocioException.class, () -> c.agregar(1L, -1, null));
        assertEquals(1, c.buscar(1L).orElseThrow().getCantidad());
    }

    @Test
    void quitarYVaciar() {
        Carrito c = new Carrito(5L);
        c.agregar(1L, 1, new BigDecimal("1000"));
        c.agregar(2L, 1, new BigDecimal("1000"));
        c.quitar(1L);
        assertEquals(new BigDecimal("1000"), c.getTotal());
        assertThrows(RecursoNoEncontradoException.class, () -> c.quitar(1L));
        c.vaciar();
        assertTrue(c.estaVacio());
        assertEquals(BigDecimal.ZERO, c.getTotal());
    }

    @Test
    void limiteDeProductosDistintos() {
        Carrito c = new Carrito(5L);
        for (long id = 1; id <= Carrito.MAX_PRODUCTOS; id++) {
            c.agregar(id, 1, BigDecimal.ONE);
        }
        assertThrows(ValidacionNegocioException.class, () -> c.agregar(999L, 1, BigDecimal.ONE));
        c.agregar(1L, 1, BigDecimal.ONE); // sumar a uno existente sí se puede
        assertEquals(2, c.buscar(1L).orElseThrow().getCantidad());
    }
}
