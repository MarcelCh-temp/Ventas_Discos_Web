package com.ventasdiscos.mspostsale.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name="solicitudes_cancelacion")
@Getter
@Setter
public class SolicitudCancelacion extends SolicitudPostVenta {
    @jakarta.validation.constraints.NotBlank
    @Column(nullable=false)
    private String motivoSolicitud;
    private String motivoResolucion;
    private LocalDateTime fechaResolucion;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    @NotNull
    private EstadoCancelacion estado=EstadoCancelacion.PENDIENTE;

    public boolean estaPendiente() {
        return estado==EstadoCancelacion.PENDIENTE;
    }

    public void aprobar(String motivo) {
        if(!estaPendiente())throw new IllegalStateException("Solicitud ya resuelta");
        estado=EstadoCancelacion.APROBADA;
        motivoResolucion=motivo;
        fechaResolucion=LocalDateTime.now();
    }

    public void rechazar(String motivo) {
        if(!estaPendiente())throw new IllegalStateException("Solicitud ya resuelta");
        estado=EstadoCancelacion.RECHAZADA;
        motivoResolucion=motivo;
        fechaResolucion=LocalDateTime.now();
    }
}
