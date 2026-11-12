package com.ventasdiscos.mscatalog.model;

/**
 * RN-09: se DERIVA del stock y del umbral; nunca se guarda ni se marca a mano (HU-42).
 * Lote 2: trasladado desde ms-inventory. Catalog lo traduce a DisponibilidadCatalogo para las vistas.
 */
public enum Disponibilidad {
    DISPONIBLE,
    STOCK_CRITICO,
    AGOTADO
}
