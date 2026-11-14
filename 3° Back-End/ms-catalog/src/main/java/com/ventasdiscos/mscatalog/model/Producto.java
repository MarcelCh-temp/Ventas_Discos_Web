package com.ventasdiscos.mscatalog.model;

import jakarta.validation.constraints.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Información COMERCIAL del producto (HU-08..17). El stock NO vive aquí (RN-06, ADR-11).
 * id generado por el sistema e inmutable (RN-05): no hay setter usado desde DTO.
 */
@Entity
@Table(name = "producto", indexes = {
        @Index(name = "idx_producto_categoria", columnList = "categoria"),
        @Index(name = "idx_producto_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Atributo id: propiedad que se guarda o utiliza en esta entidad.
    private Long id;

    @Column(nullable = false, length = 150)
    @NotBlank
    // Atributo nombre: propiedad que se guarda o utiliza en esta entidad.
    private String nombre;

    @Column(length = 2000)
    // Dato descripcion del registro.
    private String descripcion;

    /** Dinero en BigDecimal, nunca double. */
    @Column(nullable = false, precision = 12, scale = 2)
    @DecimalMin(value = "0.0")
    @NotNull
    // Atributo precioBase: propiedad que se guarda o utiliza en esta entidad.
    private BigDecimal precioBase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @NotNull
    // Atributo categoria: propiedad que se guarda o utiliza en esta entidad.
    private Categoria categoria;

    /** Género/subcategoría: atributo/filtro, no categoría principal (HU-14, HU-16). */
    @Column(nullable = false, length = 60)
    @NotBlank
    // Atributo genero: propiedad que se guarda o utiliza en esta entidad.
    private String genero;

    /** CD, Vinilo, DVD, Blu-ray, Físico, Digital... (atributo/filtro). */
    @Column(nullable = false, length = 60)
    // Atributo formato: propiedad que se guarda o utiliza en esta entidad.
    private String formato;

    /** Solo Videojuegos (PS5, Switch, PC...). */
    @Column(length = 60)
    // Atributo plataforma: propiedad que se guarda o utiliza en esta entidad.
    private String plataforma;

    @Column(length = 500)
    // Atributo imagenUrl: propiedad que se guarda o utiliza en esta entidad.
    private String imagenUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull
    // Atributo estado: propiedad que se guarda o utiliza en esta entidad.
    private EstadoComercial estado;

    // Ejecuta esta activo como parte de las responsabilidades de esta clase.
    public boolean estaActivo() {
        return estado == EstadoComercial.ACTIVO;
    }
}
