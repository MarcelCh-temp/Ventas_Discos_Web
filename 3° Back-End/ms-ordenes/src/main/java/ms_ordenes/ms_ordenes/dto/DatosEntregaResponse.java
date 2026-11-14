package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.DatosEntrega;
import ms_ordenes.ms_ordenes.model.MetodoEntrega;

// DTO: define los datos que entran o salen de una API; evita exponer directamente toda la entidad. Clase DatosEntregaResponse.
public record DatosEntregaResponse(MetodoEntrega metodoEntrega, String nombreMetodo, String nombreReceptor,
                                   String telefono, String direccion, String comuna, String referencia) {

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método from (from).
    public static DatosEntregaResponse from(DatosEntrega d) {
        if (d == null) {
            return null; // pedidos antiguos (antes del Lote 3) no tienen datos de entrega
        }
        return new DatosEntregaResponse(d.getMetodoEntrega(), d.getMetodoEntrega().getNombre(), d.getNombreReceptor(),
                d.getTelefono(), d.getDireccion(), d.getComuna(), d.getReferencia());
    }
}
