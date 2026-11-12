package ms_ordenes.ms_ordenes.model;

/**
 * UML 02 EstadoPedido ≡ EstadoOrden (nombre original conservado, equivalencia aprobada en el Lote 3).
 * RN-16: PENDIENTE -> CONFIRMADO -> PREPARANDO -> DESPACHADO -> ENTREGADO; CANCELADO es la alternativa
 * antes del despacho (RN-18, RN-19). CANCELADO y ENTREGADO son terminales (RN-39).
 * Significado en este backend (Lote 3): PENDIENTE = pago simulado aprobado, descuento de stock por confirmar;
 * CONFIRMADO = pago aprobado y stock descontado en Catalog.
 */
public enum EstadoOrden {
    PENDIENTE,
    CONFIRMADO,
    PREPARANDO,
    DESPACHADO,
    ENTREGADO,
    CANCELADO;

    public boolean esTransicionValida(EstadoOrden nuevoEstado) {
        if (this == CANCELADO || this == ENTREGADO) {
            return false; // Estados terminales
        }
        if (nuevoEstado == CANCELADO) {
            // Solo se puede cancelar antes de ser despachado
            return this == PENDIENTE || this == CONFIRMADO || this == PREPARANDO;
        }
        switch (this) {
            case PENDIENTE:
                return nuevoEstado == CONFIRMADO;
            case CONFIRMADO:
                return nuevoEstado == PREPARANDO;
            case PREPARANDO:
                return nuevoEstado == DESPACHADO;
            case DESPACHADO:
                return nuevoEstado == ENTREGADO;
            default:
                return false;
        }
    }
}
