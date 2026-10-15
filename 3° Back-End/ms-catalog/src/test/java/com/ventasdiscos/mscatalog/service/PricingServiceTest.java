package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.dto.PricingRequest;
import com.ventasdiscos.mscatalog.dto.PricingResponse;
import com.ventasdiscos.mscatalog.exception.ValidacionNegocioException;
import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.EstadoComercial;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.model.TipoDescuento;
import com.ventasdiscos.mscatalog.repository.ProductoRepository;
import com.ventasdiscos.mscatalog.repository.PromocionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** HU-24/HU-52: precio con promociones, compra mínima y productos no comprables. */
class PricingServiceTest {

    private ProductoRepository productoRepo;
    private PromocionRepository promoRepo;
    private PricingService service;
    private final Producto a = Fixtures.producto(1L, "10000", Categoria.MUSICA, EstadoComercial.ACTIVO);
    private final Producto b = Fixtures.producto(2L, "5000", Categoria.VIDEOJUEGOS, EstadoComercial.ACTIVO);
    private final Producto retirado = Fixtures.producto(3L, "8000", Categoria.MUSICA, EstadoComercial.RETIRADO);

    @BeforeEach
    void setUp() {
        productoRepo = mock(ProductoRepository.class);
        promoRepo = mock(PromocionRepository.class);
        service = new PricingService(productoRepo, promoRepo, new CalculadoraPromociones());
        when(productoRepo.findAllById(anyIterable())).thenReturn(List.of(a, b, retirado));
    }

    @Test
    void aplicaLaMejorPromocionYCalculaTotales() {
        when(promoRepo.findActivas(any())).thenReturn(List.of(
                Fixtures.promo(10L, TipoDescuento.PORCENTAJE, "10", null, a),
                Fixtures.promo(11L, TipoDescuento.MONTO, "2000", null, a)));

        PricingResponse r = service.cotizar(new PricingRequest(List.of(new PricingRequest.Item(1L, 2))));

        PricingResponse.Linea linea = r.lineas().get(0);
        assertEquals(11L, linea.promocionId());                       // 2000 > 1000 (10%)
        assertEquals(0, new BigDecimal("8000").compareTo(linea.precioFinalUnitario()));
        assertEquals(0, new BigDecimal("20000").compareTo(r.subtotal()));
        assertEquals(0, new BigDecimal("16000").compareTo(r.total()));
    }

    @Test
    void compraMinimaNoAlcanzadaNoAplicaPromocion() {
        when(promoRepo.findActivas(any())).thenReturn(List.of(
                Fixtures.promo(10L, TipoDescuento.PORCENTAJE, "50", "30000", a)));

        PricingResponse r = service.cotizar(new PricingRequest(List.of(new PricingRequest.Item(1L, 1))));

        assertNull(r.lineas().get(0).promocionId());
        assertEquals(0, new BigDecimal("10000").compareTo(r.total()));
    }

    @Test
    void compraMinimaAlcanzadaAplicaPromocion() {
        when(promoRepo.findActivas(any())).thenReturn(List.of(
                Fixtures.promo(10L, TipoDescuento.PORCENTAJE, "50", "30000", a)));

        PricingResponse r = service.cotizar(new PricingRequest(List.of(new PricingRequest.Item(1L, 3))));

        assertEquals(10L, r.lineas().get(0).promocionId());
        assertEquals(0, new BigDecimal("15000").compareTo(r.total()));
    }

    @Test
    void productoRetiradoNoEsComprable() {
        when(promoRepo.findActivas(any())).thenReturn(List.of());
        PricingResponse r = service.cotizar(new PricingRequest(List.of(
                new PricingRequest.Item(2L, 1), new PricingRequest.Item(3L, 1))));
        assertFalse(r.todosComprables());
        assertFalse(r.lineas().get(1).comprable());
        assertEquals(0, new BigDecimal("5000").compareTo(r.total()));
    }

    @Test
    void productoRepetidoSeRechaza() {
        assertThrows(ValidacionNegocioException.class, () -> service.cotizar(new PricingRequest(List.of(
                new PricingRequest.Item(1L, 1), new PricingRequest.Item(1L, 2)))));
    }

    @Test
    void productoInexistenteNoEsComprable() {
        when(promoRepo.findActivas(any())).thenReturn(List.of());
        PricingResponse r = service.cotizar(new PricingRequest(List.of(new PricingRequest.Item(99L, 1))));
        assertNull(r.lineas().get(0).nombre());
        assertEquals("Producto no encontrado", r.lineas().get(0).motivo());
        assertFalse(r.todosComprables());
    }

    @Test
    void empateDeDescuentoGanaLaPromocionDeMenorId() {
        // Mismo descuento (1000) por % y por monto; ID 9 llega primero para probar que no depende del orden
        when(promoRepo.findActivas(any())).thenReturn(List.of(
                Fixtures.promo(9L, TipoDescuento.MONTO, "1000", null, a),
                Fixtures.promo(3L, TipoDescuento.PORCENTAJE, "10", null, a)));
        PricingResponse r = service.cotizar(new PricingRequest(List.of(new PricingRequest.Item(1L, 1))));
        assertEquals(3L, r.lineas().get(0).promocionId());

        when(promoRepo.findActivas(any())).thenReturn(List.of(
                Fixtures.promo(3L, TipoDescuento.PORCENTAJE, "10", null, a),
                Fixtures.promo(9L, TipoDescuento.MONTO, "1000", null, a)));
        r = service.cotizar(new PricingRequest(List.of(new PricingRequest.Item(1L, 1))));
        assertEquals(3L, r.lineas().get(0).promocionId());
    }
}
