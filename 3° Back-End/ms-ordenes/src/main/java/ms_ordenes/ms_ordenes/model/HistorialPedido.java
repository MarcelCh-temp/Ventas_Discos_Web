package ms_ordenes.ms_ordenes.model;

import jakarta.validation.constraints.*;
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
    // Atributo id: propiedad que se guarda o utiliza en esta entidad.
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    @NotNull
    // Atributo orden: propiedad que se guarda o utiliza en esta entidad.
    private Orden orden;

    @Column(name = "fecha_hora", nullable = false)
    // Atributo fechaHora: propiedad que se guarda o utiliza en esta entidad.
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    // Dato estado anterior del registro; enum persistido con las opciones permitidas.
    private EstadoOrden estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    @NotNull
    // Dato estado nuevo del registro; validación: valor obligatorio (no nulo); enum persistido con las opciones permitidas.
    private EstadoOrden estadoNuevo;

    @Column(length = 255)
    // Dato motivo del registro.
    private String motivo;

    @Column(length = 500)
    // Dato descripcion del registro.
    private String descripcion;

    @Column(name = "usuario_responsable_id")
    // Dato usuario responsable id del registro.
    private Long usuarioResponsableId;

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método HistorialPedido (historial pedido).
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

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getId (consultar id).
    public Long getId() { return id; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getFechaHora (consultar fecha hora).
    public LocalDateTime getFechaHora() { return fechaHora; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getEstadoAnterior (consultar estado anterior).
    public EstadoOrden getEstadoAnterior() { return estadoAnterior; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getEstadoNuevo (consultar estado nuevo).
    public EstadoOrden getEstadoNuevo() { return estadoNuevo; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getMotivo (consultar motivo).
    public String getMotivo() { return motivo; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getDescripcion (consultar descripcion).
    public String getDescripcion() { return descripcion; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getUsuarioResponsableId (consultar usuario responsable id).
    public Long getUsuarioResponsableId() { return usuarioResponsableId; }
}
