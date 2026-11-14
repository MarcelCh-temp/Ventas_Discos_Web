package ms_ordenes.ms_ordenes.model;

import jakarta.validation.constraints.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import ms_ordenes.ms_ordenes.exception.OperacionNoPermitidaException;
import ms_ordenes.ms_ordenes.exception.ValidacionNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * UML 02 Pedido ≡ Orden (clase y tabla "ordenes" originales conservadas; equivalencia aprobada en el Lote 3).
 * Equivalencias de atributos: userId ≡ idUsuario (columna id_usuario), fechaCompra ≡ fechaCreacion
 * (columna fecha_creacion). Columnas nuevas (nullable, las agrega ddl-auto=update sin tocar las filas antiguas):
 * numero_pedido, descuento_historico, datos_entrega_id y despacho_id.
 * Lote 3: sin setters de estado, total ni dueño. El estado SOLO cambia con cambiarEstado() y cada cambio queda
 * en el historial (RF-PED-04/07, RN-39). Las columnas antiguas direccion_entrega y numero_seguimiento siguen en
 * la tabla con sus datos, pero ya no se mapean (los reemplazan DatosEntrega y Despacho del UML 02).
 */
@Entity
@Table(name = "ordenes")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Atributo id: propiedad que se guarda o utiliza en esta entidad.
    private Long id;

    /** UML 02 numeroPedido (RN-40). Columna nueva; null en pedidos antiguos hasta asignarlo. */
    @Column(name = "numero_pedido", unique = true, length = 20)

    // Atributo numeroPedido: propiedad que se guarda o utiliza en esta entidad.
    private String numeroPedido;

    @Column(name = "id_usuario", nullable = false)
    @Positive
    @NotNull
    // Atributo idUsuario: propiedad que se guarda o utiliza en esta entidad.
    private Long idUsuario;

    @Column(nullable = false, precision = 12, scale = 2)
    @DecimalMin(value = "0.0")
    @NotNull
    // Atributo total: propiedad que se guarda o utiliza en esta entidad.
    private BigDecimal total;

    @Column(name = "descuento_historico", precision = 12, scale = 2)
    // Atributo descuentoHistorico: propiedad que se guarda o utiliza en esta entidad.
    private BigDecimal descuentoHistorico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    // Dato estado del registro; validación: valor obligatorio (no nulo); enum persistido con las opciones permitidas.
    private EstadoOrden estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    // Fecha u hora utilizada para registrar o controlar el proceso.
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ItemPedido> items = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "datos_entrega_id", unique = true)
    // Dato datos entrega del registro; relación JPA entre entidades del mismo microservicio.
    private DatosEntrega datosEntrega;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL)
    @OrderBy("fechaHora ASC, id ASC")
    private List<HistorialPedido> historial = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "despacho_id", unique = true)
    // Dato despacho del registro; relación JPA entre entidades del mismo microservicio.
    private Despacho despacho;

    @PrePersist
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método onCreate (on create).
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoOrden.PENDIENTE;
        }
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método Orden (orden).
    protected Orden() {
    }

    /**
     * HU-25 / RN-15: pedido nuevo con el snapshot de la compra aprobada. El total lo calcula el backend como la
     * suma de los subtotales de sus ítems (nunca se toma un monto enviado por el frontend). Nace PENDIENTE.
     */
    public static Orden crear(Long idUsuario, List<ItemPedido> items, BigDecimal descuento, DatosEntrega datos,
                              String motivo) {
        if (items == null || items.isEmpty()) {
            throw new ValidacionNegocioException("El pedido debe tener al menos un producto");
        }
        Orden o = new Orden();
        o.idUsuario = Objects.requireNonNull(idUsuario);
        o.datosEntrega = Objects.requireNonNull(datos);
        o.descuentoHistorico = descuento == null ? BigDecimal.ZERO : descuento;
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : items) {
            item.asignarOrden(o);
            o.items.add(item);
            total = total.add(item.getSubtotalHistorico());
        }
        o.total = total;
        o.estado = EstadoOrden.PENDIENTE;
        o.fechaCreacion = LocalDateTime.now();
        o.historial.add(new HistorialPedido(o, null, EstadoOrden.PENDIENTE, motivo, "Pedido creado", idUsuario));
        return o;
    }

    /** Número visible (ej. PED-000015). Se asigna una vez, después de guardar (igual que SOP- en Soporte). */
    // Ejecuta asignar numero pedido como parte de las responsabilidades de esta clase.
    public void asignarNumeroPedido() {
        if (this.id == null) {
            throw new IllegalStateException("El número se asigna después de guardar el pedido");
        }
        if (this.numeroPedido == null) {
            this.numeroPedido = String.format("PED-%06d", this.id);
        }
    }

    // ===================== UML 02 =====================

    public boolean puedeTransicionarA(EstadoOrden nuevoEstado) {
        return nuevoEstado != null && estado.esTransicionValida(nuevoEstado);
    }

    /**
     * UML 02 cambiarEstado. Como en el Lote 1 (Soporte), recibe también el motivo y el responsable para
     * dejarlos en el historial (RNF-09). Transición inválida -> 409 y el pedido no cambia (RN-16, RN-39).
     */
    public void cambiarEstado(EstadoOrden nuevoEstado, String motivo, String descripcion, Long responsableId) {
        if (!puedeTransicionarA(nuevoEstado)) {
            throw new OperacionNoPermitidaException("No se puede cambiar el pedido de " + estado + " a " + nuevoEstado);
        }
        if (nuevoEstado == EstadoOrden.CANCELADO && (motivo == null || motivo.isBlank())) {
            throw new ValidacionNegocioException("La cancelación requiere un motivo",
                    Map.of("motivo", "Indica el motivo de la cancelación"));
        }
        EstadoOrden anterior = this.estado;
        this.estado = nuevoEstado;
        historial.add(new HistorialPedido(this, anterior, nuevoEstado, motivo, descripcion, responsableId));
    }

    /** RN-18 / RN-19: cancelable mientras no haya sido despachado. */
    // Ejecuta puede cancelarse como parte de las responsabilidades de esta clase.
    public boolean puedeCancelarse() {
        return estado == EstadoOrden.PENDIENTE || estado == EstadoOrden.CONFIRMADO || estado == EstadoOrden.PREPARANDO;
    }

    // Ejecuta esta activo como parte de las responsabilidades de esta clase.
    public boolean estaActivo() {
        return !estaFinalizado();
    }

    /** UML 02: CANCELADO y ENTREGADO son terminales. */
    // Ejecuta esta finalizado como parte de las responsabilidades de esta clase.
    public boolean estaFinalizado() {
        return estado == EstadoOrden.ENTREGADO || estado == EstadoOrden.CANCELADO;
    }

    // ===================== Apoyo (reglas del ERS) =====================

    public boolean perteneceA(Long userId) {
        return idUsuario.equals(userId);
    }

    /** RF-PED-05: los datos de entrega se corrigen solo antes de DESPACHADO (y nunca en CANCELADO). */
    // Ejecuta permite corregir entrega como parte de las responsabilidades de esta clase.
    public boolean permiteCorregirEntrega() {
        return estado == EstadoOrden.PENDIENTE || estado == EstadoOrden.CONFIRMADO || estado == EstadoOrden.PREPARANDO;
    }

    public List<String> corregirDatosEntrega(String nombreReceptor, String telefono, String direccion,
                                             String comuna, String referencia, Long adminId) {
        if (!permiteCorregirEntrega()) {
            throw new OperacionNoPermitidaException("Los datos de entrega no se pueden corregir con el pedido " + estado);
        }
        if (datosEntrega == null) {
            throw new OperacionNoPermitidaException("Este pedido antiguo no tiene datos de entrega registrados");
        }
        List<String> cambios = datosEntrega.corregir(nombreReceptor, telefono, direccion, comuna, referencia);
        if (!cambios.isEmpty()) {
            historial.add(new HistorialPedido(this, estado, estado, "Corrección de datos de entrega",
                    "Campos corregidos: " + String.join(", ", cambios), adminId));
        }
        return cambios;
    }

    /**
     * RF-PED-08: en PREPARANDO registra el despacho y pasa a DESPACHADO; en DESPACHADO actualiza los datos.
     * En cualquier otro estado -> 409.
     */
    public void registrarDespacho(String transportista, String numeroSeguimiento, LocalDateTime fecha, Long adminId) {
        if (estado == EstadoOrden.PREPARANDO) {
            this.despacho = new Despacho(transportista, numeroSeguimiento, fecha);
            cambiarEstado(EstadoOrden.DESPACHADO, "Despacho registrado", "Transportista: " + despacho.getTransportista(),
                    adminId);
        } else if (estado == EstadoOrden.DESPACHADO) {
            if (despacho == null) {
                this.despacho = new Despacho(transportista, numeroSeguimiento, fecha);
            } else {
                despacho.actualizar(transportista, numeroSeguimiento, fecha);
            }
            historial.add(new HistorialPedido(this, estado, estado, "Actualización de despacho",
                    "Transportista: " + despacho.getTransportista(), adminId));
        } else {
            throw new OperacionNoPermitidaException("El despacho solo se registra con el pedido PREPARANDO o DESPACHADO"
                    + " (estado actual: " + estado + ")");
        }
    }

    // ===================== Revisión Lote 3: reposición de stock al cancelar =====================

    public static final String MOTIVO_REPOSICION_PENDIENTE = "Reposición de stock pendiente";
    // Constante MOTIVO_STOCK_REPUESTO: valor compartido que no cambia durante la ejecución.
    public static final String MOTIVO_STOCK_REPUESTO = "Stock repuesto";

    /**
     * Marca en el HISTORIAL (UML 02, sin columnas nuevas) que falta reponer el stock de este pedido cancelado.
     * Se guarda junto con el CANCELADO, antes de llamar a Catalog: si después algo falla, la marca queda y la
     * cancelación se puede reintentar sin reponer dos veces (la operación de Catalog es idempotente).
     */
    public void marcarReposicionPendiente(String descripcion, Long responsableId) {
        if (estado != EstadoOrden.CANCELADO) {
            throw new IllegalStateException("La reposición solo se marca en un pedido CANCELADO");
        }
        registrarEvento(MOTIVO_REPOSICION_PENDIENTE, descripcion, responsableId);
    }

    /** Cierra la marca cuando Catalog confirmó la reposición. Si no había marca pendiente, no hace nada. */
    // Ejecuta confirmar reposicion como parte de las responsabilidades de esta clase.
    public boolean confirmarReposicion(Long responsableId) {
        if (!reposicionPendiente()) {
            return false;
        }
        registrarEvento(MOTIVO_STOCK_REPUESTO, "Stock repuesto en Catalog", responsableId);
        return true;
    }

    /** true si la última marca de reposición del historial sigue sin confirmarse. */
    // Ejecuta reposicion pendiente como parte de las responsabilidades de esta clase.
    public boolean reposicionPendiente() {
        boolean pendiente = false;
        for (HistorialPedido h : historial) {
            if (MOTIVO_REPOSICION_PENDIENTE.equals(h.getMotivo())) {
                pendiente = true;
            } else if (MOTIVO_STOCK_REPUESTO.equals(h.getMotivo())) {
                pendiente = false;
            }
        }
        return pendiente;
    }

    /** Evento relevante sin cambio de estado (por ejemplo, el descuento de stock quedó por confirmar). */
    // Ejecuta registrar evento como parte de las responsabilidades de esta clase.
    public void registrarEvento(String motivo, String descripcion, Long responsableId) {
        historial.add(new HistorialPedido(this, estado, estado, motivo, descripcion, responsableId));
    }

    // ===================== Getters (sin setters: ver javadoc de la clase) =====================

    public Long getId() { return id; }
    public String getNumeroPedido() { return numeroPedido; }
    public Long getIdUsuario() { return idUsuario; }
    public BigDecimal getTotal() { return total; }
    /** Pedidos antiguos sin el dato -> 0. */
    public BigDecimal getDescuentoHistorico() { return descuentoHistorico == null ? BigDecimal.ZERO : descuentoHistorico; }
    public EstadoOrden getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public List<ItemPedido> getItems() { return Collections.unmodifiableList(items); }
    public DatosEntrega getDatosEntrega() { return datosEntrega; }
    public List<HistorialPedido> getHistorial() { return Collections.unmodifiableList(historial); }
    public Despacho getDespacho() { return despacho; }
}
