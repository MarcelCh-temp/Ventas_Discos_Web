package com.ventasdiscos.mscatalog.model;

/**
 * HU-14: categorías principales del negocio (fijas en el alcance actual).
 * Enum porque son valores cerrados; si el Admin necesitara crearlas, pasaría a entidad.
 */
public enum Categoria {
    MUSICA,
    SERIES_PELICULAS_ANIME,
    VIDEOJUEGOS
}
