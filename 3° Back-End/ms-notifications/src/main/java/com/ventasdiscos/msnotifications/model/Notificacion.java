
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
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El usuario destinatario es obligatorio")
    @Positive(message = "El identificador del usuario debe ser positivo")
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @NotBlank(message = "El titulo es obligatorio")
    @Column(nullable = false)
    private String titulo;

    @NotBlank(message = "El mensaje es obligatorio")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @NotNull(message = "El tipo de notificacion es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    @NotNull(message = "La fecha de creacion es obligatoria")
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @NotNull(message = "El estado de lectura es obligatorio")
    @Column(nullable = false)
    private Boolean leida = false;

    @Positive(message = "El identificador de referencia debe ser positivo")
    @Column(name = "referencia_id")
    private Long referenciaId;

    public Notificacion() {
    }

    @PrePersist
    protected void antesDeGuardar() {

        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }

        if (leida == null) {
            leida = false;
        }
    }

    public void marcarComoLeida() {
        this.leida = true;
    }

    public boolean estaLeida() {
        return Boolean.TRUE.equals(this.leida);
    }

    public boolean perteneceA(Long usuarioId) {
        return usuarioId != null
                && Objects.equals(this.usuarioId, usuarioId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoNotificacion tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Boolean getLeida() {
        return leida;
    }

    public void setLeida(Boolean leida) {
        this.leida = leida;
    }

    public Long getReferenciaId() {
        return referenciaId;
    }

    public void setReferenciaId(Long referenciaId) {
        this.referenciaId = referenciaId;
    }
}
