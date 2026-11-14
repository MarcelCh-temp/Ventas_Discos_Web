package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.MetodoEntrega;
import ms_ordenes.ms_ordenes.model.Orden;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** RF-PED-01 / RF-MIP-01: identificador, cliente, fecha, total y estado. */
public record PedidoResumenResponse(Long id, String numeroPedido, Long userId, LocalDateTime fechaCompra,
                                    BigDecimal total, EstadoOrden estado, MetodoEntrega metodoEntrega,
                                    String nombreReceptor, int cantidadProductos) {

    // Ejecuta from como parte de las responsabilidades de esta clase.
    public static PedidoResumenResponse from(Orden o) {
        return new PedidoResumenResponse(o.getId(), o.getNumeroPedido(), o.getIdUsuario(), o.getFechaCreacion(),
                o.getTotal(), o.getEstado(),
                o.getDatosEntrega() == null ? null : o.getDatosEntrega().getMetodoEntrega(),
                o.getDatosEntrega() == null ? null : o.getDatosEntrega().getNombreReceptor(),
                o.getItems().size());
    }
}
