package com.ventasdiscos.mscatalog.model;

import com.ventasdiscos.mscatalog.exception.StockInsuficienteException;
import com.ventasdiscos.mscatalog.exception.ValidacionNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Existencias de UN producto (UML 03: Inventario; RN-42: Catalog es la fuente de verdad del stock).
 * Lote 2: trasladado desde ms-inventory. Cambios respecto del original:
 *  - Relación real con Producto (UML 03: Producto "1" *-- "1" Inventario). Antes productId era un id externo.
 *    Se conserva la columna original "product_id" (misma tabla "inventario") para migrar los datos sin transformar.
 *  - Los métodos del UML 03 (hayStockSuficiente, aumentarStock, descontarStock, ajustarStock, estaAgotado,
 *    estaEnNivelCritico) protegen la regla "stock nunca negativo" (RN-10) en la propia entidad.
 *    Ya no hay setter público de stockActual: el stock solo cambia con esos métodos.
 */
@Entity
@Table(name = "inventario", uniqueConstraints =
        @UniqueConstraint(name = "uk_inventario_product", columnNames = "product_id"))
@Getter
@NoArgsConstructor
public class Inventario {

    /** Revisión Lote 2: stock_actual es int; ninguna entrada puede dejarlo sobre este valor. */
    public static final int STOCK_MAXIMO = Integer.MAX_VALUE;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UML 03: un inventario por producto. Solo lectura: la columna la escribe productId. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, insertable = false, updatable = false)
    private Producto producto;

    /** Columna original de ms-inventory. Las consultas por id siguen igual que en el código original. */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** RN-10/RN-11: entero, nunca negativo. */
    @Column(nullable = false)
    private Integer stockActual;

    /** HU-42: umbral configurado por el Admin (entero >= 0). */
    @Column(nullable = false)
    private Integer umbralCritico;

    /** Producto nuevo: stock 0 (Agotado, RN-09) y umbral por defecto. */
    public Inventario(Producto producto, int umbralCritico) {
        this.producto = producto;
        this.productId = producto.getId();
        this.stockActual = 0;
        configurarUmbral(umbralCritico);
    }

    // ===================== UML 03 =====================

    public boolean hayStockSuficiente(int cantidad) {
        return cantidad >= 0 && stockActual >= cantidad;
    }

    /** Revisión Lote 2: si la suma no cabe en int, se rechaza y el stock no cambia (antes quedaba negativo). */
    public void aumentarStock(int cantidad) {
        validarCantidadPositiva(cantidad);
        if ((long) this.stockActual + cantidad > STOCK_MAXIMO) {
            throw new ValidacionNegocioException("La entrada dejaría el stock del producto " + productId
                    + " sobre el máximo permitido (" + STOCK_MAXIMO + ")");
        }
        this.stockActual = this.stockActual + cantidad;
    }

    /** RN-10: una salida mayor que el stock se rechaza y el stock no cambia. */
    public void descontarStock(int cantidad) {
        validarCantidadPositiva(cantidad);
        if (!hayStockSuficiente(cantidad)) {
            throw new StockInsuficienteException("Stock insuficiente para el producto " + productId
                    + ": disponible " + stockActual + ", solicitado " + cantidad);
        }
        this.stockActual = this.stockActual - cantidad;
    }

    /** HU-40: fija el valor contado físicamente (entero >= 0). */
    public void ajustarStock(int nuevoStock) {
        if (nuevoStock < 0) {
            throw new ValidacionNegocioException("El stock no puede ser negativo");
        }
        this.stockActual = nuevoStock;
    }

    public boolean estaAgotado() {
        return stockActual == 0;
    }

    /** RN-09: 0 < stock <= umbral. */
    public boolean estaEnNivelCritico() {
        return stockActual > 0 && stockActual <= umbralCritico;
    }

    // ===================== apoyo =====================

    /** RN-09: 0 -> Agotado; 0 < stock <= umbral -> Crítico; stock > umbral -> Disponible. */
    public Disponibilidad getDisponibilidad() {
        if (estaAgotado()) {
            return Disponibilidad.AGOTADO;
        }
        return estaEnNivelCritico() ? Disponibilidad.STOCK_CRITICO : Disponibilidad.DISPONIBLE;
    }

    /** Aplica un movimiento usando los métodos del UML (lo usa InventarioService al registrar el historial). */
    public void aplicar(TipoMovimiento tipo, int cantidad) {
        switch (tipo) {
            case ENTRADA -> aumentarStock(cantidad);
            case SALIDA -> descontarStock(cantidad);
            case AJUSTE -> ajustarStock(cantidad);
        }
    }

    /** HU-42: el Admin define el umbral (entero >= 0); la disponibilidad se recalcula sola. */
    public void configurarUmbral(int umbralCritico) {
        if (umbralCritico < 0) {
            throw new ValidacionNegocioException("El umbral no puede ser negativo");
        }
        this.umbralCritico = umbralCritico;
    }

    private static void validarCantidadPositiva(int cantidad) {
        if (cantidad <= 0) {
            throw new ValidacionNegocioException("La cantidad debe ser mayor que cero");
        }
    }
}
