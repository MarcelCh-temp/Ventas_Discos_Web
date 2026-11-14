package com.ventasdiscos.mscatalog.model;

/**
 * HU-14: categorías principales del negocio (fijas en el alcance actual).
 * Enum porque son valores cerrados; si el Admin necesitara crearlas, pasaría a entidad.
 */
public enum Categoria {
    // Opción MUSICA: representa el estado o tipo «musica» permitido en este dominio.
    MUSICA,
    // Opción SERIES_PELICULAS_ANIME: representa el estado o tipo «series peliculas anime» permitido en este dominio.
    SERIES_PELICULAS_ANIME,
    // Opción VIDEOJUEGOS: representa el estado o tipo «videojuegos» permitido en este dominio.
    VIDEOJUEGOS
}
