package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.EstadoComercial;
import com.ventasdiscos.mscatalog.model.EstadoPromocion;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.model.Promocion;
import com.ventasdiscos.mscatalog.model.TipoDescuento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RN-27: estado derivado de fechas y descuento que nunca deja precio negativo. */
class PromocionModelTest {

    private final Producto disco = Fixtures.producto(1L, "10000", Categoria.MUSICA, EstadoComercial.ACTIVO);

    @Test
    void estadoSeDerivaDeFechas() {
        Promocion p = Fixtures.promo(1L, TipoDescuento.PORCENTAJE, "10", null, disco);
        LocalDate hoy = LocalDate.now();
        assertEquals(EstadoPromocion.ACTIVA, p.estado(hoy));
        assertEquals(EstadoPromocion.PROGRAMADA, p.estado(hoy.minusDays(5)));
        assertEquals(EstadoPromocion.FINALIZADA, p.estado(hoy.plusDays(5)));
        p.setDesactivada(true);
        assertEquals(EstadoPromocion.DESACTIVADA, p.estado(hoy));
    }

    @Test
    void porcentajeCalculaDescuento() {
        Promocion p = Fixtures.promo(1L, TipoDescuento.PORCENTAJE, "15", null, disco);
        assertEquals(0, new BigDecimal("1500").compareTo(p.descuentoUnitario(disco.getPrecioBase())));
    }

    @Test
    void montoMayorAlPrecioNoGeneraPrecioNegativo() {
        Promocion p = Fixtures.promo(1L, TipoDescuento.MONTO, "99999", null, disco);
        assertEquals(0, disco.getPrecioBase().compareTo(p.descuentoUnitario(disco.getPrecioBase())));
    }

    @Test
    void promocionPorCategoriaAplicaATodaLaCategoria() {
        Promocion p = Fixtures.promo(1L, TipoDescuento.PORCENTAJE, "10", null);
        p.setCategoria(Categoria.MUSICA);
        assertTrue(p.aplicaA(disco));
    }

    @Test
    void noAplicaAProductoDeOtraCategoriaNiFueraDeLaLista() {
        Promocion p = Fixtures.promo(1L, TipoDescuento.PORCENTAJE, "10", null, disco);
        p.setCategoria(Categoria.VIDEOJUEGOS);
        Producto otro = Fixtures.producto(2L, "5000", Categoria.MUSICA, EstadoComercial.ACTIVO);
        assertFalse(p.aplicaA(otro));
        assertTrue(p.aplicaA(disco));
    }
}
