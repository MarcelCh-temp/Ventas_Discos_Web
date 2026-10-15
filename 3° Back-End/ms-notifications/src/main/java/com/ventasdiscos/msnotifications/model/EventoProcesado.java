
package com.ventasdiscos.msnotifications.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "eventos_notificacion_procesados",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_evento_origen",
            columnNames = {"origen", "evento_id"}
        )
    }
)
public class EventoProcesado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, length = 120)
    private String eventoId;

    @Column(name = "origen", nullable = false, length = 40)
    private String origen;

    @Column(
        name = "fecha_procesamiento",
        nullable = false,
        updatable = false
    )
    private LocalDateTime fechaProcesamiento;

    public EventoProcesado() {
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

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(
            LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }
}
