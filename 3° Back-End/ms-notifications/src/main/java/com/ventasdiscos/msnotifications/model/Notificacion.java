
package com.ventasdiscos.msnotifications.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notificaciones")
// Modelo de dominio: representa los datos y las reglas básicas de integridad de una entidad. Clase Notificacion.
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Identificador para identificar el registro.
    private Long id;

    @NotNull(message = "El usuario destinatario es obligatorio")
    @Positive(message = "El identificador del usuario debe ser positivo")
    @Column(name = "usuario_id", nullable = false)
    // Dato usuario id del registro; validación: valor obligatorio (no nulo), número mayor que cero.
    private Long usuarioId;

    @NotBlank(message = "El titulo es obligatorio")
    @Column(nullable = false)
    // Dato titulo del registro; validación: texto obligatorio y no vacío.
    private String titulo;

    @NotBlank(message = "El mensaje es obligatorio")
    @Column(nullable = false, columnDefinition = "TEXT")
    // Dato mensaje del registro; validación: texto obligatorio y no vacío.
    private String mensaje;

    @NotNull(message = "El tipo de notificacion es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    // Dato tipo del registro; validación: valor obligatorio (no nulo); enum persistido con las opciones permitidas.
    private TipoNotificacion tipo;

    @NotNull(message = "La fecha de creacion es obligatoria")
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    // Fecha u hora utilizada para registrar o controlar el proceso; validación: valor obligatorio (no nulo).
    private LocalDateTime fechaCreacion;

    @NotNull(message = "El estado de lectura es obligatorio")
    @Column(nullable = false)
    // Dato leida del registro; validación: valor obligatorio (no nulo).
    private Boolean leida = false;

    @Positive(message = "El identificador de referencia debe ser positivo")
    @Column(name = "referencia_id")
    // Dato referencia id del registro; validación: número mayor que cero.
    private Long referenciaId;

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método Notificacion (notificacion).
    public Notificacion() {
    }

    @PrePersist
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método antesDeGuardar (antes de guardar).
    protected void antesDeGuardar() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }

        if (leida == null) {
            leida = false;
        }
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método marcarComoLeida (marcar como leida).
    public void marcarComoLeida() {
        this.leida = true;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método estaLeida (esta leida).
    public boolean estaLeida() {
        return Boolean.TRUE.equals(this.leida);
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método perteneceA (pertenece a).
    public boolean perteneceA(Long usuarioId) {
        return usuarioId != null
                && Objects.equals(this.usuarioId, usuarioId);
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getId (consultar id).
    public Long getId() {
        return id;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setId (actualizar id).
    public void setId(Long id) {
        this.id = id;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getUsuarioId (consultar usuario id).
    public Long getUsuarioId() {
        return usuarioId;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setUsuarioId (actualizar usuario id).
    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getTitulo (consultar titulo).
    public String getTitulo() {
        return titulo;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setTitulo (actualizar titulo).
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getMensaje (consultar mensaje).
    public String getMensaje() {
        return mensaje;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setMensaje (actualizar mensaje).
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getTipo (consultar tipo).
    public TipoNotificacion getTipo() {
        return tipo;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setTipo (actualizar tipo).
    public void setTipo(TipoNotificacion tipo) {
        this.tipo = tipo;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getFechaCreacion (consultar fecha creacion).
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setFechaCreacion (actualizar fecha creacion).
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getLeida (consultar leida).
    public Boolean getLeida() {
        return leida;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setLeida (actualizar leida).
    public void setLeida(Boolean leida) {
        this.leida = leida;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getReferenciaId (consultar referencia id).
    public Long getReferenciaId() {
        return referenciaId;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setReferenciaId (actualizar referencia id).
    public void setReferenciaId(Long referenciaId) {
        this.referenciaId = referenciaId;
    }
}
