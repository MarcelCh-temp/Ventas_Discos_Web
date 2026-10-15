package com.ventasdiscos.msinventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Historial automático de cada cambio de stock (HU-43, RNF-09, RN-32: no se elimina).
 * operacionId: clave de idempotencia de llamadas de otros MS (Orders/PostSale);
 * UNIQUE (operacion_id, inventario_id) impide aplicar dos veces la misma operación.
 */
@Entity
@Table(name = "movimiento_stock",
        uniqueConstraints = @UniqueConstraint(name = "uk_mov_operacion_inventario",
                columnNames = {"operacion_id", "inventario_id"}),
        indexes = @Index(name = "idx_mov_fecha", columnList = "fecha_hora"))
@Getter
@Setter
@NoArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Relación JPA válida: ambas entidades viven en MS-Inventory. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventario_id", nullable = false)
    private Inventario inventario;

    @Column(name = "operacion_id", length = 100)
    private String operacionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    /** Para ENTRADA/SALIDA: unidades movidas. Para AJUSTE: nuevo valor contado. */
    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Integer stockAnterior;

    @Column(nullable = false)
    private Integer stockResultante;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(length = 255)
    private String motivo;

    /** Quién lo hizo (Admin). Null si viene de otro MS (queda operacionId). */
    private Long usuarioId;
}
