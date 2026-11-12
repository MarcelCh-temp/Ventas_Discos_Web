package ms_ordenes.ms_ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * UML 02 HistorialPedido (Pedido "1" *-- "0..*" HistorialPedido). Lote 3 (RF-PED-07, HU-32, RNF-09).
 * Se crea SOLO desde Orden (cada cambio de estado, corrección o despacho) y no se modifica ni se borra.
 * usuarioResponsableId: quién lo hizo (del token). Null cuando lo hace el sistema o PostSale; entonces la
 * descripcion dice el origen.
 */
@Entity
@Table(name = "historial_pedido")
public class HistorialPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoOrden estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoOrden estadoNuevo;

    @Column(length = 255)
    private String motivo;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "usuario_responsable_id")
    private Long usuarioResponsableId;

    protected HistorialPedido() {
    }

    HistorialPedido(Orden orden, EstadoOrden estadoAnterior, EstadoOrden estadoNuevo, String motivo,
                    String descripcion, Long usuarioResponsableId) {
        this.orden = Objects.requireNonNull(orden);
        this.estadoNuevo = Objects.requireNonNull(estadoNuevo);
        this.estadoAnterior = estadoAnterior;
        this.motivo = motivo;
        this.descripcion = descripcion;
        this.usuarioResponsableId = usuarioResponsableId;
        this.fechaHora = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public EstadoOrden getEstadoAnterior() { return estadoAnterior; }
    public EstadoOrden getEstadoNuevo() { return estadoNuevo; }
    public String getMotivo() { return motivo; }
    public String getDescripcion() { return descripcion; }
    public Long getUsuarioResponsableId() { return usuarioResponsableId; }
}
