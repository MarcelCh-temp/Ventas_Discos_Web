package ms_soporte.ms_soporte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes_soporte")
// Modelo de dominio: representa los datos y las reglas básicas de integridad de una entidad. Clase MensajeSoporte.
public class MensajeSoporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Identificador para identificar el registro.
    private Long id;

    @NotBlank(message = "El contenido del mensaje no puede estar vacío")
    @Column(columnDefinition = "TEXT", nullable = false)
    // Dato contenido del registro; validación: texto obligatorio y no vacío.
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    // Dato autor del registro; validación: valor obligatorio (no nulo); enum persistido con las opciones permitidas.
    private RolAutor autor;

    // Lote 1 (UML: autorId): quién escribió el mensaje (id del cliente o del administrador, desde el token).
    // Columna nueva y nullable: los mensajes antiguos no la tienen.
    @Column(name = "autor_id")
    private Long autorId;

    @Column(name = "fecha_envio", nullable = false)
    @NotNull
    // Atributo fechaEnvio: propiedad que se guarda o utiliza en esta entidad.
    private LocalDateTime fechaEnvio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    @JsonIgnore
    // Atributo ticket: propiedad que se guarda o utiliza en esta entidad.
    private Ticket ticket;

    @PrePersist
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método onCreate (on create).
    protected void onCreate() {
        if (this.fechaEnvio == null) {
            this.fechaEnvio = LocalDateTime.now();
        }
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método MensajeSoporte (mensaje soporte).
    public MensajeSoporte() {}

    /** Lote 1: mensaje con autor identificado (cliente o administrador). */
    // Ejecuta mensaje soporte como parte de las responsabilidades de esta clase.
    public MensajeSoporte(String contenido, RolAutor autor, Long autorId) {
        this.contenido = contenido;
        this.autor = autor;
        this.autorId = autorId;
        this.fechaEnvio = LocalDateTime.now();
    }

    // Ejecuta mensaje soporte como parte de las responsabilidades de esta clase.
    public MensajeSoporte(String contenido, RolAutor autor, Ticket ticket) {
        this.contenido = contenido;
        this.autor = autor;
        this.ticket = ticket;
        this.fechaEnvio = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public RolAutor getAutor() { return autor; }
    public void setAutor(RolAutor autor) { this.autor = autor; }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getAutorId (consultar autor id).
    public Long getAutorId() { return autorId; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setAutorId (actualizar autor id).
    public void setAutorId(Long autorId) { this.autorId = autorId; }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getFechaEnvio (consultar fecha envio).
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setFechaEnvio (actualizar fecha envio).
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getTicket (consultar ticket).
    public Ticket getTicket() { return ticket; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setTicket (actualizar ticket).
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
}