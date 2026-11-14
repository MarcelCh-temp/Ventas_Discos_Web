package com.ventasdiscos.mspostsale.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

/**
 * Clase original (UML 04): producto y cantidad incluidos en una DEVOLUCION.
 * Lote 4: la solicitud ahora es la entidad única SolicitudPostVenta (antes SolicitudDevolucion). Misma tabla
 * {@code items_devolucion} y misma columna {@code solicitud_id}: como la herencia JOINED usaba el MISMO id en la tabla
 * base y en la hija, los valores guardados ya apuntan a filas de {@code solicitudes_postventa}. Solo cambia la clave
 * foránea (script opcional seguimiento/sql/postventa_4_claves_foraneas_OPCIONAL.sql, sin ejecutar).
 */
@Entity
@Table(name = "items_devolucion")
@Getter
public class ItemDevolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Atributo id: propiedad que se guarda o utiliza en esta entidad.
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    // Atributo solicitud: propiedad que se guarda o utiliza en esta entidad.
    private SolicitudPostVenta solicitud;

    @NotNull
    @Positive
    @Column(nullable = false)
    // Atributo productoId: propiedad que se guarda o utiliza en esta entidad.
    private Long productoId;

    @NotNull
    @Positive
    @Column(nullable = false)
    // Cantidad utilizada por la regla de negocio; sus límites se comprueban antes de guardar; validación: valor obligatorio (no nulo), número mayor que cero.
    private Integer cantidadSolicitada;

    /** Para JPA. */
    // Ejecuta item devolucion como parte de las responsabilidades de esta clase.
    protected ItemDevolucion() {
    }

    /** Solo la crea SolicitudPostVenta.devolucion. */
    ItemDevolucion(SolicitudPostVenta solicitud, Long productoId, Integer cantidadSolicitada) {
        this.solicitud = solicitud;
        this.productoId = productoId;
        this.cantidadSolicitada = cantidadSolicitada;
        if (productoId == null || productoId <= 0 || !tieneCantidadValida()) {
            throw new IllegalArgumentException("Producto y cantidad deben ser positivos");
        }
    }

    // Ejecuta tiene cantidad valida como parte de las responsabilidades de esta clase.
    public boolean tieneCantidadValida() {
        return cantidadSolicitada != null && cantidadSolicitada > 0;
    }
}
