package com.ventasdiscos.mspostsale.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="solicitudes_reembolso")
@Getter
@Setter
public class SolicitudReembolso extends SolicitudPostVenta {
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    @NotNull
    private TipoOrigenReembolso tipoOrigen;
    @Column(nullable=false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long solicitudOrigenId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    @NotNull
    private EstadoReembolso estado=EstadoReembolso.REGISTRADA;

    public boolean estaRegistrada() {
        return estado==EstadoReembolso.REGISTRADA;
    }
}
