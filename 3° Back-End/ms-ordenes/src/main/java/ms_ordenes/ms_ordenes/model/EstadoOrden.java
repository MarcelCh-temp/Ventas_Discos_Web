package ms_ordenes.ms_ordenes.model;

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