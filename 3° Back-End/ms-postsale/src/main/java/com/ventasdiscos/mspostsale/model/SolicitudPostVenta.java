package com.ventasdiscos.mspostsale.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name="solicitudes_postventa")
@Inheritance(strategy=InheritanceType.JOINED)
@Getter
@Setter
public abstract class SolicitudPostVenta {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long pedidoId;
    @Column(nullable=false)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long usuarioId;
    @Column(nullable=false,updatable=false)
    @NotNull
    private LocalDateTime fechaSolicitud;
    @PrePersist

    protected void crear() {
        if(fechaSolicitud==null)fechaSolicitud=LocalDateTime.now();
    }

    public boolean perteneceA(Long idUsuario) {
        return usuarioId!=null&&usuarioId.equals(idUsuario);
    }
}
