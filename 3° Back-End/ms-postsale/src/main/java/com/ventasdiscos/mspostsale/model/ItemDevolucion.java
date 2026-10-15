package com.ventasdiscos.mspostsale.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="items_devolucion")
@Getter
@Setter
public class ItemDevolucion {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="solicitud_id",nullable=false)
    @NotNull
    private SolicitudDevolucion solicitud;
    @Column(nullable=false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long productoId;
    @Column(nullable=false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Integer cantidadSolicitada;

    public boolean tieneCantidadValida() {
        return cantidadSolicitada!=null&&cantidadSolicitada>0;
    }
}
