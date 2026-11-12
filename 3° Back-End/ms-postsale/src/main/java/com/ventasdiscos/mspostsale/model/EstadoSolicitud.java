package com.ventasdiscos.mspostsale.model;

/**
 * Lote 4 · UML 04 / RN-41. CANCELACION y DEVOLUCION: PENDIENTE -> APROBADA o RECHAZADA (ambos finales).
 * REEMBOLSO: solo REGISTRADA (sin aprobación ni rechazo; no se procesa dinero).
 * Reemplaza a EstadoCancelacion, EstadoDevolucion y EstadoReembolso de la herencia anterior.
 */
public enum EstadoSolicitud {
    PENDIENTE, APROBADA, RECHAZADA, REGISTRADA
}
