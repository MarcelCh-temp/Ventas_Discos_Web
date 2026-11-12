package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.dto.AjusteRequest;
import com.ventasdiscos.mscatalog.dto.ConsultaDisponibilidadRequest;
import com.ventasdiscos.mscatalog.dto.DisponibilidadItemResponse;
import com.ventasdiscos.mscatalog.dto.DisponibilidadResponse;
import com.ventasdiscos.mscatalog.dto.InventarioResponse;
import com.ventasdiscos.mscatalog.dto.UmbralRequest;
import com.ventasdiscos.mscatalog.model.Disponibilidad;
import com.ventasdiscos.mscatalog.dto.MovimientoRequest;
import com.ventasdiscos.mscatalog.dto.MovimientoResponse;
import com.ventasdiscos.mscatalog.dto.OperacionInternaItem;
import com.ventasdiscos.mscatalog.dto.OperacionInternaRequest;
import com.ventasdiscos.mscatalog.dto.OperacionInternaResponse;
import com.ventasdiscos.mscatalog.dto.OperacionMasivaItem;
import com.ventasdiscos.mscatalog.dto.OperacionMasivaRequest;
import com.ventasdiscos.mscatalog.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mscatalog.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mscatalog.exception.RecursoRetiradoException;
import com.ventasdiscos.mscatalog.exception.StockInsuficienteException;
import com.ventasdiscos.mscatalog.exception.ValidacionNegocioException;
import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.EstadoComercial;
import com.ventasdiscos.mscatalog.model.Inventario;
import com.ventasdiscos.mscatalog.model.MovimientoStock;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.model.TipoMovimiento;
import com.ventasdiscos.mscatalog.repository.InventarioRepository;
import com.ventasdiscos.mscatalog.repository.MovimientoStockRepository;
import com.ventasdiscos.mscatalog.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas de EP-07: stock nunca negativo, todo-o-nada, historial e idempotencia.
 * Lote 2: portado desde ms-inventory. Cambios: el producto debe existir en el catálogo (ProductoRepository),
 * el stock inicial se fija con aumentarStock (ya no hay setStockActual) y se agregan las consultas públicas.
 */
class InventarioServiceTest {

    private InventarioRepository invRepo;
    private MovimientoStockRepository movRepo;
    private ProductoRepository prodRepo;
    /** Mock: TransactionTemplate solo llama getTransaction/commit; así se ejecuta la "transacción nueva". */
    private PlatformTransactionManager tm;
    private InventarioService service;
    private final List<Inventario> enBd = new ArrayList<>();
    /** Productos del catálogo: 1, 2, 50 y 60 ACTIVOS; 70 RETIRADO. */
    private final Map<Long, Producto> catalogo = new HashMap<>();

    @BeforeEach
    void setUp() {
        invRepo = mock(InventarioRepository.class);
        movRepo = mock(MovimientoStockRepository.class);
        prodRepo = mock(ProductoRepository.class);
        tm = mock(PlatformTransactionManager.class);
        service = new InventarioService(invRepo, movRepo, prodRepo, tm, 5);
        catalogo.clear();
        for (long id : new long[]{1L, 2L, 50L, 60L}) {
            catalogo.put(id, Fixtures.producto(id, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO));
        }
        catalogo.put(70L, Fixtures.producto(70L, "1000", Categoria.MUSICA, EstadoComercial.RETIRADO));
        when(prodRepo.findAllById(anyIterable())).thenAnswer(inv -> {
            Iterable<Long> ids = inv.getArgument(0);
            return StreamSupport.stream(ids.spliterator(), false).filter(catalogo::containsKey).map(catalogo::get).toList();
        });
        when(prodRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(catalogo.get(inv.<Long>getArgument(0))));
        enBd.clear();
        enBd.add(inventario(1L, 10));
        enBd.add(inventario(2L, 3));
        when(invRepo.findForUpdate(anyCollection())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return enBd.stream().filter(i -> ids.contains(i.getProductId())).toList();
        });
        when(invRepo.save(any(Inventario.class))).thenAnswer(inv -> inv.getArgument(0));
        // Fila creada en la transacción aparte: queda "en la BD" para el segundo bloqueo
        when(invRepo.saveAndFlush(any(Inventario.class))).thenAnswer(inv -> {
            Inventario nuevo = inv.getArgument(0);
            enBd.add(nuevo);
            return nuevo;
        });
        when(movRepo.save(any(MovimientoStock.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Inventario inventario(Long productId, int stock) {
        Inventario i = new Inventario(catalogo.get(productId), 5);
        if (stock > 0) {
            i.aumentarStock(stock);
        }
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
        // Revisión Lote 2: la fila se crea en una transacción aparte y luego se vuelve a bloquear
        verify(invRepo).saveAndFlush(any(Inventario.class));
        verify(invRepo, times(2)).findForUpdate(anyCollection());
        verify(tm).getTransaction(any());
    }

    /** Revisión Lote 2: otra operación creó la fila al mismo tiempo (UNIQUE product_id) -> se usa esa, sin error. */
    @Test
    void filaCreadaAlMismoTiempoPorOtraOperacionNoEsError() {
        when(invRepo.saveAndFlush(any(Inventario.class))).thenAnswer(inv -> {
            enBd.add(inventario(50L, 4)); // la otra operación ya confirmó su fila (y su entrada de 4)
            throw new DataIntegrityViolationException("uk_inventario_product");
        });
        MovimientoResponse r = service.registrarMovimiento(50L,
                new MovimientoRequest(TipoMovimiento.ENTRADA, new BigDecimal("2"), null), 99L);
        assertEquals(4, r.stockAnterior());
        assertEquals(6, r.stockResultante());
        assertEquals(1, enBd.stream().filter(i -> i.getProductId() == 50L).count());
        verify(movRepo).save(any(MovimientoStock.class));
    }

    @Test
    void productoConFilaNoAbreTransaccionAparte() {
        service.registrarMovimiento(1L, new MovimientoRequest(TipoMovimiento.ENTRADA, new BigDecimal("1"), null), 99L);
        verify(tm, never()).getTransaction(any());
        verify(invRepo, never()).saveAndFlush(any());
        verify(invRepo, times(1)).findForUpdate(anyCollection());
    }

    // ---------- Revisión Lote 2: desbordamiento de int ----------

    @Test
    void entradaQueSuperaElMaximoSeRechazaYNoCambiaNada() {
        enBd.add(inventario(60L, Integer.MAX_VALUE - 1));
        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> service.registrarMovimiento(60L,
                        new MovimientoRequest(TipoMovimiento.ENTRADA, new BigDecimal("2"), null), 99L));
        assertTrue(ex.getMessage().contains("máximo"));
        assertEquals(Integer.MAX_VALUE - 1, enBd.get(2).getStockActual());
        verify(movRepo, never()).save(any());
        // Justo hasta el máximo sí se permite
        MovimientoResponse r = service.registrarMovimiento(60L,
                new MovimientoRequest(TipoMovimiento.ENTRADA, new BigDecimal("1"), null), 99L);
        assertEquals(Integer.MAX_VALUE, r.stockResultante());
    }

    @Test
    void masivoQueSuperaElMaximoEnLaSegundaFilaNoAplicaNinguna() {
        enBd.add(inventario(60L, 2_000_000_000));
        OperacionMasivaRequest req = new OperacionMasivaRequest(List.of(
                new OperacionMasivaItem(60L, TipoMovimiento.ENTRADA, new BigDecimal("100000000"), null),
                new OperacionMasivaItem(60L, TipoMovimiento.ENTRADA, new BigDecimal("100000000"), null)));
        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> service.aplicarMasivo(req, 99L));
        assertTrue(ex.getErrores().containsKey("operaciones[1].cantidad"));
        assertFalse(ex.getErrores().containsKey("operaciones[0].cantidad"));
        assertEquals(2_000_000_000, enBd.get(2).getStockActual());
        verify(movRepo, never()).save(any());
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
        previo.setOperacionId("ORDERS-PEDIDO-1-VENTA");
        previo.setMotivo("Venta");
        when(movRepo.findByOperacionId("ORDERS-PEDIDO-1-VENTA")).thenReturn(List.of(previo));

        OperacionInternaResponse r = service.aplicarOperacionInterna(new OperacionInternaRequest("ORDERS-PEDIDO-1-VENTA",
                "Venta", List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 2))));

        assertTrue(r.yaAplicada());
        assertEquals(10, enBd.get(0).getStockActual());
        verify(movRepo, never()).save(any());
    }

    @Test
    void operacionInternaSinStockNoDescuentaNinguna() {
        OperacionInternaRequest req = new OperacionInternaRequest("ORDERS-PEDIDO-2-VENTA", "Venta", List.of(
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
        when(invRepo.findByProductId(1L)).thenReturn(Optional.of(enBd.get(0)));
        when(invRepo.findByProductId(60L)).thenReturn(Optional.empty());
        assertEquals(10, service.registrarProducto(catalogo.get(1L)).stockActual());
        verify(invRepo, never()).save(any());
        InventarioResponse nuevo = service.registrarProducto(catalogo.get(60L));
        assertEquals(0, nuevo.stockActual());
        assertEquals(Disponibilidad.AGOTADO, nuevo.disponibilidad());
        verify(invRepo).save(any(Inventario.class));
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
        OperacionInternaResponse r = service.aplicarOperacionInterna(new OperacionInternaRequest("ORDERS-PEDIDO-3-VENTA",
                "Venta", List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 4))));
        assertEquals(false, r.yaAplicada());
        assertEquals(6, enBd.get(0).getStockActual());
        assertEquals("ORDERS-PEDIDO-3-VENTA", r.movimientos().get(0).operacionId());
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

    // Lote 2: disponibilidadLote (HTTP Catalog -> Inventory) se reemplaza por existencias (consulta local)
    @Test
    void existenciasCompletaProductosSinRegistro() {
        when(invRepo.findByProductIdIn(anyCollection())).thenReturn(List.of(enBd.get(0)));
        Map<Long, InventarioResponse> r = service.existencias(List.of(catalogo.get(1L), catalogo.get(50L)));
        assertEquals(2, r.size());
        assertEquals(Disponibilidad.DISPONIBLE, r.get(1L).disponibilidad());
        assertEquals(Disponibilidad.AGOTADO, r.get(50L).disponibilidad());
        assertEquals(0, r.get(50L).stockActual());
        assertEquals("Producto 1", r.get(1L).nombre());
    }

    @Test
    void existenciasDeListaVaciaNoConsulta() {
        assertTrue(service.existencias(List.of()).isEmpty());
        verify(invRepo, never()).findByProductIdIn(anyCollection());
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
        when(invRepo.findByProductId(50L)).thenReturn(Optional.empty());
        assertEquals(10, service.obtener(1L).stockActual());
        assertEquals(0, service.obtener(50L).stockActual());
        // Lote 2: un id que no es producto del catálogo ya no tiene inventario -> 404
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(99L));
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
        when(movRepo.findByOperacionId("ORDERS-PEDIDO-5-VENTA")).thenReturn(List.of(
                movimientoPrevio(enBd.get(0), "ORDERS-PEDIDO-5-VENTA", 2, "Venta")));

        // Otra cantidad, otro motivo y otro producto con la MISMA clave -> 409, sin mover stock
        assertThrows(OperacionNoPermitidaException.class, () -> service.aplicarOperacionInterna(
                new OperacionInternaRequest("ORDERS-PEDIDO-5-VENTA", "Venta",
                        List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 3)))));
        assertThrows(OperacionNoPermitidaException.class, () -> service.aplicarOperacionInterna(
                new OperacionInternaRequest("ORDERS-PEDIDO-5-VENTA", "Otro motivo",
                        List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 2)))));
        assertThrows(OperacionNoPermitidaException.class, () -> service.aplicarOperacionInterna(
                new OperacionInternaRequest("ORDERS-PEDIDO-5-VENTA", "Venta", List.of(
                        new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 2),
                        new OperacionInternaItem(2L, TipoMovimiento.SALIDA, 1)))));
        assertEquals(10, enBd.get(0).getStockActual());
        verify(movRepo, never()).save(any());
    }

    @Test
    void reintentoMultiItemEnOtroOrdenEsIdempotente() {
        when(movRepo.findByOperacionId("ORDERS-PEDIDO-6-VENTA")).thenReturn(List.of(
                movimientoPrevio(enBd.get(0), "ORDERS-PEDIDO-6-VENTA", 2, "Venta"),
                movimientoPrevio(enBd.get(1), "ORDERS-PEDIDO-6-VENTA", 1, "Venta")));

        OperacionInternaResponse r = service.aplicarOperacionInterna(new OperacionInternaRequest("ORDERS-PEDIDO-6-VENTA",
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
        service.aplicarOperacionInterna(new OperacionInternaRequest("ORDERS-PEDIDO-7-VENTA", "Venta",
                List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 1))));
        // calls(1): verificación en orden no "golosa" (findForUpdate se vuelve a llamar al aplicar el lote)
        orden.verify(invRepo, org.mockito.Mockito.calls(1)).findForUpdate(anyCollection());
        orden.verify(movRepo, org.mockito.Mockito.calls(1)).findByOperacionId("ORDERS-PEDIDO-7-VENTA");
    }

    // ===================== Lote 2: el producto debe existir =====================

    @Test
    void movimientoDeProductoInexistenteDa404YNoGuardaNada() {
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> service.registrarMovimiento(99L,
                        new MovimientoRequest(TipoMovimiento.ENTRADA, new BigDecimal("1"), null), 99L));
        assertTrue(ex.getMessage().contains("99"));
        verify(invRepo, never()).save(any());
        verify(movRepo, never()).save(any());
    }

    @Test
    void operacionInternaConProductoInexistenteNoDescuentaNinguna() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.aplicarOperacionInterna(
                new OperacionInternaRequest("ORDERS-PEDIDO-8-VENTA", "Venta", List.of(
                        new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 1),
                        new OperacionInternaItem(99L, TipoMovimiento.SALIDA, 1)))));
        assertEquals(10, enBd.get(0).getStockActual());
        verify(movRepo, never()).save(any());
    }

    @Test
    void responsableNullEnOperacionesInternas() {
        OperacionInternaResponse r = service.aplicarOperacionInterna(new OperacionInternaRequest("ORDERS-PEDIDO-9-VENTA",
                "Venta", List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 1))));
        // RNF-09 / DOC-07: las operaciones de otros MS no tienen usuario; se identifican por operacionId
        assertNull(r.movimientos().get(0).usuarioId());
    }

    // ===================== Lote 2: disponibilidad pública (ERS 12) =====================

    @Test
    void disponibilidadPublicaSinStockExacto() {
        when(invRepo.findByProductId(1L)).thenReturn(Optional.of(enBd.get(0)));
        when(invRepo.findByProductId(50L)).thenReturn(Optional.empty());
        DisponibilidadResponse conStock = service.disponibilidadPublica(1L);
        assertEquals(Disponibilidad.DISPONIBLE, conStock.disponibilidad());
        assertTrue(conStock.permiteCompra());
        DisponibilidadResponse sinRegistro = service.disponibilidadPublica(50L);
        assertEquals(Disponibilidad.AGOTADO, sinRegistro.disponibilidad());
        assertFalse(sinRegistro.permiteCompra());
    }

    @Test
    void disponibilidadPublicaDeRetiradoDa410YDeInexistente404() {
        assertThrows(RecursoRetiradoException.class, () -> service.disponibilidadPublica(70L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.disponibilidadPublica(99L));
    }

    private ConsultaDisponibilidadRequest consulta(long[]... pares) {
        return new ConsultaDisponibilidadRequest(java.util.Arrays.stream(pares)
                .map(p -> new ConsultaDisponibilidadRequest.Item(p[0], (int) p[1])).toList());
    }

    @Test
    void consultaEnLoteIndicaSiAlcanzaElStock() {
        when(invRepo.findByProductIdIn(anyCollection())).thenReturn(List.of(enBd.get(0), enBd.get(1)));
        List<DisponibilidadItemResponse> r = service.consultarDisponibilidad(
                consulta(new long[]{1, 10}, new long[]{2, 4}, new long[]{50, 1}));
        assertTrue(r.get(0).haySuficiente());
        assertTrue(r.get(0).permiteCompra());
        assertFalse(r.get(1).haySuficiente());
        assertEquals(Disponibilidad.STOCK_CRITICO, r.get(1).disponibilidad());
        assertFalse(r.get(2).permiteCompra());
        // Solo consulta: no bloquea ni descuenta
        verify(invRepo, never()).findForUpdate(anyCollection());
        verify(movRepo, never()).save(any());
        assertEquals(10, enBd.get(0).getStockActual());
    }

    @Test
    void consultaEnLoteConRetiradoNoPermiteCompra() {
        when(invRepo.findByProductIdIn(anyCollection())).thenReturn(List.of());
        assertFalse(service.consultarDisponibilidad(consulta(new long[]{70, 1})).get(0).permiteCompra());
    }

    @Test
    void consultaEnLoteRechazaRepetidosEInexistentes() {
        assertThrows(ValidacionNegocioException.class,
                () -> service.consultarDisponibilidad(consulta(new long[]{1, 1}, new long[]{1, 2})));
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.consultarDisponibilidad(consulta(new long[]{1, 1}, new long[]{99, 1})));
    }

    // ===================== DOC-08: prefijo de operacionId =====================

    @Test
    void operacionInternaSinPrefijoPermitidoDa400YNoTocaNada() {
        for (String id : List.of("PEDIDO-1-VENTA", "orders-1-venta", "ORDERS-", "POSTSALE", "INVENTORY-1", "X-ORDERS-1")) {
            ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                    () -> service.aplicarOperacionInterna(new OperacionInternaRequest(id, "Venta",
                            List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 1)))), id);
            assertTrue(ex.getErrores().containsKey("operacionId"), id);
        }
        verify(invRepo, never()).findForUpdate(anyCollection());
        verify(movRepo, never()).save(any());
        assertEquals(10, enBd.get(0).getStockActual());
    }

    @Test
    void prefijosDeOrdersYPostSaleSeAceptan() {
        assertEquals(9, service.aplicarOperacionInterna(new OperacionInternaRequest("ORDERS-PEDIDO-40-VENTA", "Venta",
                List.of(new OperacionInternaItem(1L, TipoMovimiento.SALIDA, 1)))).movimientos().get(0).stockResultante());
        // Formato que PostSale ya usa (DevolucionService): se conserva
        assertEquals(10, service.aplicarOperacionInterna(new OperacionInternaRequest("POSTSALE-RECEPCION-7",
                "Recepcion de devolucion apta", List.of(new OperacionInternaItem(1L, TipoMovimiento.ENTRADA, 1))))
                .movimientos().get(0).stockResultante());
    }
}

