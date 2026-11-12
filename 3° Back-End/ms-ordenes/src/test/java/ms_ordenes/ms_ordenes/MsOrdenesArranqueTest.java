package ms_ordenes.ms_ordenes;

import ms_ordenes.ms_ordenes.client.CatalogClient;
import ms_ordenes.ms_ordenes.client.NotificacionesClient;
import ms_ordenes.ms_ordenes.dto.CambioEstadoRequest;
import ms_ordenes.ms_ordenes.dto.PedidoDetalleResponse;
import ms_ordenes.ms_ordenes.exception.OperacionNoPermitidaException;
import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.repository.OrdenRepository;
import ms_ordenes.ms_ordenes.service.OrdenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Lote 1.1: arranca el microservicio completo con Boot 4 y H2 y comprueba que la tabla "ordenes" se mapea bien.
 * Lote 3: además, que un pedido ANTIGUO (fila de "ordenes" sin ítems, sin datos de entrega ni número, como los
 * que ya existen en bd_ordenes) se sigue leyendo y gestionando con el modelo nuevo.
 */
@SpringBootTest
class MsOrdenesArranqueTest {

    @Autowired
    private OrdenService ordenService;
    @Autowired
    private OrdenRepository ordenRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoBean
    private CatalogClient catalog;
    @MockitoBean
    private NotificacionesClient notificaciones;

    private long pedidoAntiguo(long usuario, String estado) {
        jdbc.update("INSERT INTO ordenes (id_usuario, total, estado, fecha_creacion) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                usuario, new BigDecimal("1500.00"), estado);
        return jdbc.queryForObject("SELECT MAX(id) FROM ordenes WHERE id_usuario = ?", Long.class, usuario);
    }

    @Test
    void contextLoads_ConBoot4YH2() {
        assertNotNull(ordenService);
        assertNotNull(ordenRepository);
    }

    @Test
    void pedidoAntiguoSeLeeSinDatosNuevos() {
        long id = pedidoAntiguo(7_000_001L, "CONFIRMADO");
        PedidoDetalleResponse d = ordenService.detalleAdmin(id);
        assertNull(d.numeroPedido());
        assertNull(d.datosEntrega());
        assertTrue(d.items().isEmpty());
        assertEquals(0, new BigDecimal("1500.00").compareTo(d.total()));
        assertEquals(0, BigDecimal.ZERO.compareTo(d.descuento()));
    }

    /** HU-29 / RN-39 sobre un pedido antiguo: recibe su número al gestionarse y no llama a Catalog (no tiene ítems). */
    @Test
    void pedidoAntiguoSeGestionaYNoTocaStock() {
        long id = pedidoAntiguo(7_000_002L, "CONFIRMADO");
        PedidoDetalleResponse d = ordenService.cambiarEstadoAdmin(id,
                new CambioEstadoRequest(EstadoOrden.CANCELADO, "Pedido de prueba antiguo"), 1L);
        assertEquals(EstadoOrden.CANCELADO, d.estado());
        assertEquals(String.format("PED-%06d", id), d.numeroPedido());
        assertEquals(1, d.historial().size());
        verify(catalog, never()).aplicarOperacion(anyString(), anyString(), anyString(), anyList());
        // CANCELADO es terminal y la transición inválida no cambia el pedido guardado
        assertThrows(OperacionNoPermitidaException.class, () -> ordenService.cambiarEstadoAdmin(id,
                new CambioEstadoRequest(EstadoOrden.DESPACHADO, null), 1L));
        assertEquals(EstadoOrden.CANCELADO, ordenRepository.findById(id).orElseThrow().getEstado());
    }
}
