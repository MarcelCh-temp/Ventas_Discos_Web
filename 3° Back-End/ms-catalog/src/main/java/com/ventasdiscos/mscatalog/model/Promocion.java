package com.ventasdiscos.mscatalog.model;

import jakarta.validation.constraints.*;
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
    // Atributo id: propiedad que se guarda o utiliza en esta entidad.
    private Long id;

    @Column(nullable = false, length = 120)
    @NotBlank
    // Atributo nombre: propiedad que se guarda o utiliza en esta entidad.
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull
    // Importe monetario; se conserva la precisión decimal para evitar errores de redondeo; validación: valor obligatorio (no nulo); enum persistido con las opciones permitidas.
    private TipoDescuento tipoDescuento;

    /** % (0 < v <= 100) o monto por unidad (> 0). */
    @Column(nullable = false, precision = 12, scale = 2)
    @DecimalMin(value = "0.0")
    // Atributo valor: propiedad que se guarda o utiliza en esta entidad.
    private BigDecimal valor;

    @Column(nullable = false)
    @NotNull
    // Atributo fechaInicio: propiedad que se guarda o utiliza en esta entidad.
    private LocalDate fechaInicio;

    @Column(nullable = false)
    // Atributo fechaFin: propiedad que se guarda o utiliza en esta entidad.
    private LocalDate fechaFin;

    /** Condición opcional: subtotal mínimo del carrito (HU-47, HU-52). */
    @Column(precision = 12, scale = 2)
    @DecimalMin(value = "0.0")
    // Atributo montoMinimoCompra: propiedad que se guarda o utiliza en esta entidad.
    private BigDecimal montoMinimoCompra;

    /** Grupo de productos: si no es null, aplica a toda la categoría. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    // Atributo categoria: propiedad que se guarda o utiliza en esta entidad.
    private Categoria categoria;

    @ManyToMany
    @JoinTable(name = "promocion_producto",
            joinColumns = @JoinColumn(name = "promocion_id"),
            inverseJoinColumns = @JoinColumn(name = "producto_id"))
    private Set<Producto> productos = new HashSet<>();

    /** HU-51: término manual anticipado. */
    @Column(nullable = false)
    // Atributo desactivada: propiedad que se guarda o utiliza en esta entidad.
    private boolean desactivada;

    // Atributo fechaDesactivacion: propiedad que se guarda o utiliza en esta entidad.
    private LocalDateTime fechaDesactivacion;

    /** RN-27: estado derivado, nunca guardado. */
    // Ejecuta estado como parte de las responsabilidades de esta clase.
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

    // Ejecuta aplica a como parte de las responsabilidades de esta clase.
    public boolean aplicaA(Producto producto) {
        if (categoria != null && categoria == producto.getCategoria()) {
            return true;
        }
        return productos.stream().anyMatch(p -> p.getId().equals(producto.getId()));
    }

    /** Descuento por unidad. Nunca mayor que el precio: el precio final no puede ser negativo (RN-27). */
    // Ejecuta descuento unitario como parte de las responsabilidades de esta clase.
    public BigDecimal descuentoUnitario(BigDecimal precio) {
        BigDecimal descuento = tipoDescuento == TipoDescuento.PORCENTAJE
                ? precio.multiply(valor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : valor;
        return descuento.min(precio);
    }
}
