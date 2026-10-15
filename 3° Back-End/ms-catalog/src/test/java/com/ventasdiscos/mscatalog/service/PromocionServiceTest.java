package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.dto.OfertaResponse;
import com.ventasdiscos.mscatalog.dto.PageResponse;
import com.ventasdiscos.mscatalog.dto.PromocionRequest;
import com.ventasdiscos.mscatalog.dto.PromocionResponse;
import com.ventasdiscos.mscatalog.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mscatalog.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mscatalog.exception.ValidacionNegocioException;
import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.EstadoComercial;
import com.ventasdiscos.mscatalog.model.EstadoPromocion;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.model.Promocion;
import com.ventasdiscos.mscatalog.model.TipoDescuento;
import com.ventasdiscos.mscatalog.repository.ProductoRepository;
import com.ventasdiscos.mscatalog.repository.PromocionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Reglas de EP-09 (HU-47..52): fechas, valores, estados y precio nunca negativo. */
class PromocionServiceTest {

    private PromocionRepository promoRepo;
    private ProductoRepository productoRepo;
    private PromocionService service;
    private final LocalDate hoy = LocalDate.now();
    private final Producto disco = Fixtures.producto(1L, "10000", Categoria.MUSICA, EstadoComercial.ACTIVO);

    @BeforeEach
    void setUp() {
        promoRepo = mock(PromocionRepository.class);
        productoRepo = mock(ProductoRepository.class);
        service = new PromocionService(promoRepo, productoRepo, new CalculadoraPromociones());
        when(promoRepo.save(any(Promocion.class))).thenAnswer(inv -> {
            Promocion p = inv.getArgument(0);
            p.setId(50L);
            return p;
        });
        when(productoRepo.findAllById(anyIterable())).thenReturn(List.of(disco));
    }

    private PromocionRequest req(TipoDescuento tipo, String valor, LocalDate inicio, LocalDate fin,
                                 List<Long> productIds, Categoria categoria) {
        return new PromocionRequest("Promo", tipo, new BigDecimal(valor), inicio, fin, null, productIds, categoria);
    }

    private Promocion existente(LocalDate inicio, LocalDate fin, boolean desactivada) {
        Promocion p = Fixtures.promo(7L, TipoDescuento.PORCENTAJE, "10", null, disco);
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        p.setDesactivada(desactivada);
        when(promoRepo.findById(7L)).thenReturn(Optional.of(p));
        return p;
    }

    // ---------- HU-47 ----------

    @Test
    void crearPromocionActiva() {
        PromocionResponse r = service.crear(req(TipoDescuento.PORCENTAJE, "15", hoy, hoy.plusDays(5), List.of(1L), null));
        assertEquals(50L, r.id());
        assertEquals(EstadoPromocion.ACTIVA, r.estado());
        assertEquals(List.of(1L), r.productIds());
    }

    @Test
    void crearPromocionProgramadaPorCategoriaSinProductos() {
        when(productoRepo.findAllById(anyIterable())).thenReturn(List.of());
        PromocionResponse r = service.crear(req(TipoDescuento.PORCENTAJE, "15", hoy.plusDays(2), hoy.plusDays(5),
                null, Categoria.MUSICA));
        assertEquals(EstadoPromocion.PROGRAMADA, r.estado());
        assertEquals(Categoria.MUSICA, r.categoria());
    }

    @Test
    void validacionesDeCreacion() {
        assertThrows(ValidacionNegocioException.class, () -> service.crear(
                req(TipoDescuento.PORCENTAJE, "10", hoy.minusDays(9), hoy.minusDays(1), List.of(1L), null)));
        assertThrows(ValidacionNegocioException.class, () -> service.crear(
                req(TipoDescuento.PORCENTAJE, "10", hoy.plusDays(5), hoy.plusDays(1), List.of(1L), null)));
        assertThrows(ValidacionNegocioException.class, () -> service.crear(
                req(TipoDescuento.PORCENTAJE, "101", hoy, hoy.plusDays(1), List.of(1L), null)));
        assertThrows(ValidacionNegocioException.class, () -> service.crear(
                req(TipoDescuento.PORCENTAJE, "10", hoy, hoy.plusDays(1), List.of(), null)));
    }

    @Test
    void productoInexistenteSeInforma() {
        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class, () -> service.crear(
                req(TipoDescuento.PORCENTAJE, "10", hoy, hoy.plusDays(1), List.of(1L, 99L), null)));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void montoMayorAlPrecioSeRechaza() {
        assertThrows(ValidacionNegocioException.class, () -> service.crear(
                req(TipoDescuento.MONTO, "20000", hoy, hoy.plusDays(1), List.of(1L), null)));
    }

    @Test
    void montoMayorAlPrecioDeUnProductoDeLaCategoriaSeRechaza() {
        when(productoRepo.findByCategoriaAndEstado(Categoria.MUSICA, EstadoComercial.ACTIVO)).thenReturn(
                List.of(Fixtures.producto(2L, "3000", Categoria.MUSICA, EstadoComercial.ACTIVO)));
        assertThrows(ValidacionNegocioException.class, () -> service.crear(
                req(TipoDescuento.MONTO, "5000", hoy, hoy.plusDays(1), List.of(1L), Categoria.MUSICA)));
    }

    @Test
    void montoValido() {
        PromocionResponse r = service.crear(req(TipoDescuento.MONTO, "2000", hoy, hoy.plusDays(1), List.of(1L), null));
        assertEquals(0, new BigDecimal("2000").compareTo(r.valor()));
    }

    // ---------- HU-50 ----------

    @Test
    void editarProgramada() {
        existente(hoy.plusDays(3), hoy.plusDays(9), false);
        PromocionResponse r = service.actualizar(7L,
                req(TipoDescuento.PORCENTAJE, "30", hoy.plusDays(4), hoy.plusDays(9), List.of(1L), null));
        assertEquals(0, new BigDecimal("30").compareTo(r.valor()));
        assertEquals(hoy.plusDays(4), r.fechaInicio());
    }

    @Test
    void noSeEditaFinalizadaNiDesactivada() {
        existente(hoy.minusDays(9), hoy.minusDays(1), false);
        assertThrows(OperacionNoPermitidaException.class, () -> service.actualizar(7L,
                req(TipoDescuento.PORCENTAJE, "30", hoy, hoy.plusDays(9), List.of(1L), null)));
        existente(hoy, hoy.plusDays(9), true);
        assertThrows(OperacionNoPermitidaException.class, () -> service.actualizar(7L,
                req(TipoDescuento.PORCENTAJE, "30", hoy, hoy.plusDays(9), List.of(1L), null)));
    }

    @Test
    void activaNoCambiaFechaInicioNiTerminaEnElPasado() {
        existente(hoy.minusDays(2), hoy.plusDays(9), false);
        assertThrows(OperacionNoPermitidaException.class, () -> service.actualizar(7L,
                req(TipoDescuento.PORCENTAJE, "30", hoy, hoy.plusDays(9), List.of(1L), null)));
        assertThrows(ValidacionNegocioException.class, () -> service.actualizar(7L,
                req(TipoDescuento.PORCENTAJE, "30", hoy.minusDays(2), hoy.minusDays(1), List.of(1L), null)));
    }

    // ---------- HU-51 ----------

    @Test
    void desactivarActiva() {
        existente(hoy.minusDays(1), hoy.plusDays(9), false);
        PromocionResponse r = service.desactivar(7L);
        assertEquals(EstadoPromocion.DESACTIVADA, r.estado());
        assertNotNull(r.fechaDesactivacion());
    }

    @Test
    void noSeDesactivaDosVecesNiSiFinalizo() {
        existente(hoy, hoy.plusDays(9), true);
        assertThrows(OperacionNoPermitidaException.class, () -> service.desactivar(7L));
        existente(hoy.minusDays(9), hoy.minusDays(1), false);
        assertThrows(OperacionNoPermitidaException.class, () -> service.desactivar(7L));
    }

    // ---------- HU-48/49 ----------

    @Test
    void obtenerYNoEncontrada() {
        existente(hoy, hoy.plusDays(1), false);
        assertEquals(7L, service.obtener(7L).id());
        when(promoRepo.findById(8L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(8L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listarMapeaEstadoCalculado() {
        Promocion p = Fixtures.promo(7L, TipoDescuento.PORCENTAJE, "10", null, disco);
        when(promoRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));
        PageResponse<PromocionResponse> r = service.listar(null, null, null, null, null, 0, 20);
        assertEquals(EstadoPromocion.ACTIVA, r.contenido().get(0).estado());
    }

    @Test
    void listarValidaRangoYProducto() {
        assertThrows(ValidacionNegocioException.class,
                () -> service.listar(null, hoy.plusDays(1), hoy, null, null, 0, 20));
        when(productoRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.listar(null, null, null, 99L, null, 0, 20));
    }

    // ---------- HU-52 ----------

    @Test
    void ofertasSoloConProductosActivos() {
        Producto retirado = Fixtures.producto(3L, "5000", Categoria.MUSICA, EstadoComercial.RETIRADO);
        Promocion porProducto = Fixtures.promo(1L, TipoDescuento.PORCENTAJE, "10", null, disco, retirado);
        Promocion porCategoria = Fixtures.promo(2L, TipoDescuento.MONTO, "1000", "20000");
        porCategoria.setCategoria(Categoria.VIDEOJUEGOS);
        Promocion sinProductos = Fixtures.promo(3L, TipoDescuento.PORCENTAJE, "10", null, retirado);
        when(promoRepo.findActivas(any())).thenReturn(List.of(porProducto, porCategoria, sinProductos));
        when(productoRepo.findByCategoriaAndEstado(Categoria.VIDEOJUEGOS, EstadoComercial.ACTIVO)).thenReturn(
                List.of(Fixtures.producto(4L, "40000", Categoria.VIDEOJUEGOS, EstadoComercial.ACTIVO)));

        List<OfertaResponse> ofertas = service.ofertasVigentes();

        assertEquals(2, ofertas.size());
        assertEquals(1, ofertas.get(0).productos().size());
        assertEquals(0, new BigDecimal("9000").compareTo(ofertas.get(0).productos().get(0).precioOferta()));
        assertEquals(0, new BigDecimal("39000").compareTo(ofertas.get(1).productos().get(0).precioOferta()));
    }
}
