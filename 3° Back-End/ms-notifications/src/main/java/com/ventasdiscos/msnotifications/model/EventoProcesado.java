
package com.ventasdiscos.msnotifications.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "eventos_notificacion_procesados",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_evento_origen",
                columnNames = {"origen", "evento_id"}
        )
)
public class EventoProcesado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "evento_id",
            nullable = false,
            length = 120
    )
    private String eventoId;

    @Column(nullable = false, length = 40)
    private String origen;

    @Column(
            name = "fecha_procesamiento",
            nullable = false,
            updatable = false
    )
    private LocalDateTime fechaProcesamiento;

    public EventoProcesado() {
    }

    public EventoProcesado(String eventoId, String origen) {
        this.eventoId = eventoId;
        this.origen = origen;
    }

    @PrePersist
    protected void antesDeGuardar() {
        if (fechaProcesamiento == null) {
            fechaProcesamiento = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getEventoId() {
        return eventoId;
    }

    public String getOrigen() {
        return origen;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }
}
