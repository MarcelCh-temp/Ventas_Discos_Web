package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.dto.InventarioResponse;
import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.Disponibilidad;
import com.ventasdiscos.mscatalog.model.EstadoComercial;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.model.Promocion;
import com.ventasdiscos.mscatalog.model.TipoDescuento;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de prueba reutilizables. */
final class Fixtures {

    private Fixtures() {
    }

    static Producto producto(Long id, String precio, Categoria categoria, EstadoComercial estado) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre("Producto " + id);
        p.setPrecioBase(new BigDecimal(precio));
        p.setCategoria(categoria);
        p.setGenero("Rock");
        p.setFormato("CD");
        p.setEstado(estado);
        return p;
    }

    /** Lote 2: existencias como las entrega InventarioService (reemplaza al InventarioDto del cliente HTTP). */
    static InventarioResponse existencia(long productId, int stock, Disponibilidad disponibilidad) {
        return new InventarioResponse(productId, "Producto " + productId, Categoria.MUSICA, stock, 5, disponibilidad);
    }

    static Promocion promo(Long id, TipoDescuento tipo, String valor, String minimo, Producto... productos) {
        Promocion p = new Promocion();
        p.setId(id);
        p.setNombre("Promo " + id);
        p.setTipoDescuento(tipo);
        p.setValor(new BigDecimal(valor));
        p.setFechaInicio(LocalDate.now().minusDays(1));
        p.setFechaFin(LocalDate.now().plusDays(1));
        p.setMontoMinimoCompra(minimo == null ? null : new BigDecimal(minimo));
        for (Producto prod : productos) {
            p.getProductos().add(prod);
        }
        return p;
    }
}
