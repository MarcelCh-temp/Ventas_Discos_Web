package com.ventasdiscos.mspostsale.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

/**
 * Clase original (UML 04): reposición en Catalog de una unidad devuelta apta. {@code operacionId} es la clave de
 * idempotencia (única aquí y en Catalog): reintentar nunca repone dos veces.
 * PENDIENTE = Catalog todavía no confirmó (no respondió): se reintenta con la misma operacionId.
 * CONFIRMADA = Catalog la aplicó (201) o ya la tenía (200).
 * FALLIDA = Catalog la rechazó (400/404/409): no se reintenta sola; el Administrador revisa el stock en Catalog.
 * Lote 4: sin cambios de tabla; solo se quitó el setter libre para que el estado cambie únicamente por sus métodos.
 */
@Entity
@Table(name = "operaciones_reposicion", uniqueConstraints = @UniqueConstraint(columnNames = "operacion_id"))
@Getter
public class OperacionReposicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Atributo id: propiedad que se guarda o utiliza en esta entidad.
    private Long id;

    @NotBlank
    @Column(name = "operacion_id", nullable = false, unique = true, length = 100)
    // Atributo operacionId: propiedad que se guarda o utiliza en esta entidad.
    private String operacionId;

    @NotNull
    @Positive
    @Column(nullable = false)
    // Atributo cantidad: propiedad que se guarda o utiliza en esta entidad.
    private Integer cantidad;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    // Dato estado del registro; validación: valor obligatorio (no nulo); enum persistido con las opciones permitidas.
    private EstadoReposicion estado = EstadoReposicion.PENDIENTE;

    /** Para JPA. */
    // Ejecuta operacion reposicion como parte de las responsabilidades de esta clase.
    protected OperacionReposicion() {
    }

    OperacionReposicion(String operacionId, Integer cantidad) {
        this.operacionId = operacionId;
        this.cantidad = cantidad;
    }

    /** Solo desde PENDIENTE: una operación ya FALLIDA no cambia sola (la revisa el Administrador). */
    // Ejecuta confirmar como parte de las responsabilidades de esta clase.
    public void confirmar() {
        if (estado == EstadoReposicion.PENDIENTE) {
            estado = EstadoReposicion.CONFIRMADA;
        }
    }

    // Ejecuta marcar fallida como parte de las responsabilidades de esta clase.
    public void marcarFallida() {
        if (estado == EstadoReposicion.PENDIENTE) {
            estado = EstadoReposicion.FALLIDA;
        }
    }

    // Ejecuta esta pendiente como parte de las responsabilidades de esta clase.
    public boolean estaPendiente() {
        return estado == EstadoReposicion.PENDIENTE;
    }
}
