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

import java.math.BigDecimal;
import java.util.Objects;

/**
 * UML 02 ItemPedido (Pedido "1" *-- "1..*" ItemPedido). Lote 3.
 * Snapshot del momento de compra (RN-15, RNF-10, RF-MIP-02): nombre, precio final unitario (con la promoción
 * vigente) y subtotal. Los cambios posteriores del catálogo no lo alteran: no tiene setters.
 */
@Entity
@Table(name = "item_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "nombre_producto_snapshot", nullable = false, length = 150)
    private String nombreProductoSnapshot;

    @Column(name = "precio_historico", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioHistorico;

    @Column(name = "subtotal_historico", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalHistorico;

    @Column(nullable = false)
    private Integer cantidad;

    protected ItemPedido() {
    }

    public ItemPedido(Long productId, String nombreProducto, BigDecimal precioUnitario, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        if (precioUnitario == null || precioUnitario.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        this.productId = Objects.requireNonNull(productId);
        this.nombreProductoSnapshot = nombreProducto == null ? "Producto " + productId : nombreProducto;
        this.precioHistorico = precioUnitario;
        this.cantidad = cantidad;
        this.subtotalHistorico = calcularSubtotal();
    }

    void asignarOrden(Orden orden) {
        this.orden = orden;
    }

    public BigDecimal calcularSubtotal() {
        return precioHistorico.multiply(BigDecimal.valueOf(cantidad));
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getNombreProductoSnapshot() { return nombreProductoSnapshot; }
    public BigDecimal getPrecioHistorico() { return precioHistorico; }
    public BigDecimal getSubtotalHistorico() { return subtotalHistorico; }
    public Integer getCantidad() { return cantidad; }
}
