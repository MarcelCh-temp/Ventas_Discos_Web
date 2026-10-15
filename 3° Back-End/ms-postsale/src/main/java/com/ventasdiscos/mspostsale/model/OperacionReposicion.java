package com.ventasdiscos.mspostsale.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table
(name="operaciones_reposicion",uniqueConstraints=@UniqueConstraint(columnNames="operacion_id"))
@Getter
@Setter
public class OperacionReposicion {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="operacion_id",nullable=false,unique=true,length=100)
    @NotBlank
    private String operacionId;
    @Column(nullable=false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Integer cantidad;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    @NotNull
    private EstadoReposicion estado=EstadoReposicion.PENDIENTE;

    public void confirmar() {
        estado=EstadoReposicion.CONFIRMADA;
    }

    public void marcarFallida() {
        estado=EstadoReposicion.FALLIDA;
    }

    public boolean estaPendiente() {
        return estado==EstadoReposicion.PENDIENTE;
    }
}
