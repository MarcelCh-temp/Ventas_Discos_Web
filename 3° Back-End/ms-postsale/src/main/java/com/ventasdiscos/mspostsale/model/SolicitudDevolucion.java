package com.ventasdiscos.mspostsale.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name="solicitudes_devolucion")
@Getter
@Setter
public class SolicitudDevolucion extends SolicitudPostVenta {
    @jakarta.validation.constraints.NotBlank
    @Column(nullable=false)
    private String motivoSolicitud;
    private String motivoResolucion;
    private LocalDateTime fechaResolucion;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    @NotNull
    private EstadoDevolucion estado=EstadoDevolucion.PENDIENTE;
    @OneToMany(mappedBy="solicitud",cascade=CascadeType.ALL,orphanRemoval=true)
    private List<ItemDevolucion> items=new ArrayList<>();
    @OneToMany(mappedBy="solicitud",cascade=CascadeType.ALL)
    private List<RecepcionDevolucion> recepciones=new ArrayList<>();

    public boolean estaAprobada() {
        return estado==EstadoDevolucion.APROBADA;
    }

    public void aprobar(String motivo) {
        if(estado!=EstadoDevolucion.PENDIENTE)throw new IllegalStateException("Solicitud ya resuelta");
        estado=EstadoDevolucion.APROBADA;
        motivoResolucion=motivo;
        fechaResolucion=LocalDateTime.now();
    }

    public void rechazar(String motivo) {
        if(estado!=EstadoDevolucion.PENDIENTE)throw new IllegalStateException("Solicitud ya resuelta");
        estado=EstadoDevolucion.RECHAZADA;
        motivoResolucion=motivo;
        fechaResolucion=LocalDateTime.now();
    }
}
