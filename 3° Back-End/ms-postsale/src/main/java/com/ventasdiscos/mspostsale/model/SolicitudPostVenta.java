package com.ventasdiscos.mspostsale.model;

import com.ventasdiscos.mspostsale.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mspostsale.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mspostsale.exception.ValidacionNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lote 4 · UML 04 definitivo (observación del profesor, CC-06, RN-41): UNA sola entidad concreta para las
 * solicitudes de CANCELACION, DEVOLUCION y REEMBOLSO, que se distinguen por {@link TipoSolicitud}.
 * Reemplaza la herencia JOINED anterior (SolicitudCancelacion, SolicitudDevolucion y SolicitudReembolso).
 * <p>
 * Tabla: se conserva la tabla ORIGINAL {@code solicitudes_postventa} (decisión del equipo, Lote 4, opción 1).
 * Hibernate (ddl-auto=update) solo AGREGA las columnas nuevas; las tablas hijas antiguas no se tocan y sus datos se
 * trasladan con los scripts OPCIONALES {@code seguimiento/sql/postventa_1..4} (verificar, respaldar, migrar datos y
 * ajustar claves foráneas), que no se ejecutan sin aprobación del equipo.
 * Las columnas nuevas quedan sin NOT NULL en la base a propósito: así agregarlas a una tabla con filas no inventa
 * valores (MariaDB rellenaría con '' o con el primer valor del ENUM). La regla "tipo y estado obligatorios" la
 * asegura esta clase: solo se crea con los métodos de fábrica.
 * <p>
 * DOC-09 (aprobada, RNF-09): {@code usuarioResolucionId} = Administrador (del JWT) que aprobó o rechazó.
 */
@Entity
@Table(name = "solicitudes_postventa",
        indexes = @Index(name = "idx_solicitud_pedido", columnList = "pedido_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_solicitud_origen", columnNames = "solicitud_origen_id"))
@Getter
public class SolicitudPostVenta {

    // Constante MAX_TEXTO: valor compartido que no cambia durante la ejecución.
    public static final int MAX_TEXTO = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Atributo id: propiedad que se guarda o utiliza en esta entidad.
    private Long id;

    @NotNull
    @Positive
    @Column(name = "pedido_id", nullable = false)
    // Atributo pedidoId: propiedad que se guarda o utiliza en esta entidad.
    private Long pedidoId;

    @NotNull
    @Positive
    @Column(name = "usuario_id", nullable = false)
    // Dato usuario id del registro; validación: valor obligatorio (no nulo), número mayor que cero.
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20)
    // Dato tipo del registro; enum persistido con las opciones permitidas.
    private TipoSolicitud tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    // Dato estado del registro; validación: valor obligatorio (no nulo); enum persistido con las opciones permitidas.
    private EstadoSolicitud estado;

    @Column(name = "motivo_solicitud", length = MAX_TEXTO)
    // Dato motivo solicitud del registro.
    private String motivoSolicitud;

    @Column(name = "motivo_resolucion", length = MAX_TEXTO)
    // Dato motivo resolucion del registro.
    private String motivoResolucion;

    @NotNull
    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    // Fecha u hora utilizada para registrar o controlar el proceso; validación: valor obligatorio (no nulo).
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    // Fecha u hora utilizada para registrar o controlar el proceso.
    private LocalDateTime fechaResolucion;

    /** Solo REEMBOLSO: la cancelación o devolución aprobada de la que nace (auto-relación origenDeReembolso). */
    @Column(name = "solicitud_origen_id")
    // Atributo solicitudOrigenId: propiedad que se guarda o utiliza en esta entidad.
    private Long solicitudOrigenId;

    /** DOC-09: Administrador que resolvió (del JWT). null mientras está PENDIENTE, en REEMBOLSO y en datos antiguos. */
    @Column(name = "usuario_resolucion_id")
    // Atributo usuarioResolucionId: propiedad que se guarda o utiliza en esta entidad.
    private Long usuarioResolucionId;

    /** Solo DEVOLUCION (al menos uno, RN-41). */
    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemDevolucion> items = new ArrayList<>();

    /** Solo DEVOLUCION APROBADA: puede haber varias recepciones parciales. */
    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<RecepcionDevolucion> recepciones = new ArrayList<>();

    /** Para JPA. */
    // Ejecuta solicitud post venta como parte de las responsabilidades de esta clase.
    protected SolicitudPostVenta() {
    }

    private SolicitudPostVenta(Long pedidoId, Long usuarioId, TipoSolicitud tipo, EstadoSolicitud estado,
                               String motivoSolicitud, Long solicitudOrigenId) {
        this.pedidoId = pedidoId;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.estado = estado;
        this.motivoSolicitud = motivoSolicitud;
        this.solicitudOrigenId = solicitudOrigenId;
    }

    // ===================== Creación (HU-68, HU-69, HU-77) =====================

    /** RF-POS-01: queda PENDIENTE. El estado del pedido lo valida el Service con Orders antes de llamar aquí. */
    // Ejecuta cancelacion como parte de las responsabilidades de esta clase.
    public static SolicitudPostVenta cancelacion(Long pedidoId, Long usuarioId, String motivo) {
        return new SolicitudPostVenta(pedidoId, usuarioId, TipoSolicitud.CANCELACION, EstadoSolicitud.PENDIENTE,
                motivoObligatorio(motivo, "motivoSolicitud"), null);
    }

    /**
     * RF-POS-02 / RN-41: productoId -> cantidad solicitada (al menos un producto). Que el producto se haya comprado y
     * que la cantidad no supere lo comprado (RN-22) lo valida el Service con los datos reales de Orders.
     */
    public static SolicitudPostVenta devolucion(Long pedidoId, Long usuarioId, String motivo, Map<Long, Integer> productos) {
        String m = motivoObligatorio(motivo, "motivoSolicitud");
        if (productos == null || productos.isEmpty()) {
            throw new ValidacionNegocioException("La devolución debe incluir al menos un producto",
                    Map.of("items", "Indica al menos un producto a devolver"));
        }
        SolicitudPostVenta s = new SolicitudPostVenta(pedidoId, usuarioId, TipoSolicitud.DEVOLUCION,
                EstadoSolicitud.PENDIENTE, m, null);
        productos.forEach((productoId, cantidad) -> s.items.add(new ItemDevolucion(s, productoId, cantidad)));
        return s;
    }

    /**
     * RF-POS-10 / RN-41: solo con una cancelación o devolución APROBADA del mismo Cliente. Queda REGISTRADA (final):
     * no se aprueba ni se rechaza y no se procesa dinero. Que no exista otro reembolso para el mismo origen lo revisa
     * el Service (y la base, con la restricción única uk_solicitud_origen).
     */
    public static SolicitudPostVenta reembolso(SolicitudPostVenta origen, Long usuarioId, String motivo) {
        if (origen == null || !origen.perteneceA(usuarioId) || !origen.estaMigrada()) {
            throw new RecursoNoEncontradoException("Solicitud de origen no encontrada");
        }
        if (origen.tipo == TipoSolicitud.REEMBOLSO) {
            throw new ValidacionNegocioException("Un reembolso solo puede originarse en una cancelación o una devolución",
                    Map.of("solicitudOrigenId", "Indica una solicitud de cancelación o de devolución"));
        }
        if (!origen.estaAprobada()) {
            throw new OperacionNoPermitidaException("Solo puedes solicitar el reembolso de una " + nombre(origen.tipo)
                    + " APROBADA (estado actual: " + origen.estado + ")");
        }
        String m = null;
        if (motivo != null && !motivo.isBlank()) {
            m = motivo.trim();
            if (m.length() > MAX_TEXTO) {
                throw new ValidacionNegocioException("El motivo es demasiado largo",
                        Map.of("motivoSolicitud", "Máximo " + MAX_TEXTO + " caracteres"));
            }
        }
        return new SolicitudPostVenta(origen.pedidoId, usuarioId, TipoSolicitud.REEMBOLSO, EstadoSolicitud.REGISTRADA,
                m, origen.id);
    }

    @PrePersist
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método crear (crear).
    protected void crear() {
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
        }
    }

    // ===================== Consultas del UML 04 =====================

    public boolean perteneceA(Long idUsuario) {
        return usuarioId != null && usuarioId.equals(idUsuario);
    }

    // Ejecuta esta pendiente como parte de las responsabilidades de esta clase.
    public boolean estaPendiente() {
        return estado == EstadoSolicitud.PENDIENTE;
    }

    // Ejecuta esta aprobada como parte de las responsabilidades de esta clase.
    public boolean estaAprobada() {
        return estado == EstadoSolicitud.APROBADA;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método estaRegistrada (esta registrada).
    public boolean estaRegistrada() {
        return estado == EstadoSolicitud.REGISTRADA;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método esDeTipo (es de tipo).
    public boolean esDeTipo(TipoSolicitud t) {
        return tipo == t;
    }

    /** false = fila antigua de la herencia todavía sin migrar (tipo o estado vacíos): no se muestra ni se gestiona. */
    // Ejecuta esta migrada como parte de las responsabilidades de esta clase.
    public boolean estaMigrada() {
        return tipo != null && estado != null;
    }

    // ===================== Resolución (HU-74, HU-75) =====================

    /**
     * RF-POS-07 / RF-POS-08. El estado del pedido lo revalida el Service con Orders ANTES de llamar aquí.
     * El UML 04 conserva la firma aprobar(motivo); el segundo parámetro es el responsable de DOC-09.
     */
    public void aprobar(String motivo, Long usuarioResolucionId) {
        resolver(EstadoSolicitud.APROBADA, motivo, usuarioResolucionId);
    }

    // Ejecuta rechazar como parte de las responsabilidades de esta clase.
    public void rechazar(String motivo, Long usuarioResolucionId) {
        resolver(EstadoSolicitud.RECHAZADA, motivo, usuarioResolucionId);
    }

    /** Una sola transición PENDIENTE -> APROBADA/RECHAZADA; el responsable no se sobrescribe después (DOC-09). */
    // Ejecuta resolver como parte de las responsabilidades de esta clase.
    private void resolver(EstadoSolicitud nuevo, String motivo, Long responsable) {
        if (tipo == TipoSolicitud.REEMBOLSO) {
            throw new OperacionNoPermitidaException("Un reembolso queda REGISTRADO: no se aprueba ni se rechaza");
        }
        if (!estaPendiente()) {
            throw new OperacionNoPermitidaException("La solicitud ya fue resuelta (" + estado + ")");
        }
        String m = motivoObligatorio(motivo, "motivo");
        if (responsable == null) {
            throw new IllegalArgumentException("El responsable de la resolución es obligatorio (RNF-09)");
        }
        this.estado = nuevo;
        this.motivoResolucion = m;
        this.fechaResolucion = LocalDateTime.now();
        this.usuarioResolucionId = responsable;
    }

    // ===================== Devolución: productos y recepciones (HU-75, HU-76) =====================

    public Optional<ItemDevolucion> item(Long itemDevolucionId) {
        return items.stream().filter(i -> i.getId() != null && i.getId().equals(itemDevolucionId)).findFirst();
    }

    /** Unidades de ese producto incluidas en esta solicitud. */
    // Ejecuta cantidad solicitada como parte de las responsabilidades de esta clase.
    public int cantidadSolicitada(Long productoId) {
        return items.stream().filter(i -> i.getProductoId().equals(productoId))
                .mapToInt(ItemDevolucion::getCantidadSolicitada).sum();
    }

    // Ejecuta incluye producto como parte de las responsabilidades de esta clase.
    public boolean incluyeProducto(Long productoId) {
        return items.stream().anyMatch(i -> i.getProductoId().equals(productoId));
    }

    /** Unidades ya recibidas de ese ítem, sumando todas las recepciones registradas. */
    // Ejecuta cantidad recibida como parte de las responsabilidades de esta clase.
    public int cantidadRecibida(ItemDevolucion item) {
        return recepciones.stream().flatMap(r -> r.getItems().stream())
                .filter(i -> mismo(i.getItemDevolucion(), item))
                .mapToInt(ItemRecepcionDevolucion::getCantidadRecibida).sum();
    }

    /** Lo que todavía se puede recibir de ese ítem (RF-POS-09: no superar lo autorizado). */
    // Ejecuta pendiente de recibir como parte de las responsabilidades de esta clase.
    public int pendienteDeRecibir(ItemDevolucion item) {
        return item.getCantidadSolicitada() - cantidadRecibida(item);
    }

    /** RF-POS-09: solo una devolución APROBADA admite recepción. DOC-09: guarda al Administrador que la registra. */
    // Ejecuta registrar recepcion como parte de las responsabilidades de esta clase.
    public RecepcionDevolucion registrarRecepcion(String observaciones, Long usuarioRecepcionId) {
        if (tipo != TipoSolicitud.DEVOLUCION) {
            throw new OperacionNoPermitidaException("Solo una devolución admite el registro de recepción");
        }
        if (!estaAprobada()) {
            throw new OperacionNoPermitidaException(
                    "Solo se registra la recepción de una devolución APROBADA (estado actual: " + estado + ")");
        }
        RecepcionDevolucion r = new RecepcionDevolucion(this, observaciones, usuarioRecepcionId);
        recepciones.add(r);
        return r;
    }

    // ===================== Apoyo =====================

    /** Compara por id (sirve aunque Hibernate entregue un proxy); sin id, por identidad. */
    // Ejecuta mismo como parte de las responsabilidades de esta clase.
    private static boolean mismo(ItemDevolucion a, ItemDevolucion b) {
        if (a == b) {
            return true;
        }
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

    // Ejecuta motivo obligatorio como parte de las responsabilidades de esta clase.
    private static String motivoObligatorio(String motivo, String campo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ValidacionNegocioException("El motivo es obligatorio", Map.of(campo, "Indica el motivo"));
        }
        String m = motivo.trim();
        if (m.length() > MAX_TEXTO) {
            throw new ValidacionNegocioException("El motivo es demasiado largo",
                    Map.of(campo, "Máximo " + MAX_TEXTO + " caracteres"));
        }
        return m;
    }

    /** Nombre legible para los mensajes al Cliente. */
    // Ejecuta nombre como parte de las responsabilidades de esta clase.
    public static String nombre(TipoSolicitud t) {
        if (t == null) {
            return "solicitud";
        }
        return switch (t) {
            case CANCELACION -> "cancelación";
            case DEVOLUCION -> "devolución";
            case REEMBOLSO -> "solicitud de reembolso";
        };
    }
}
