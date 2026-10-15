package com.ventasdiscos.msinventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Existencias de UN producto (fuente de verdad del stock, ADR-11).
 * productId es ID externo de MS-Catalog: sin @ManyToOne entre MS (ADR-09).
 */
@Entity
@Table(name = "inventario", uniqueConstraints =
        @UniqueConstraint(name = "uk_inventario_product", columnNames = "product_id"))
@Getter
@Setter
@NoArgsConstructor
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** RN-10/RN-11: entero, nunca negativo. */
    @Column(nullable = false)
    private Integer stockActual;

    /** HU-42: umbral configurado por el Admin (entero >= 0). */
    @Column(nullable = false)
    private Integer umbralCritico;

    public Inventario(Long productId, int umbralCritico) {
        this.productId = productId;
        this.stockActual = 0;
        this.umbralCritico = umbralCritico;
    }

    /** RN-09: 0 -> Agotado; 0 < stock <= umbral -> Crítico; stock > umbral -> Disponible. */
    public Disponibilidad getDisponibilidad() {
        if (stockActual == 0) {
            return Disponibilidad.AGOTADO;
        }
        return stockActual <= umbralCritico ? Disponibilidad.STOCK_CRITICO : Disponibilidad.DISPONIBLE;
    }

    /** Calcula el stock que resultaría SIN aplicarlo (para validar lotes antes de ejecutar). */
    public int calcularResultado(TipoMovimiento tipo, int cantidad) {
        return switch (tipo) {
            case ENTRADA -> stockActual + cantidad;
            case SALIDA -> stockActual - cantidad;
            case AJUSTE -> cantidad;
        };
    }
}
