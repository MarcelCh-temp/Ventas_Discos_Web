package com.ventasdiscos.msaccounts.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuenta de Cliente o Administrador (HU-01..07).
 * Un solo tipo con atributo rol (ERS 11.2): ambos comparten los mismos datos.
 * UNIQUE en rut y correo = 2.ª barrera contra duplicados (RN-33).
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

    public boolean esAdministrador() {
        return rol == Role.ADMINISTRADOR;
    }
}
