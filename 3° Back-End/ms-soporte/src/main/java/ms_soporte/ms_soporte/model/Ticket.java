package ms_soporte.ms_soporte.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "tickets")
// Modelo de dominio: representa los datos y las reglas básicas de integridad de una entidad. Clase Ticket.
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Identificador para identificar el registro.
    private Long id;

    // Lote 1 (HU-59): número visible del ticket (ej. SOP-000012). Se asigna una sola vez con asignarNumeroTicket().
    // Columna nueva y nullable: los tickets antiguos de bd_soporte siguen funcionando.
    @Column(name = "numero_ticket", unique = true, length = 20)

    private String numeroTicket;

    // Lote 1: lo pone el Service con el usuario del token, nunca el cliente.
    @NotNull(message = "El idUsuario es obligatorio")
    @Column(name = "id_usuario", nullable = false)
    @Positive
    private Long idUsuario;

    @Column(name = "order_id")
    private Long orderId; // Asociado opcionalmente a un pedido

    @NotBlank(message = "El asunto no puede estar vacío")
    @Column(nullable = false)
    // Dato asunto del registro; validación: texto obligatorio y no vacío.
    private String asunto;

    @NotBlank(message = "La descripción no puede estar vacía")
    @Column(columnDefinition = "TEXT", nullable = false)
    // Dato descripcion del registro; validación: texto obligatorio y no vacío.
    private String descripcion;

    @Column(nullable = false)
    // Dato categoria del registro.
    private String categoria = "GENERAL";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    // Dato estado del registro; validación: valor obligatorio (no nulo); enum persistido con las opciones permitidas.
    private EstadoTicket estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    // Fecha u hora utilizada para registrar o controlar el proceso.
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_cierre")
    // Fecha u hora utilizada para registrar o controlar el proceso.
    private LocalDateTime fechaCierre;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaEnvio ASC, id ASC")
    private List<MensajeSoporte> mensajes = new ArrayList<>();

    // Lote 1 (HU-66, HU-67, UML 05_MS-Support): historial de cambios de estado.
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaHora ASC, id ASC")
    private List<HistorialEstadoSoporte> historial = new ArrayList<>();

    @PrePersist
    // Ejecuta on create como parte de las responsabilidades de esta clase.
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoTicket.ABIERTA;
        }
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método Ticket (ticket).
    public Ticket() {}

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método agregarMensaje (agregar mensaje).
    public void agregarMensaje(MensajeSoporte mensaje) {
        mensajes.add(mensaje);
        mensaje.setTicket(this);
    }

    // ---- Lote 1: reglas propias del ticket (UML: estaCerrada, iniciarAtencion, cerrar) ----

    /** RN-30: una solicitud CERRADA no acepta respuestas ni más cambios. */
    // Ejecuta esta cerrada como parte de las responsabilidades de esta clase.
    public boolean estaCerrada() {
        return this.estado == EstadoTicket.CERRADA;
    }

    /**
     * HU-66: único cambio intermedio permitido, ABIERTA -> EN_PROCESO.
     * Lote 1.1 (DOC-01): recibe el responsable (el Administrador autenticado) para el historial (RNF-09).
     */
    public void iniciarAtencion(Long usuarioResponsableId) {
        if (this.estado != EstadoTicket.ABIERTA) {
            throw new IllegalStateException("Solo una solicitud ABIERTA puede pasar a EN_PROCESO (estado actual: " + this.estado + ")");
        }
        cambiarEstado(EstadoTicket.EN_PROCESO, usuarioResponsableId);
    }

    /**
     * HU-67: cierre con fecha. Si ya está cerrada no se repite la operación.
     * Lote 1.1 (DOC-01): recibe el responsable (el Administrador autenticado) para el historial (RNF-09).
     */
    public void cerrar(Long usuarioResponsableId) {
        if (estaCerrada()) {
            throw new IllegalStateException("La solicitud ya está cerrada");
        }
        cambiarEstado(EstadoTicket.CERRADA, usuarioResponsableId);
        this.fechaCierre = LocalDateTime.now();
    }

    /** Cambia el estado y deja registro en el historial (HU-66: cada cambio queda registrado). */
    // Ejecuta cambiar estado como parte de las responsabilidades de esta clase.
    private void cambiarEstado(EstadoTicket nuevo, Long usuarioResponsableId) {
        HistorialEstadoSoporte registro = new HistorialEstadoSoporte(this.estado, nuevo, usuarioResponsableId);
        registro.setTicket(this);
        historial.add(registro);
        this.estado = nuevo;
    }

    /**
     * Registra el estado inicial (null -> ABIERTA) al crear la solicitud.
     * Lote 1.1: solo para una solicitud nueva; así no se puede "reiniciar" una solicitud existente o cerrada.
     * Lote 1.1 (DOC-01): el responsable es el Cliente autenticado que crea la solicitud (RNF-09).
     */
    public void registrarEstadoInicial(Long usuarioResponsableId) {
        if (this.estado != null) {
            throw new IllegalStateException("La solicitud ya tiene estado (" + this.estado + ")");
        }
        // primero el registro (valida el responsable); recién después cambia el estado: nunca queda a medias
        HistorialEstadoSoporte registro = new HistorialEstadoSoporte(null, EstadoTicket.ABIERTA, usuarioResponsableId);
        registro.setTicket(this);
        historial.add(registro);
        this.estado = EstadoTicket.ABIERTA;
    }

    /**
     * Lote 1.1 (HU-59): número visible derivado del id (único porque el id es único).
     * Se asigna una sola vez, después del primer guardado (cuando ya existe el id).
     */
    public void asignarNumeroTicket() {
        if (this.id == null) {
            throw new IllegalStateException("El número se asigna después de guardar la solicitud");
        }
        if (this.numeroTicket == null) {
            this.numeroTicket = String.format("SOP-%06d", this.id);
        }
    }

    // Ejecuta pertenece a como parte de las responsabilidades de esta clase.
    public boolean perteneceA(Long usuarioId) {
        return this.idUsuario != null && this.idUsuario.equals(usuarioId);
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    // Lote 1.1: sin setNumeroTicket: el número solo se asigna con asignarNumeroTicket().
    public String getNumeroTicket() { return numeroTicket; }

    // Lote 1.1: lista de solo lectura. El historial solo crece con los métodos de estado (RN-32: no se borra).
    public List<HistorialEstadoSoporte> getHistorial() { return Collections.unmodifiableList(historial); }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getAsunto() { return asunto; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setAsunto (actualizar asunto).
    public void setAsunto(String asunto) { this.asunto = asunto; }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getDescripcion (consultar descripcion).
    public String getDescripcion() { return descripcion; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setDescripcion (actualizar descripcion).
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getCategoria (consultar categoria).
    public String getCategoria() { return categoria; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método setCategoria (actualizar categoria).
    public void setCategoria(String categoria) { this.categoria = categoria; }

    // Lote 1.1: sin setEstado: el estado solo cambia con registrarEstadoInicial(), iniciarAtencion() y cerrar(),
    // que validan la transición y registran el historial (HU-66, HU-67). JPA no lo necesita: usa los campos.
    public EstadoTicket getEstado() { return estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    // Lote 1.1: sin setFechaCierre: la fecha la pone cerrar() (HU-67).
    public LocalDateTime getFechaCierre() { return fechaCierre; }

    // Lote 1.1: sin setMensajes y lista de solo lectura. Con orphanRemoval, reemplazar o vaciar la lista
    // BORRARÍA la conversación en la BD (RN-32). Los mensajes solo se agregan con agregarMensaje().
    public List<MensajeSoporte> getMensajes() { return Collections.unmodifiableList(mensajes); }
}