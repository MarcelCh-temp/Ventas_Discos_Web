package ms_soporte.ms_soporte.model;

import jakarta.validation.constraints.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Lote 1 (UML 05_MS-Support, HU-66, HU-67): registro de cada cambio de estado de una solicitud.
 * Tabla nueva "historial_estado_soporte" (la crea ddl-auto=update; no toca las tablas existentes).
 */
@Entity
@Table(name = "historial_estado_soporte")
public class HistorialEstadoSoporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Atributo id: propiedad que se guarda o utiliza en esta entidad.
    private Long id;

    // null solo en el primer registro (creación de la solicitud)
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private EstadoTicket estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false)
    @NotNull
    // Atributo estadoNuevo: propiedad que se guarda o utiliza en esta entidad.
    private EstadoTicket estadoNuevo;

    @Column(name = "fecha_hora", nullable = false)
    // Fecha u hora utilizada para registrar o controlar el proceso.
    private LocalDateTime fechaHora;

    // Lote 1.1 (DOC-01 aprobado, RNF-09): quién hizo el cambio. Es el usuario autenticado (token):
    // el Cliente al crear la solicitud; el Administrador al pasarla a EN_PROCESO o cerrarla.
    // Columna nullable: los registros anteriores a este cambio quedan sin dato (no se borra nada).
    @Column(name = "usuario_responsable_id")
    private Long usuarioResponsableId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    @NotNull
    // Atributo ticket: propiedad que se guarda o utiliza en esta entidad.
    private Ticket ticket;

    public HistorialEstadoSoporte() {}

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método HistorialEstadoSoporte (historial estado soporte).
    public HistorialEstadoSoporte(EstadoTicket estadoAnterior, EstadoTicket estadoNuevo, Long usuarioResponsableId) {
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuarioResponsableId = Objects.requireNonNull(usuarioResponsableId,
                "Todo cambio de estado debe registrar su responsable (RNF-09)");
        this.fechaHora = LocalDateTime.now();
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getId (consultar id).
    public Long getId() { return id; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getEstadoAnterior (consultar estado anterior).
    public EstadoTicket getEstadoAnterior() { return estadoAnterior; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getEstadoNuevo (consultar estado nuevo).
    public EstadoTicket getEstadoNuevo() { return estadoNuevo; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getFechaHora (consultar fecha hora).
    public LocalDateTime getFechaHora() { return fechaHora; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getUsuarioResponsableId (consultar usuario responsable id).
    public Long getUsuarioResponsableId() { return usuarioResponsableId; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getTicket (consultar ticket).
    public Ticket getTicket() { return ticket; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setTicket (actualizar ticket).
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
}
