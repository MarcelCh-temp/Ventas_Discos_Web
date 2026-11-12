package com.ventasdiscos.msaccounts.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Cuenta de Cliente o Administrador (HU-01..07).
 * Un solo tipo con atributo rol (ERS 11.2): ambos comparten los mismos datos.
 * UNIQUE en rut y correo = 2.ª barrera contra duplicados (RN-33).
 * <p>
 * Lote 5 (UML 01, HU-78): + estado, fechaSolicitudEliminacion, fechaEliminacion y los métodos puedeOperar,
 * iniciarEliminacion, rechazarEliminacion y completarEliminacion. El estado y las fechas NO tienen setter: solo
 * cambian por esos métodos.
 * Compatibilidad con cuentas anteriores al Lote 5 (decisión del equipo): la columna estado se agrega sin NOT NULL y
 * un valor NULL se entiende como ACTIVA (antes no existía la eliminación). Un valor desconocido NO se entiende como
 * ACTIVA: la cuenta no puede operar. Por eso el atributo se guarda como texto y se lee con getEstado().
 */
@Entity
@Table(name = "usuario", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_rut", columnNames = "rut"),
        @UniqueConstraint(name = "uk_usuario_correo", columnNames = "correo")
})
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    /** Lote 5: no es un hash BCrypt válido; BCrypt nunca lo da por correcto. */
    public static final String PASSWORD_INUTILIZABLE = "!CUENTA-ELIMINADA";

    /** userId que los demás MS guardan como ID externo (ADR-09). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Normalizado "12345678-K". HU-01 (DV válido) y HU-07 (recuperar correo por RUT). */
    @Column(nullable = false, length = 12)
    private String rut;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    /** Credencial de login, guardado en minúsculas para que la unicidad no dependa de mayúsculas. */
    @Column(nullable = false, length = 120)
    private String correo;

    /** Hash BCrypt; la contraseña en texto plano nunca se guarda (RNF-15). */
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role rol;

    /**
     * UML 01: EstadoCuenta. Texto en la base (VARCHAR) para poder reconocer un valor inválido sin que falle la carga.
     * Se lee con getEstado() / estadoReconocido(); se cambia solo con los métodos del ciclo de eliminación.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(name = "estado", length = 20)
    private String estado;

    /** Fecha de la última confirmación de eliminación (RNF-09). Se conserva aunque la solicitud sea rechazada. */
    @Setter(AccessLevel.NONE)
    private LocalDateTime fechaSolicitudEliminacion;

    /** Fecha en que la eliminación terminó (estado ELIMINADA). */
    @Setter(AccessLevel.NONE)
    private LocalDateTime fechaEliminacion;

    /** Las cuentas nuevas se guardan explícitamente ACTIVA (no se toca ninguna fila existente). */
    @PrePersist
    protected void estadoInicial() {
        if (estado == null) {
            estado = EstadoCuenta.ACTIVA.name();
        }
    }

    public boolean esAdministrador() {
        return rol == Role.ADMINISTRADOR;
    }

    /** Estado efectivo: NULL (cuenta anterior al Lote 5) = ACTIVA; un valor desconocido = null (no reconocido). */
    public EstadoCuenta getEstado() {
        if (estado == null) {
            return EstadoCuenta.ACTIVA;
        }
        for (EstadoCuenta e : EstadoCuenta.values()) {
            if (e.name().equals(estado)) {
                return e;
            }
        }
        return null;
    }

    public boolean estadoReconocido() {
        return getEstado() != null;
    }

    /** UML 01 / RN-44: solo una cuenta ACTIVA puede iniciar sesión y hacer nuevas operaciones. */
    public boolean puedeOperar() {
        return getEstado() == EstadoCuenta.ACTIVA;
    }

    public boolean estaEnEliminacion() {
        return getEstado() == EstadoCuenta.EN_ELIMINACION;
    }

    public boolean estaEliminada() {
        return getEstado() == EstadoCuenta.ELIMINADA;
    }

    /** UML 01: el Cliente confirmó con su contraseña. Desde aquí no puede operar (RN-37). */
    public void iniciarEliminacion() {
        if (!puedeOperar()) {
            throw new IllegalStateException("Solo una cuenta ACTIVA puede iniciar su eliminación");
        }
        estado = EstadoCuenta.EN_ELIMINACION.name();
        fechaSolicitudEliminacion = LocalDateTime.now();
    }

    /** UML 01: hay impedimentos (pedidos activos, tickets abiertos o postventa pendiente): vuelve a ACTIVA. */
    public void rechazarEliminacion() {
        if (!estaEnEliminacion()) {
            throw new IllegalStateException("Solo una cuenta EN_ELIMINACION puede volver a ACTIVA");
        }
        estado = EstadoCuenta.ACTIVA.name();
    }

    /**
     * UML 01: verificación y depuraciones confirmadas. Se conserva la fila y su id (los pedidos, tickets y solicitudes
     * guardan ese userId) y se ANONIMIZAN los datos personales (decisión del equipo, RN-38):
     * RUT, nombres, apellidos y correo pasan a valores no identificables que nunca chocan con una cuenta real
     * (un RUT real no lleva letras y un correo real lleva "@"), y la contraseña queda inutilizable: se guarda un
     * marcador que no es un hash BCrypt, así que ninguna contraseña puede coincidir (además, ELIMINADA no inicia
     * sesión). Misma firma del UML 01: completarEliminacion() sin parámetros.
     */
    public void completarEliminacion() {
        if (!estaEnEliminacion()) {
            throw new IllegalStateException("Solo una cuenta EN_ELIMINACION puede completar su eliminación");
        }
        String rutAnonimo = "ELIM-" + id;
        rut = rutAnonimo.length() <= 12 ? rutAnonimo : "E" + id;
        nombres = "Cuenta";
        apellidos = "eliminada";
        correo = "cuenta-eliminada-" + id;
        passwordHash = PASSWORD_INUTILIZABLE;
        estado = EstadoCuenta.ELIMINADA.name();
        fechaEliminacion = LocalDateTime.now();
    }
}
