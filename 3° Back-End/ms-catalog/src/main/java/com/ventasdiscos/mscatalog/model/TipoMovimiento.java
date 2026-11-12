package com.ventasdiscos.mscatalog.model;

/**
 * HU-40/41/43: tipo de cambio de stock (UML 03: TipoMovimiento).
 * ENTRADA suma, SALIDA resta, AJUSTE fija el valor contado físicamente.
 * El origen (venta, cancelación, devolución) queda en "motivo" + "operacionId".
 * Lote 2: trasladado desde ms-inventory sin cambios (ERS 11.4).
 */
public enum TipoMovimiento {
    ENTRADA,
    SALIDA,
    AJUSTE
}
