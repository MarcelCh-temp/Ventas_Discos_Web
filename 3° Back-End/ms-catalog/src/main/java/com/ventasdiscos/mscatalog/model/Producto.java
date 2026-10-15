package com.ventasdiscos.mscatalog.model;

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
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 2000)
    private String descripcion;

    /** Dinero en BigDecimal, nunca double. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Categoria categoria;

    /** Género/subcategoría: atributo/filtro, no categoría principal (HU-14, HU-16). */
    @Column(nullable = false, length = 60)
    private String genero;

    /** CD, Vinilo, DVD, Blu-ray, Físico, Digital... (atributo/filtro). */
    @Column(nullable = false, length = 60)
    private String formato;

    /** Solo Videojuegos (PS5, Switch, PC...). */
    @Column(length = 60)
    private String plataforma;

    @Column(length = 500)
    private String imagenUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoComercial estado;

    public boolean estaActivo() {
        return estado == EstadoComercial.ACTIVO;
    }
}
