package ms_ordenes.ms_ordenes.service;

import ms_ordenes.ms_ordenes.client.AccountsClient;
import ms_ordenes.ms_ordenes.client.CatalogClient;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.ItemSolicitado;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.RespuestaOperacion;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.ResultadoOperacion;
import ms_ordenes.ms_ordenes.client.NotificacionesClient;
import ms_ordenes.ms_ordenes.dto.CambioEstadoRequest;
import ms_ordenes.ms_ordenes.dto.PedidoDetalleResponse;
import ms_ordenes.ms_ordenes.exception.ServicioNoDisponibleException;
import ms_ordenes.ms_ordenes.model.DatosEntrega;
import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.ItemPedido;
import ms_ordenes.ms_ordenes.model.MetodoEntrega;
import ms_ordenes.ms_ordenes.model.Orden;
import ms_ordenes.ms_ordenes.repository.CarritoRepository;
import ms_ordenes.ms_ordenes.repository.OrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Revisión Lote 3: escenarios de FALLO entre Orders y Catalog, sin levantar Spring.
 * La "base de datos" es el propio objeto Orden y Catalog es un simulador IDEMPOTENTE: aplica cada operacionId una
 * sola vez y cuenta el stock repuesto, como hace el InventarioService real (Lote 2).
 */
class OrdenServiceFallosTest {

    private static final String OP_CANCELACION = "ORDERS-PEDIDO-15-CANCELACION";

    private OrdenRepository ordenRepository;
    private CarritoRepository carritoRepository;
    private CatalogClient catalog;
    private OrdenService service;
    private Orden orden;
    private final Set<String> operacionesAplicadas = new HashSet<>();
    private int stockRepuesto;

    @BeforeEach
    void setUp() {
        ordenRepository = mock(OrdenRepository.class);
        carritoRepository = mock(CarritoRepository.class);
        catalog = mock(CatalogClient.class);
        // Mock: TransactionTemplate solo llama getTransaction/commit/rollback
        // Lote 5: + AccountsClient (HU-78); aquí no se crean compras, así que basta un simulado
        service = new OrdenService(ordenRepository, carritoRepository, catalog, mock(NotificacionesClient.class),
                mock(AccountsClient.class), mock(PlatformTransactionManager.class));
        orden = Orden.crear(10L, List.of(new ItemPedido(1L, "Disco", new BigDecimal("10000"), 2)), BigDecimal.ZERO,
                DatosEntrega.crear(MetodoEntrega.RETIRO_TIENDA, "Ana", "912345678", null, null, null), "Pago simulado");
        ReflectionTestUtils.setField(orden, "id", 15L);
        orden.asignarNumeroPedido();
        when(ordenRepository.findById(15L)).thenReturn(Optional.of(orden));
        when(catalog.aplicarOperacion(anyString(), anyString(), anyString(), anyList())).thenAnswer(inv -> {
            String operacionId = inv.getArgument(0);
            List<ItemSolicitado> items = inv.getArgument(3);
            if (operacionesAplicadas.add(operacionId) && "ENTRADA".equals(inv.getArgument(2))) {
                stockRepuesto += items.stream().mapToInt(ItemSolicitado::cantidad).sum();
            }
            return new RespuestaOperacion(ResultadoOperacion.APLICADA, null, 201);
        });
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método cancelar (cancelar).
    private PedidoDetalleResponse cancelar() {
        return service.cambiarEstadoAdmin(15L, new CambioEstadoRequest(EstadoOrden.CANCELADO, "Cliente desistió"), 1L);
    }

    /**
     * Catalog CONFIRMA la reposición, pero la transacción de Orders que debía cerrarla FALLA.
     * El pedido queda CANCELADO con la reposición pendiente (no puede despacharse) y el stock repuesto UNA vez.
     * El reintento completa el cierre con la misma operación: Catalog no repone de nuevo.
     */
    @Test
    void reposicionConfirmadaPeroFallaOrdersDespues_reintentoSinDuplicar() {
        orden.cambiarEstado(EstadoOrden.CONFIRMADO, null, null, null);
        when(ordenRepository.findForUpdate(15L))
                .thenReturn(Optional.of(orden))                                       // paso 1: CANCELADO + marca
                .thenThrow(new DataAccessResourceFailureException("BD de Orders caída")) // paso 3 falla
                .thenReturn(Optional.of(orden));                                      // reintento

        assertThrows(DataAccessResourceFailureException.class, this::cancelar);
        assertEquals(EstadoOrden.CANCELADO, orden.getEstado());
        assertTrue(orden.reposicionPendiente());
        assertEquals(2, stockRepuesto);
        assertFalse(orden.puedeTransicionarA(EstadoOrden.DESPACHADO));

        PedidoDetalleResponse d = service.cambiarEstadoAdmin(15L, new CambioEstadoRequest(EstadoOrden.CANCELADO, null), 1L);
        assertFalse(d.reposicionStockPendiente());
        assertEquals(2, stockRepuesto); // no se duplicó
        verify(catalog, times(2)).aplicarOperacion(eq(OP_CANCELACION), anyString(), eq("ENTRADA"), anyList());
    }

    /** Si Orders falla ANTES de guardar el CANCELADO, Catalog no se llama: no hay stock repuesto sin cancelación. */
    @Test
    void fallaOrdersAntesDeCancelar_catalogNoSeLlama() {
        orden.cambiarEstado(EstadoOrden.CONFIRMADO, null, null, null);
        when(ordenRepository.findForUpdate(15L)).thenThrow(new DataAccessResourceFailureException("BD de Orders caída"));
        assertThrows(DataAccessResourceFailureException.class, this::cancelar);
        verify(catalog, never()).aplicarOperacion(anyString(), anyString(), anyString(), anyList());
        assertEquals(EstadoOrden.CONFIRMADO, orden.getEstado());
    }

    /** Catalog no responde al reponer: el pedido queda CANCELADO con la reposición pendiente; nunca sobra stock. */
    @Test
    void catalogNoRespondeAlReponer_quedaPendienteYSeCompletaDespues() {
        orden.cambiarEstado(EstadoOrden.CONFIRMADO, null, null, null);
        when(ordenRepository.findForUpdate(15L)).thenReturn(Optional.of(orden));
        when(catalog.aplicarOperacion(eq(OP_CANCELACION), anyString(), eq("ENTRADA"), anyList()))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.NO_CONFIRMADA, "caído"))
                .thenAnswer(inv -> {
                    if (operacionesAplicadas.add(OP_CANCELACION)) {
                        stockRepuesto += 2;
                    }
                    return new RespuestaOperacion(ResultadoOperacion.APLICADA, null, 201);
                });
        PedidoDetalleResponse d = cancelar();
        assertEquals(EstadoOrden.CANCELADO, d.estado());
        assertTrue(d.reposicionStockPendiente());
        assertEquals(0, stockRepuesto);

        assertFalse(service.cambiarEstadoAdmin(15L, new CambioEstadoRequest(EstadoOrden.CANCELADO, null), 1L)
                .reposicionStockPendiente());
        assertEquals(2, stockRepuesto);
    }

    /** Checkout: 409 de Catalog y la verificación de stock tampoco responde -> sigue PENDIENTE, no se cancela. */
    @Test
    void conflictoSinPoderConfirmarStock_noCancelaLaCompra() {
        when(ordenRepository.findForUpdate(15L)).thenReturn(Optional.of(orden));
        when(catalog.aplicarOperacion(anyString(), anyString(), eq("SALIDA"), anyList()))
                .thenReturn(new RespuestaOperacion(ResultadoOperacion.CONFLICTO, "Stock insuficiente para el producto 1", 409));
        when(catalog.disponibilidad(anyList())).thenThrow(new ServicioNoDisponibleException("caído"));
        PedidoDetalleResponse d = service.descontarStockDeCompra(15L);
        assertEquals(EstadoOrden.PENDIENTE, d.estado());
        verify(carritoRepository, never()).findByUserIdForUpdate(any());
    }
}
