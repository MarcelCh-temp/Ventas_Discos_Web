package ms_ordenes.ms_ordenes.model;

import jakarta.validation.constraints.*;
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
    // Atributo id: propiedad que se guarda o utiliza en esta entidad.
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    @NotNull
    // Atributo orden: propiedad que se guarda o utiliza en esta entidad.
    private Orden orden;

    @Column(name = "product_id", nullable = false)
    @Positive
    @NotNull
    // Atributo productId: propiedad que se guarda o utiliza en esta entidad.
    private Long productId;

    @Column(name = "nombre_producto_snapshot", nullable = false, length = 150)
    @NotBlank
    // Dato nombre producto snapshot del registro; validación: texto obligatorio y no vacío.
    private String nombreProductoSnapshot;

    @Column(name = "precio_historico", nullable = false, precision = 12, scale = 2)
    @DecimalMin(value = "0.0")
    @NotNull
    // Importe monetario; se conserva la precisión decimal para evitar errores de redondeo; validación: valor obligatorio (no nulo), límite inferior decimal.
    private BigDecimal precioHistorico;

    @Column(name = "subtotal_historico", nullable = false, precision = 12, scale = 2)
    @NotNull
    // Importe monetario; se conserva la precisión decimal para evitar errores de redondeo; validación: valor obligatorio (no nulo).
    private BigDecimal subtotalHistorico;

    @Column(nullable = false)
    @Positive
    @NotNull
    // Cantidad utilizada por la regla de negocio; sus límites se comprueban antes de guardar; validación: valor obligatorio (no nulo), número mayor que cero.
    private Integer cantidad;

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método ItemPedido (item pedido).
    protected ItemPedido() {
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método ItemPedido (item pedido).
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

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método calcularSubtotal (calcular subtotal).
    public BigDecimal calcularSubtotal() {
        return precioHistorico.multiply(BigDecimal.valueOf(cantidad));
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getId (consultar id).
    public Long getId() { return id; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getProductId (consultar product id).
    public Long getProductId() { return productId; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getNombreProductoSnapshot (consultar nombre producto snapshot).
    public String getNombreProductoSnapshot() { return nombreProductoSnapshot; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getPrecioHistorico (consultar precio historico).
    public BigDecimal getPrecioHistorico() { return precioHistorico; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getSubtotalHistorico (consultar subtotal historico).
    public BigDecimal getSubtotalHistorico() { return subtotalHistorico; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getCantidad (consultar cantidad).
    public Integer getCantidad() { return cantidad; }
}
