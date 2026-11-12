package ms_ordenes.ms_ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * UML 02 ItemCarrito (Carrito "1" *-- "0..*" ItemCarrito). Lote 3.
 * precioUnitario = último precio final informado por Catalog; es referencial: el checkout siempre vuelve a
 * cotizar (RN-13) y el pedido usa el precio de ese momento.
 */
@Entity
@Table(name = "item_carrito", uniqueConstraints =
        @UniqueConstraint(name = "uk_item_carrito_producto", columnNames = {"carrito_id", "product_id"}))
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    protected ItemCarrito() {
    }

    ItemCarrito(Carrito carrito, Long productId, int cantidad, BigDecimal precioUnitario) {
        this.carrito = Objects.requireNonNull(carrito);
        this.productId = Objects.requireNonNull(productId);
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario == null ? BigDecimal.ZERO : precioUnitario;
    }

    public BigDecimal calcularSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    /** RN-11: entera y mayor que cero. */
    public boolean tieneCantidadValida() {
        return cantidad != null && cantidad > 0;
    }

    void cambiarCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    void actualizarPrecio(BigDecimal precio) {
        if (precio != null) {
            this.precioUnitario = precio;
        }
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
}
