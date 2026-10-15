package com.ventasdiscos.mscatalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HU-56..58. userId = ID externo de MS-Accounts (sin JPA entre MS).
 * Producto sí es @ManyToOne porque vive en este mismo MS.
 * UNIQUE (user_id, producto_id): el mismo favorito no puede repetirse (RN-33).
 */
@Entity
@Table(name = "favorito", uniqueConstraints =
        @UniqueConstraint(name = "uk_favorito_user_producto", columnNames = {"user_id", "producto_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    public Favorito(Long userId, Producto producto) {
        this.userId = userId;
        this.producto = producto;
    }
}
