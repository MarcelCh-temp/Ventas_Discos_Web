package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.model.Promocion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Elige la promoción a aplicar a un producto (HU-52). Lógica pura, sin BD: fácil de probar.
 * Regla: si varias aplican, gana la de MAYOR descuento (no se acumulan) y, si empatan,
 * la de MENOR id. El desempate hace el resultado determinista: no depende del orden
 * en que la BD devuelva las promociones (auditoría CA1).
 */
@Component
public class CalculadoraPromociones {

    /**
     * @param subtotalCarrito null = vista de catálogo (solo promociones sin compra mínima);
     *                        con valor = carrito (se respeta la compra mínima).
     */
    public Optional<Promocion> mejorPromocion(Producto producto, List<Promocion> activas, BigDecimal subtotalCarrito) {
        return activas.stream()
                .filter(p -> p.aplicaA(producto))
                .filter(p -> p.getMontoMinimoCompra() == null
                        || (subtotalCarrito != null && subtotalCarrito.compareTo(p.getMontoMinimoCompra()) >= 0))
                .max(prioridad(producto));
    }

    /** Mayor descuento primero; en empate gana el id menor (para max: id mayor = menos prioridad). */
    static Comparator<Promocion> prioridad(Producto producto) {
        return Comparator.<Promocion, BigDecimal>comparing(p -> p.descuentoUnitario(producto.getPrecioBase()))
                .thenComparing(Promocion::getId, Comparator.reverseOrder());
    }

    public BigDecimal precioFinal(Producto producto, Promocion promocion) {
        return producto.getPrecioBase().subtract(promocion.descuentoUnitario(producto.getPrecioBase()));
    }
}
