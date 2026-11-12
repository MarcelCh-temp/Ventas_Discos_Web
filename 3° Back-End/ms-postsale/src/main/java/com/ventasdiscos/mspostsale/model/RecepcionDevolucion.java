package com.ventasdiscos.mspostsale.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase original (UML 04): recepción física de productos de una devolución APROBADA (puede haber varias parciales).
 * Lote 4: la solicitud ahora es SolicitudPostVenta (misma tabla y columna; ver ItemDevolucion).
 * DOC-09 (aprobada, RNF-09): {@code usuarioRecepcionId} = Administrador (del JWT) que registró la recepción.
 */
@Entity
@Table(name = "recepciones_devolucion")
@Getter
public class RecepcionDevolucion {

    public static final int MAX_OBSERVACIONES = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudPostVenta solicitud;

    @NotNull
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRecepcion;

    private String observaciones;

    /** DOC-09. Columna nueva sin NOT NULL: las recepciones anteriores al Lote 4 no tienen ese dato (quedan null). */
    @Column(name = "usuario_recepcion_id", updatable = false)
    private Long usuarioRecepcionId;

    @OneToMany(mappedBy = "recepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemRecepcionDevolucion> items = new ArrayList<>();

    /** Para JPA. */
    protected RecepcionDevolucion() {
    }

    /** Solo la crea SolicitudPostVenta.registrarRecepcion (que exige una devolución APROBADA). */
    RecepcionDevolucion(SolicitudPostVenta solicitud, String observaciones, Long usuarioRecepcionId) {
        if (usuarioRecepcionId == null) {
            throw new IllegalArgumentException("El responsable de la recepción es obligatorio (RNF-09)");
        }
        this.solicitud = solicitud;
        this.observaciones = observaciones == null || observaciones.isBlank() ? null : observaciones.trim();
        this.usuarioRecepcionId = usuarioRecepcionId;
    }

    /**
     * Agrega un producto recibido. El Service ya validó (con el detalle por campo) que el ítem es de esta devolución,
     * que no se repite y que no supera lo pendiente; aquí se protege la misma regla por si se llama de otra forma.
     */
    public ItemRecepcionDevolucion agregarItem(ItemDevolucion itemDevolucion, int cantidad, CondicionProducto condicion) {
        SolicitudPostVenta duena = itemDevolucion.getSolicitud();
        if (duena != solicitud && (duena == null || duena.getId() == null || !duena.getId().equals(solicitud.getId()))) {
            throw new IllegalArgumentException("El producto no pertenece a esta devolución");
        }
        if (cantidad <= 0 || cantidad > solicitud.pendienteDeRecibir(itemDevolucion)) {
            throw new IllegalArgumentException("La cantidad recibida supera lo autorizado");
        }
        ItemRecepcionDevolucion i = new ItemRecepcionDevolucion(this, itemDevolucion, cantidad, condicion);
        items.add(i);
        return i;
    }

    @PrePersist
    protected void asignarFechaRecepcion() {
        if (fechaRecepcion == null) {
            fechaRecepcion = LocalDateTime.now();
        }
    }
}
