package com.ventasdiscos.mscatalog.model;

/** RN-27: Programada/Activa/Finalizada se DERIVAN de las fechas; Desactivada es término manual (HU-51). */
public enum EstadoPromocion {
    // Opción PROGRAMADA: representa el estado o tipo «programada» permitido en este dominio.
    PROGRAMADA,
    // Opción ACTIVA: representa el estado o tipo «activa» permitido en este dominio.
    ACTIVA,
    // Opción FINALIZADA: representa el estado o tipo «finalizada» permitido en este dominio.
    FINALIZADA,
    // Opción DESACTIVADA: representa el estado o tipo «desactivada» permitido en este dominio.
    DESACTIVADA
}
