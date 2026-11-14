package com.ventasdiscos.mscatalog;

import com.ventasdiscos.mscatalog.dto.MovimientoRequest;
import com.ventasdiscos.mscatalog.dto.OperacionInternaItem;
import com.ventasdiscos.mscatalog.dto.OperacionInternaRequest;
import com.ventasdiscos.mscatalog.dto.OperacionInternaResponse;
import com.ventasdiscos.mscatalog.dto.ProductoRequest;
import com.ventasdiscos.mscatalog.exception.StockInsuficienteException;
import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.EstadoComercial;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.model.TipoMovimiento;
import com.ventasdiscos.mscatalog.repository.InventarioRepository;
import com.ventasdiscos.mscatalog.repository.MovimientoStockRepository;
import com.ventasdiscos.mscatalog.repository.ProductoRepository;
import com.ventasdiscos.mscatalog.service.InventarioService;
import com.ventasdiscos.mscatalog.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Lote 2: control de concurrencia del stock (bloqueo pesimista FOR UPDATE, RNF-08, RN-10).
 * SIN @Transactional a propósito: cada hilo debe tener su PROPIA transacción, como dos peticiones reales.
 * Por eso los datos quedan en la H2 de pruebas; los nombres empiezan con "Concurrencia" y el stock
 * final queda en 0 o sobre el umbral, para no alterar los filtros de las otras clases de prueba.
 */
@SpringBootTest
class InventarioConcurrenciaITTest {

    @Autowired
    private ProductoService productoService;
    @Autowired
    private InventarioService inventarioService;
    @Autowired
    private MovimientoStockRepository movimientoRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private InventarioRepository inventarioRepository;

    private long productoConStock(String nombre, int stock) {
        long id = productoService.crear(new ProductoRequest(nombre, null, new BigDecimal("9990"), Categoria.MUSICA,
                "Rock", "CD", null, null)).id();
        inventarioService.registrarMovimiento(id,
                new MovimientoRequest(TipoMovimiento.ENTRADA, new BigDecimal(stock), "Carga inicial"), 1L);
        return id;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método venta (venta).
    private static OperacionInternaRequest venta(String operacionId, long productId, int cantidad) {
        return new OperacionInternaRequest(operacionId, "Venta",
                List.of(new OperacionInternaItem(productId, TipoMovimiento.SALIDA, cantidad)));
    }

    /** Ejecuta las tareas a la vez (mismo instante de partida) y devuelve el resultado o la excepción de cada una. */
    private static List<Object> enParalelo(List<Callable<Object>> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch partida = new CountDownLatch(1);
        try {
            List<Future<Object>> futuros = new ArrayList<>();
            for (Callable<Object> t : tareas) {
                futuros.add(pool.submit(() -> {
                    partida.await();
                    try {
                        return t.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            partida.countDown();
            List<Object> resultados = new ArrayList<>();
            for (Future<Object> f : futuros) {
                resultados.add(f.get(30, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    /** Dos compras simultáneas de la ÚLTIMA unidad: solo una puede descontar; el stock nunca queda en -1. */
    @Test
    void dosVentasSimultaneasDeLaUltimaUnidad() throws Exception {
        long id = productoConStock("Concurrencia ultima unidad", 1);

        List<Object> r = enParalelo(List.of(
                () -> inventarioService.aplicarOperacionInterna(venta("ORDERS-CONC-A-" + id, id, 1)),
                () -> inventarioService.aplicarOperacionInterna(venta("ORDERS-CONC-B-" + id, id, 1))));

        long exitos = r.stream().filter(OperacionInternaResponse.class::isInstance).count();
        long sinStock = r.stream().filter(StockInsuficienteException.class::isInstance).count();
        assertEquals(1, exitos, "Resultados: " + r);
        assertEquals(1, sinStock, "Resultados: " + r);
        assertEquals(0, inventarioService.obtener(id).stockActual());
    }

    /** Doble clic / reintento simultáneo con la MISMA operacionId: se descuenta una sola vez. */
    @Test
    void mismaOperacionSimultaneaSeAplicaUnaSolaVez() throws Exception {
        long id = productoConStock("Concurrencia misma operacion", 20);
        String operacionId = "ORDERS-CONC-IDEM-" + id;

        List<Object> r = enParalelo(List.of(
                () -> inventarioService.aplicarOperacionInterna(venta(operacionId, id, 2)),
                () -> inventarioService.aplicarOperacionInterna(venta(operacionId, id, 2))));

        for (Object o : r) {
            assertInstanceOf(OperacionInternaResponse.class, o, "Resultados: " + r);
        }
        long yaAplicadas = r.stream().filter(o -> ((OperacionInternaResponse) o).yaAplicada()).count();
        assertEquals(1, yaAplicadas, "Una debe aplicarse y la otra responder 'ya aplicada'");
        assertEquals(18, inventarioService.obtener(id).stockActual());
        assertEquals(1, movimientoRepository.findByOperacionId(operacionId).size());
    }

    /**
     * Revisión Lote 2: producto que existe pero aún no tiene fila de inventario (caso de los productos anteriores
     * a la migración). Dos entradas simultáneas: las dos deben aplicarse, sin error y con una sola fila.
     */
    @Test
    void dosEntradasSimultaneasSobreProductoSinFilaDeInventario() throws Exception {
        Producto p = new Producto();
        p.setNombre("Concurrencia sin fila");
        p.setPrecioBase(new BigDecimal("9990"));
        p.setCategoria(Categoria.MUSICA);
        p.setGenero("Rock");
        p.setFormato("CD");
        p.setEstado(EstadoComercial.ACTIVO);
        long id = productoRepository.save(p).getId(); // sin pasar por ProductoService: queda SIN inventario
        assertEquals(true, inventarioRepository.findByProductId(id).isEmpty());

        List<Object> r = enParalelo(List.of(
                () -> inventarioService.aplicarOperacionInterna(new OperacionInternaRequest("ORDERS-CONC-SF-A-" + id,
                        "Entrada A", List.of(new OperacionInternaItem(id, TipoMovimiento.ENTRADA, 3)))),
                () -> inventarioService.aplicarOperacionInterna(new OperacionInternaRequest("ORDERS-CONC-SF-B-" + id,
                        "Entrada B", List.of(new OperacionInternaItem(id, TipoMovimiento.ENTRADA, 4))))));

        for (Object o : r) {
            assertInstanceOf(OperacionInternaResponse.class, o, "Resultados: " + r);
        }
        assertEquals(7, inventarioService.obtener(id).stockActual());
        assertEquals(1, movimientoRepository.findByOperacionId("ORDERS-CONC-SF-A-" + id).size());
        assertEquals(1, movimientoRepository.findByOperacionId("ORDERS-CONC-SF-B-" + id).size());
    }
}
