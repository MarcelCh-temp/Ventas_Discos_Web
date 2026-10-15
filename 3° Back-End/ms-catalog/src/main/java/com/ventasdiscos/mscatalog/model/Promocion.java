package com.ventasdiscos.mscatalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Promoción (HU-47..52). Se aplica a productos específicos y/o a un grupo = categoría completa.
 * @ManyToMany válido: Producto y Promocion viven en el mismo MS.
 */
@Entity
@Table(name = "promocion")
@Getter
@Setter
@NoArgsConstructor
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDescuento tipoDescuento;

    /** % (0 < v <= 100) o monto por unidad (> 0). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    /** Condición opcional: subtotal mínimo del carrito (HU-47, HU-52). */
    @Column(precision = 12, scale = 2)
    private BigDecimal montoMinimoCompra;

    /** Grupo de productos: si no es null, aplica a toda la categoría. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Categoria categoria;

    @ManyToMany
    @JoinTable(name = "promocion_producto",
            joinColumns = @JoinColumn(name = "promocion_id"),
            inverseJoinColumns = @JoinColumn(name = "producto_id"))
    private Set<Producto> productos = new HashSet<>();

    /** HU-51: término manual anticipado. */
    @Column(nullable = false)
    private boolean desactivada;

    private LocalDateTime fechaDesactivacion;

    /** RN-27: estado derivado, nunca guardado. */
    public EstadoPromocion estado(LocalDate hoy) {
        if (desactivada) {
            return EstadoPromocion.DESACTIVADA;
        }
        if (hoy.isBefore(fechaInicio)) {
            return EstadoPromocion.PROGRAMADA;
        }
        if (hoy.isAfter(fechaFin)) {
            return EstadoPromocion.FINALIZADA;
        }
        return EstadoPromocion.ACTIVA;
    }

    public boolean aplicaA(Producto producto) {
        if (categoria != null && categoria == producto.getCategoria()) {
            return true;
        }
        return productos.stream().anyMatch(p -> p.getId().equals(producto.getId()));
    }

    /** Descuento por unidad. Nunca mayor que el precio: el precio final no puede ser negativo (RN-27). */
    public BigDecimal descuentoUnitario(BigDecimal precio) {
        BigDecimal descuento = tipoDescuento == TipoDescuento.PORCENTAJE
                ? precio.multiply(valor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : valor;
        return descuento.min(precio);
    }
}
