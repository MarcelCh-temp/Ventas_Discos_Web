package com.ventasdiscos.mscatalog.model;

/**
 * RN-09: se DERIVA del stock y del umbral; nunca se guarda ni se marca a mano (HU-42).
 * Lote 2: trasladado desde ms-inventory. Catalog lo traduce a DisponibilidadCatalogo para las vistas.
 */
public enum Disponibilidad {
    // Opción DISPONIBLE: representa el estado o tipo «disponible» permitido en este dominio.
    DISPONIBLE,
    // Opción STOCK_CRITICO: representa el estado o tipo «stock critico» permitido en este dominio.
    STOCK_CRITICO,
    // Opción AGOTADO: representa el estado o tipo «agotado» permitido en este dominio.
    AGOTADO
}
