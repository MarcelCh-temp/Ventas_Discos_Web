package com.ventasdiscos.mspostsale.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

/**
 * Clase original (UML 04): cantidad y condición de un producto recibido. Solo una unidad BUENA origina una
 * OperacionReposicion hacia Catalog (RN-25: recibir no reintegra automáticamente). Lote 4: sin cambios de tabla.
 */
@Entity
@Table(name = "items_recepcion_devolucion")
@Getter
public class ItemRecepcionDevolucion {

    /** Formato ORIGINAL de PostSale, conservado por DOC-08 (prefijo POSTSALE- obligatorio en Catalog). */
    // Constante OPERACION_REPOSICION: valor compartido que no cambia durante la ejecución.
    public static final String OPERACION_REPOSICION = "POSTSALE-RECEPCION-%d";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Atributo id: propiedad que se guarda o utiliza en esta entidad.
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recepcion_id", nullable = false)
    // Atributo recepcion: propiedad que se guarda o utiliza en esta entidad.
    private RecepcionDevolucion recepcion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_devolucion_id", nullable = false)
    // Atributo itemDevolucion: propiedad que se guarda o utiliza en esta entidad.
    private ItemDevolucion itemDevolucion;

    @NotNull
    @Positive
    @Column(nullable = false)
    // Cantidad utilizada por la regla de negocio; sus límites se comprueban antes de guardar; validación: valor obligatorio (no nulo), número mayor que cero.
    private Integer cantidadRecibida;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    // Dato condicion del registro; validación: valor obligatorio (no nulo); enum persistido con las opciones permitidas.
    private CondicionProducto condicion;

    @OneToOne(cascade = CascadeType.ALL)
    // Dato reposicion del registro; relación JPA entre entidades del mismo microservicio.
    private OperacionReposicion reposicion;

    /** Para JPA. */
    // Ejecuta item recepcion devolucion como parte de las responsabilidades de esta clase.
    protected ItemRecepcionDevolucion() {
    }

    ItemRecepcionDevolucion(RecepcionDevolucion recepcion, ItemDevolucion itemDevolucion, Integer cantidadRecibida,
                            CondicionProducto condicion) {
        if (condicion == null) {
            throw new IllegalArgumentException("La condición del producto es obligatoria");
        }
        this.recepcion = recepcion;
        this.itemDevolucion = itemDevolucion;
        this.cantidadRecibida = cantidadRecibida;
        this.condicion = condicion;
    }

    // Ejecuta tiene cantidad valida como parte de las responsabilidades de esta clase.
    public boolean tieneCantidadValida() {
        return cantidadRecibida != null && cantidadRecibida > 0;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método esAptaParaReposicion (es apta para reposicion).
    public boolean esAptaParaReposicion() {
        return condicion == CondicionProducto.BUENA;
    }

    /**
     * Crea la operación de reposición (una sola vez) para una unidad apta. Necesita el id ya guardado porque la
     * operacionId lo usa: "POSTSALE-RECEPCION-{id}" es fija para este ítem y Catalog no repone dos veces con ella.
     */
    public OperacionReposicion prepararReposicion() {
        if (!esAptaParaReposicion() || reposicion != null) {
            return reposicion;
        }
        if (id == null) {
            throw new IllegalStateException("El ítem de recepción debe guardarse antes de preparar la reposición");
        }
        reposicion = new OperacionReposicion(String.format(OPERACION_REPOSICION, id), cantidadRecibida);
        return reposicion;
    }
}
