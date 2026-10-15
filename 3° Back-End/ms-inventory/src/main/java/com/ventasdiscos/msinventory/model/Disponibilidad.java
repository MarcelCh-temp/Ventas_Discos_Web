package com.ventasdiscos.msinventory.model;

/** RN-09: se DERIVA del stock y del umbral; nunca se guarda ni se marca a mano (HU-42). */
public enum Disponibilidad {
    DISPONIBLE,
    STOCK_CRITICO,
    AGOTADO
}
