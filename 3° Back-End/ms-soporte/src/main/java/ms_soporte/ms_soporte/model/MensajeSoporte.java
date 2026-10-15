package ms_soporte.ms_soporte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes_soporte")
public class MensajeSoporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El contenido del mensaje no puede estar vacío")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolAutor autor;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    @JsonIgnore
    private Ticket ticket;

    @PrePersist
    protected void onCreate() {
        if (this.fechaEnvio == null) {
            this.fechaEnvio = LocalDateTime.now();
        }
    }

    public MensajeSoporte() {}

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

    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }

    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
}