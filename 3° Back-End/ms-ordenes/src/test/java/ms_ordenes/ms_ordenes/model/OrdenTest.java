package ms_ordenes.ms_ordenes.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrdenTest {

    @Test
    void testConstructoresYGettersSetters() {
        LocalDateTime ahora = LocalDateTime.now();

        Orden orden = new Orden(10L, new BigDecimal("5000.00"));
        orden.setId(1L);
        orden.setFechaCreacion(ahora);

        assertEquals(1L, orden.getId());
        assertEquals(10L, orden.getIdUsuario());
        assertEquals(new BigDecimal("5000.00"), orden.getTotal());
        assertEquals(EstadoOrden.PENDIENTE, orden.getEstado());
        assertEquals(ahora, orden.getFechaCreacion());
    }

    @Test
    void testConstructorVacioYSetters() {
        Orden ordenVacia = new Orden();
        ordenVacia.setId(2L);
        ordenVacia.setIdUsuario(20L);
        ordenVacia.setTotal(new BigDecimal("1500.00"));
        ordenVacia.setEstado(EstadoOrden.CONFIRMADO);

        assertEquals(2L, ordenVacia.getId());
        assertEquals(20L, ordenVacia.getIdUsuario());
        assertEquals(new BigDecimal("1500.00"), ordenVacia.getTotal());
        assertEquals(EstadoOrden.CONFIRMADO, ordenVacia.getEstado());
    }
}