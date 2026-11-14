
package com.ventasdiscos.msnotifications.model;

import jakarta.validation.constraints.*;
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
// Modelo de dominio: representa los datos y las reglas básicas de integridad de una entidad. Clase EventoProcesado.
public class EventoProcesado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Identificador para identificar el registro.
    private Long id;

    @Column(name = "evento_id", nullable = false, length = 120)
    @NotBlank
    // Dato evento id del registro; validación: texto obligatorio y no vacío.
    private String eventoId;

    @Column(name = "origen", nullable = false, length = 40)
    // Dato origen del registro.
    private String origen;

    @Column(
        name = "fecha_procesamiento",
        nullable = false,
        updatable = false
    )
    @NotNull
    // Fecha u hora utilizada para registrar o controlar el proceso; validación: valor obligatorio (no nulo).
    private LocalDateTime fechaProcesamiento;

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método EventoProcesado (evento procesado).
    public EventoProcesado() {
    }

    @PrePersist
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método antesDeGuardar (antes de guardar).
    protected void antesDeGuardar() {
        if (fechaProcesamiento == null) {
            fechaProcesamiento = LocalDateTime.now();
        }
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getId (consultar id).
    public Long getId() {
        return id;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setId (actualizar id).
    public void setId(Long id) {
        this.id = id;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getEventoId (consultar evento id).
    public String getEventoId() {
        return eventoId;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setEventoId (actualizar evento id).
    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getOrigen (consultar origen).
    public String getOrigen() {
        return origen;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setOrigen (actualizar origen).
    public void setOrigen(String origen) {
        this.origen = origen;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getFechaProcesamiento (consultar fecha procesamiento).
    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setFechaProcesamiento (actualizar fecha procesamiento).
    public void setFechaProcesamiento(
            LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }
}
