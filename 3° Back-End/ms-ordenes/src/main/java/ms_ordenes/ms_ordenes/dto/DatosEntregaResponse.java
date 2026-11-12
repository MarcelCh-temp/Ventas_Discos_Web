package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.DatosEntrega;
import ms_ordenes.ms_ordenes.model.MetodoEntrega;

public record DatosEntregaResponse(MetodoEntrega metodoEntrega, String nombreMetodo, String nombreReceptor,
                                   String telefono, String direccion, String comuna, String referencia) {

    public static DatosEntregaResponse from(DatosEntrega d) {
        if (d == null) {
            return null; // pedidos antiguos (antes del Lote 3) no tienen datos de entrega
        }
        return new DatosEntregaResponse(d.getMetodoEntrega(), d.getMetodoEntrega().getNombre(), d.getNombreReceptor(),
                d.getTelefono(), d.getDireccion(), d.getComuna(), d.getReferencia());
    }
}
