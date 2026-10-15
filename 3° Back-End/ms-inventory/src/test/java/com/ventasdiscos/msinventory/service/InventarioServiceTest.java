package com.ventasdiscos.msinventory.service;

import com.ventasdiscos.msinventory.dto.AjusteRequest;
import com.ventasdiscos.msinventory.dto.InventarioResponse;
import com.ventasdiscos.msinventory.dto.UmbralRequest;
import com.ventasdiscos.msinventory.model.Disponibilidad;
import com.ventasdiscos.msinventory.dto.MovimientoRequest;
import com.ventasdiscos.msinventory.dto.MovimientoResponse;
import com.ventasdiscos.msinventory.dto.OperacionInternaItem;
import com.ventasdiscos.msinventory.dto.OperacionInternaRequest;
import com.ventasdiscos.msinventory.dto.OperacionInternaResponse;
import com.ventasdiscos.msinventory.dto.OperacionMasivaItem;
import com.ventasdiscos.msinventory.dto.OperacionMasivaRequest;
import com.ventasdiscos.msinventory.exception.OperacionNoPermitidaException;
import com.ventasdiscos.msinventory.exception.StockInsuficienteException;
import com.ventasdiscos.msinventory.exception.ValidacionNegocioException;
import com.ventasdiscos.msinventory.model.Inventario;
import com.ventasdiscos.msinventory.model.MovimientoStock;
import com.ventasdiscos.msinventory.model.TipoMovimiento;
import com.ventasdiscos.msinventory.repository.InventarioRepository;
import com.ventasdiscos.msinventory.repository.MovimientoStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas de EP-07: stock nunca negativo, todo-o-nada, historial e idempotencia. */
class InventarioServiceTest {

    private InventarioRepository invRepo;
    private MovimientoStockRepository movRepo;
    private InventarioService service;
    private final List<Inventario> enBd = new ArrayList<>();

    @BeforeEach
    void setUp() {
        invRepo = mock(InventarioRepository.class);
        movRepo = mock(MovimientoStockRepository.class);
        service = new InventarioService(invRepo, movRepo, 5);
        enBd.clear();
        enBd.add(inventario(1L, 10));
        enBd.add(inventario(2L, 3));
        when(invRepo.findForUpdate(anyCollection())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return enBd.stream().filter(i -> ids.contains(i.getProductId())).toList();
        });
        when(invRepo.save(any(Inventario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movRepo.save(any(MovimientoStock.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Inventario inventario(Long productId, int stock) {
        Inventario i = new Inventario(productId, 5);
        i.setStockActual(stock);
        return i;
    }

    @Test
    void entradaSumaYRegistraHistorial() {
        MovimientoResponse r = service.registrarMovimiento(1L,
                new MovimientoRequest(TipoMovimiento.ENTRADA, new BigDecimal("5"), "Reposición"), 99L);
        assertEquals(10, r.stockAnterior());
        assertEquals(15, r.stockResultante());
        assertEquals(99L, r.usuarioId());
        verify(movRepo).save(any(MovimientoStock.class));
    }

    @Test
    void salidaMayorAlStockSeRechazaYNoCambiaNada() {
        assertThrows(StockInsuficienteException.class, () -> service.registrarMovimiento(2L,
                new MovimientoRequest(TipoMovimiento.SALIDA, new BigDecimal("4"), null), 99L));
        assertEquals(3, enBd.get(1).getStockActual());
        verify(movRepo, never()).save(any());
    }

    @Test
    void ajusteFijaValorFisico() {
        MovimientoResponse r = service.ajustar(1L, new AjusteRequest(new BigDecimal("7"), "Conteo físico"), 99L);
        assertEquals(10, r.stockAnterior());
        assertEquals(7, r.stockResultante());
    }

    @Test
    void productoSinRegistroSeCreaConStockCero() {
        MovimientoResponse r = service.registrarMovimiento(50L,
                new MovimientoRequest(TipoMovimiento.ENTRADA, new BigDecimal("2"), null), 99L);
        assertEquals(0, r.stockAnterior());
        assertEquals(2, r.stockResultante());
    }

    @Test
    void masivoConUnaFilaInvalidaNoAplicaNinguna() {
        OperacionMasivaRequest req = new OperacionMasivaRequest(List.of(
                new OperacionMasivaItem(1L, TipoMovimiento.ENTRADA, new BigDecimal("5"), null),
                new OperacionMasivaItem(2L, TipoMovimiento.SALIDA, new BigDecimal("10"), null)));
        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class,
                () -> service.aplicarMasivo(req, 99L));
        assertTrue(ex.getErrores().containsKey("operaciones[1].cantidad"));
        assertEquals(10, enBd.get(0).getStockActual());
        assertEquals(3, enBd.get(1).getStockActual());
        verify(movRepo, never()).save(any());
    }

    @Test
    void masivoAjusteSinMotivoSeRechaza() {
        OperacionMasivaRequest req = new OperacionMasivaRequest(List.of(
                new OperacionMasivaItem(1L, TipoMovimiento.AJUSTE, new BigDecimal("5"), " ")));
        assertThrows(ValidacionNegocioException.class, () -> service.aplicarMasivo(req, 99L));
    }

    @Test
    void masivoConsideraVariasFilasDelMismoProducto() {
        // 3 - 2 = 1 (ok); 1 - 2 = -1 (falla): debe detectarse aunque cada fila sola sea válida
        OperacionMasivaRequest req = new OperacionMasivaRequest(List.of(
                new OperacionMasivaItem(2L, TipoMovimiento.SALIDA, new BigDecimal("2"), null),
                new OperacionMasivaItem(2L, TipoMovimiento.SALIDA, new BigDecimal("2"), null)));
        assertThrows(StockInsuficienteException.class, () -> service.aplicarMasivo(req, 99L));
    }

    @Test
    void masivoValidoAplicaTodo() {
        OperacionMasivaRequest req = new OperacionMasivaRequest(List.of(
                new OperacionMasivaItem(1L, TipoMovimiento.SALIDA, new BigDecimal("1"), null),
                new OperacionMasivaItem(2L, TipoMovimiento.ENTRADA, new BigDecimal("1"), null)));
        assertEquals(2, service.aplicarMasivo(req, 99L).size());
        assertEquals(9, enBd.get(0).getStockActual());
        assertEquals(4, enBd.get(1).getStockActual());
    }

    @Test
    void operacionInternaRepetidaNoDescuentaDosVeces() {
        MovimientoStock previo = new MovimientoStock();
        previo.setInventario(enBd.get(0));
        previo.setTipo(TipoMovimiento.SALIDA);
        previo.setCantidad(2);
        previo.setStockAnterior(12);
        previo.setStockResultante(10);
        previo.setOperacionId("PEDIDO-1-VENTA");
        previo.setMotivo("Venta");
        when(movRepo.findByOperacionId("PEDIDO-1-VENTA")).thenReturn(List.of(previo));

        OperacionInternaResponse r = service.aplicarOperacionInterna(new OperacionInternaRequest("PEDIDO-1-VENTA",
                "Venta", List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 2))));

        assertTrue(r.yaAplicada());
        assertEquals(10, enBd.get(0).getStockActual());
        verify(movRepo, never()).save(any());
    }

    @Test
    void operacionInternaSinStockNoDescuentaNinguna() {
        OperacionInternaRequest req = new OperacionInternaRequest("PEDIDO-2-VENTA", "Venta", List.of(
                new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 2),
                new OperacionInternaItem(2L, TipoMovimiento.SALIDA, 5)));
        assertThrows(StockInsuficienteException.class, () -> service.aplicarOperacionInterna(req));
        assertEquals(10, enBd.get(0).getStockActual());
    }

    @Test
    void historialRechazaRangoDeFechasInvertido() {
        assertThrows(ValidacionNegocioException.class, () -> service.historial(null, null,
                java.time.LocalDate.of(2026, 5, 2), java.time.LocalDate.of(2026, 5, 1), 0, 20));
    }

    @Test
    void movimientoTipoAjusteDebeIrPorAdjustments() {
        assertThrows(ValidacionNegocioException.class, () -> service.registrarMovimiento(1L,
                new MovimientoRequest(TipoMovimiento.AJUSTE, new BigDecimal("1"), null), 99L));
    }

    @Test
    void cantidadNoEnteraSeRechaza() {
        assertThrows(ValidacionNegocioException.class, () -> service.registrarMovimiento(1L,
                new MovimientoRequest(TipoMovimiento.ENTRADA, new BigDecimal("2.5"), null), 99L));
    }

    @Test
    void configurarUmbralRecalculaDisponibilidad() {
        InventarioResponse r = service.configurarUmbral(1L, new UmbralRequest(new BigDecimal("10")));
        assertEquals(10, r.umbralCritico());
        assertEquals(Disponibilidad.STOCK_CRITICO, r.disponibilidad());
    }

    @Test
    void registrarProductoEsIdempotente() {
        assertEquals(10, service.registrarProducto(1L).stockActual());
        InventarioResponse nuevo = service.registrarProducto(60L);
        assertEquals(0, nuevo.stockActual());
        assertEquals(Disponibilidad.AGOTADO, nuevo.disponibilidad());
    }

    @Test
    void masivoEntradaConCantidadCeroSeRechaza() {
        OperacionMasivaRequest req = new OperacionMasivaRequest(List.of(
                new OperacionMasivaItem(1L, TipoMovimiento.ENTRADA, BigDecimal.ZERO, null),
                new OperacionMasivaItem(2L, TipoMovimiento.AJUSTE, new BigDecimal("4"), null)));
        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> service.aplicarMasivo(req, 99L));
        assertTrue(ex.getErrores().containsKey("operaciones[0].cantidad"));
        assertTrue(ex.getErrores().containsKey("operaciones[1].motivo"));
    }

    @Test
    void operacionInternaValidaDescuenta() {
        OperacionInternaResponse r = service.aplicarOperacionInterna(new OperacionInternaRequest("PEDIDO-3-VENTA",
                "Venta", List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 4))));
        assertEquals(false, r.yaAplicada());
        assertEquals(6, enBd.get(0).getStockActual());
        assertEquals("PEDIDO-3-VENTA", r.movimientos().get(0).operacionId());
    }

    @Test
    void operacionInternaRechazaAjusteYProductoRepetido() {
        assertThrows(ValidacionNegocioException.class, () -> service.aplicarOperacionInterna(
                new OperacionInternaRequest("OP-1", "x", List.of(new OperacionInternaItem(1L, TipoMovimiento.AJUSTE, 1)))));
        assertThrows(ValidacionNegocioException.class, () -> service.aplicarOperacionInterna(
                new OperacionInternaRequest("OP-2", "x", List.of(
                        new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 1),
                        new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 1)))));
    }

    @Test
    void disponibilidadLoteCompletaProductosSinRegistro() {
        when(invRepo.findByProductIdIn(anyCollection())).thenReturn(List.of(enBd.get(0)));
        List<InventarioResponse> r = service.disponibilidadLote(List.of(1L, 99L));
        assertEquals(2, r.size());
        assertEquals(Disponibilidad.DISPONIBLE, r.get(0).disponibilidad());
        assertEquals(Disponibilidad.AGOTADO, r.get(1).disponibilidad());
    }

    @Test
    void disponibilidadLoteLimitaA500() {
        List<Long> muchos = LongStream.rangeClosed(1, 501).boxed().toList();
        assertThrows(ValidacionNegocioException.class, () -> service.disponibilidadLote(muchos));
    }

    @Test
    void idsPorDisponibilidadUsaLaConsultaCorrecta() {
        when(invRepo.findProductIdsDisponibles()).thenReturn(List.of(1L));
        when(invRepo.findProductIdsStockCritico()).thenReturn(List.of(2L));
        when(invRepo.findProductIdsAgotados()).thenReturn(List.of(3L));
        assertEquals(List.of(1L), service.productIdsPorDisponibilidad(Disponibilidad.DISPONIBLE));
        assertEquals(List.of(2L), service.productIdsPorDisponibilidad(Disponibilidad.STOCK_CRITICO));
        assertEquals(List.of(3L), service.productIdsPorDisponibilidad(Disponibilidad.AGOTADO));
    }

    @Test
    void obtenerProductoConYSinRegistro() {
        when(invRepo.findByProductId(1L)).thenReturn(Optional.of(enBd.get(0)));
        when(invRepo.findByProductId(99L)).thenReturn(Optional.empty());
        assertEquals(10, service.obtener(1L).stockActual());
        assertEquals(0, service.obtener(99L).stockActual());
    }

    private MovimientoStock movimientoPrevio(Inventario inv, String operacionId, int cantidad, String motivo) {
        MovimientoStock m = new MovimientoStock();
        m.setInventario(inv);
        m.setTipo(TipoMovimiento.SALIDA);
        m.setCantidad(cantidad);
        m.setStockAnterior(inv.getStockActual() + cantidad);
        m.setStockResultante(inv.getStockActual());
        m.setOperacionId(operacionId);
        m.setMotivo(motivo);
        return m;
    }

    @Test
    void mismaOperacionConOtroContenidoEsConflicto() {
        when(movRepo.findByOperacionId("PEDIDO-5-VENTA")).thenReturn(List.of(
                movimientoPrevio(enBd.get(0), "PEDIDO-5-VENTA", 2, "Venta")));

        // Otra cantidad, otro motivo y otro producto con la MISMA clave -> 409, sin mover stock
        assertThrows(OperacionNoPermitidaException.class, () -> service.aplicarOperacionInterna(
                new OperacionInternaRequest("PEDIDO-5-VENTA", "Venta",
                        List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 3)))));
        assertThrows(OperacionNoPermitidaException.class, () -> service.aplicarOperacionInterna(
                new OperacionInternaRequest("PEDIDO-5-VENTA", "Otro motivo",
                        List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 2)))));
        assertThrows(OperacionNoPermitidaException.class, () -> service.aplicarOperacionInterna(
                new OperacionInternaRequest("PEDIDO-5-VENTA", "Venta", List.of(
                        new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 2),
                        new OperacionInternaItem(2L, TipoMovimiento.SALIDA, 1)))));
        assertEquals(10, enBd.get(0).getStockActual());
        verify(movRepo, never()).save(any());
    }

    @Test
    void reintentoMultiItemEnOtroOrdenEsIdempotente() {
        when(movRepo.findByOperacionId("PEDIDO-6-VENTA")).thenReturn(List.of(
                movimientoPrevio(enBd.get(0), "PEDIDO-6-VENTA", 2, "Venta"),
                movimientoPrevio(enBd.get(1), "PEDIDO-6-VENTA", 1, "Venta")));

        OperacionInternaResponse r = service.aplicarOperacionInterna(new OperacionInternaRequest("PEDIDO-6-VENTA",
                "Venta", List.of(
                        new OperacionInternaItem(2L, TipoMovimiento.SALIDA, 1),
                        new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 2))));

        assertTrue(r.yaAplicada());
        assertEquals(2, r.movimientos().size());
        verify(movRepo, never()).save(any());
    }

    @Test
    void laOperacionSeBuscaDespuesDeBloquearLasFilas() {
        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(invRepo, movRepo);
        service.aplicarOperacionInterna(new OperacionInternaRequest("PEDIDO-7-VENTA", "Venta",
                List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 1))));
        // calls(1): verificación en orden no "golosa" (findForUpdate se vuelve a llamar al aplicar el lote)
        orden.verify(invRepo, org.mockito.Mockito.calls(1)).findForUpdate(anyCollection());
        orden.verify(movRepo, org.mockito.Mockito.calls(1)).findByOperacionId("PEDIDO-7-VENTA");
    }
}
