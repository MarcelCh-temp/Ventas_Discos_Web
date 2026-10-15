package com.ventasdiscos.msinventory.model;

/**
 * HU-40/41/43: tipo de cambio de stock.
 * ENTRADA suma, SALIDA resta, AJUSTE fija el valor contado físicamente.
 * El origen (venta, cancelación, devolución) queda en "motivo" + "operacionId".
 */
public enum TipoMovimiento {
    ENTRADA,
    SALIDA,
    AJUSTE
}
