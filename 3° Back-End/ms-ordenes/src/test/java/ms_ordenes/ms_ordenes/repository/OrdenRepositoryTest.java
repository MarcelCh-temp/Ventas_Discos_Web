package ms_ordenes.ms_ordenes.repository;

import ms_ordenes.ms_ordenes.client.CatalogClient;
import ms_ordenes.ms_ordenes.client.NotificacionesClient;
import ms_ordenes.ms_ordenes.model.DatosEntrega;
import ms_ordenes.ms_ordenes.model.ItemPedido;
import ms_ordenes.ms_ordenes.model.MetodoEntrega;
import ms_ordenes.ms_ordenes.model.Orden;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Original: probaba findByIdUsuario contra un mock (no probaba nada real).
 * Lote 3: se prueba contra H2 con un pedido completo (ítems, datos de entrega e historial en cascada).
 */
@SpringBootTest
@Transactional
class OrdenRepositoryTest {

    @Autowired
    private OrdenRepository ordenRepository;
    @MockitoBean
    private CatalogClient catalog;
    @MockitoBean
    private NotificacionesClient notificaciones;

    @Test
    void guardaElPedidoCompletoYLoBuscaPorUsuarioYNumero() {
        Orden o = Orden.crear(8_000_001L, List.of(new ItemPedido(1L, "Disco", new BigDecimal("5000"), 1)),
                BigDecimal.ZERO, DatosEntrega.crear(MetodoEntrega.RETIRO_TIENDA, "Ana", "912345678", null, null, null),
                "Prueba");
        o = ordenRepository.saveAndFlush(o);
        o.asignarNumeroPedido();
        ordenRepository.flush();

        List<Orden> delUsuario = ordenRepository.findByIdUsuario(8_000_001L);
        assertEquals(1, delUsuario.size());
        assertEquals(1, delUsuario.get(0).getItems().size());
        assertEquals(1, delUsuario.get(0).getHistorial().size());
        assertTrue(ordenRepository.findByNumeroPedido(o.getNumeroPedido()).isPresent());
        assertTrue(ordenRepository.findForUpdate(o.getId()).isPresent());
    }
}
