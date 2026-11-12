package ms_soporte.ms_soporte.model;

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
    private Long id;

    // null solo en el primer registro (creación de la solicitud)
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private EstadoTicket estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false)
    private EstadoTicket estadoNuevo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    // Lote 1.1 (DOC-01 aprobado, RNF-09): quién hizo el cambio. Es el usuario autenticado (token):
    // el Cliente al crear la solicitud; el Administrador al pasarla a EN_PROCESO o cerrarla.
    // Columna nullable: los registros anteriores a este cambio quedan sin dato (no se borra nada).
    @Column(name = "usuario_responsable_id")
    private Long usuarioResponsableId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    public HistorialEstadoSoporte() {}

    public HistorialEstadoSoporte(EstadoTicket estadoAnterior, EstadoTicket estadoNuevo, Long usuarioResponsableId) {
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuarioResponsableId = Objects.requireNonNull(usuarioResponsableId,
                "Todo cambio de estado debe registrar su responsable (RNF-09)");
        this.fechaHora = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public EstadoTicket getEstadoAnterior() { return estadoAnterior; }
    public EstadoTicket getEstadoNuevo() { return estadoNuevo; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public Long getUsuarioResponsableId() { return usuarioResponsableId; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
}
