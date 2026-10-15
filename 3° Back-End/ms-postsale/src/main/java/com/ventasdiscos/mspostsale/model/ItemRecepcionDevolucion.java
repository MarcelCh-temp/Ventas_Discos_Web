package com.ventasdiscos.mspostsale.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="items_recepcion_devolucion")
@Getter
@Setter
public class ItemRecepcionDevolucion {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="recepcion_id",nullable=false)
    @NotNull
    private RecepcionDevolucion recepcion;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="item_devolucion_id",nullable=false)
    @NotNull
    private ItemDevolucion itemDevolucion;
    @Column(nullable=false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Integer cantidadRecibida;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    @jakarta.validation.constraints.NotNull
    private CondicionProducto condicion;
    @OneToOne(cascade=CascadeType.ALL)
    private OperacionReposicion reposicion;

    public boolean tieneCantidadValida() {
        return cantidadRecibida!=null&&cantidadRecibida>0;
    }

    public boolean esAptaParaReposicion() {
        return condicion==CondicionProducto.BUENA;
    }
}
