package com.ventasdiscos.mspostsale.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name="recepciones_devolucion")
@Getter
@Setter
public class RecepcionDevolucion {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="solicitud_id",nullable=false)
    @NotNull
    private SolicitudDevolucion solicitud;
    @NotNull
    @Column(nullable=false,updatable=false)
    private LocalDateTime fechaRecepcion;
    private String observaciones;
    @OneToMany(mappedBy="recepcion",cascade=CascadeType.ALL,orphanRemoval=true)
    private List<ItemRecepcionDevolucion> items=new ArrayList<>();
    @PrePersist

    protected void asignarFechaRecepcion() {
        if(fechaRecepcion==null)fechaRecepcion=LocalDateTime.now();
    }
}
