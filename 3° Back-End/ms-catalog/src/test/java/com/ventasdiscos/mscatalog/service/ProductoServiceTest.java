package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.client.InventarioDto;
import com.ventasdiscos.mscatalog.client.InventoryClient;
import com.ventasdiscos.mscatalog.dto.FiltroProducto;
import com.ventasdiscos.mscatalog.dto.PageResponse;
import com.ventasdiscos.mscatalog.dto.ProductoAdminResponse;
import com.ventasdiscos.mscatalog.dto.ProductoPublicoResponse;
import com.ventasdiscos.mscatalog.dto.ProductoRequest;
import com.ventasdiscos.mscatalog.dto.RetiroRequest;
import com.ventasdiscos.mscatalog.dto.RetiroResponse;
import com.ventasdiscos.mscatalog.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mscatalog.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mscatalog.exception.RecursoRetiradoException;
import com.ventasdiscos.mscatalog.exception.ServicioExternoNoDisponibleException;
import com.ventasdiscos.mscatalog.exception.ValidacionNegocioException;
import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.DisponibilidadCatalogo;
import com.ventasdiscos.mscatalog.model.EstadoComercial;
import com.ventasdiscos.mscatalog.model.Producto;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Reglas de EP-02/EP-03 y degradación cuando Inventory no responde. */
class ProductoServiceTest {

    private ProductoRepository repo;
    private PromocionRepository promoRepo;
    private InventoryClient inventory;
    private ProductoService service;

    @BeforeEach
    void setUp() {
        repo = mock(ProductoRepository.class);
        promoRepo = mock(PromocionRepository.class);
        inventory = mock(InventoryClient.class);
        service = new ProductoService(repo, promoRepo, inventory, new CalculadoraPromociones());
        when(repo.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(promoRepo.findActivas(any())).thenReturn(List.of());
    }

    private ProductoRequest request(Categoria categoria, String plataforma) {
        return new ProductoRequest("Disco", "desc", new BigDecimal("9990"), categoria, "Rock", "CD", plataforma, null);
    }

    @Test
    void crearDejaProductoActivoConIdDelSistema() {
        when(inventory.disponibilidad(anyCollection())).thenReturn(
                Map.of(1L, new InventarioDto(1L, 0, 5, "AGOTADO")));
        ProductoAdminResponse r = service.crear(request(Categoria.MUSICA, null));
        assertEquals(1L, r.id());
        assertEquals(EstadoComercial.ACTIVO, r.estado());
        assertEquals(DisponibilidadCatalogo.AGOTADO, r.disponibilidad());
    }

    @Test
    void crearFuncionaAunqueInventoryEsteCaido() {
        doThrow(new ServicioExternoNoDisponibleException("caído")).when(inventory).registrarProducto(1L);
        when(inventory.disponibilidad(anyCollection())).thenThrow(new ServicioExternoNoDisponibleException("caído"));
        ProductoAdminResponse r = service.crear(request(Categoria.MUSICA, null));
        assertEquals(DisponibilidadCatalogo.NO_CONFIRMADA, r.disponibilidad());
        assertNull(r.stockActual());
    }

    @Test
    void videojuegoSinPlataformaEsIncoherente() {
        assertThrows(ValidacionNegocioException.class, () -> service.crear(request(Categoria.VIDEOJUEGOS, null)));
    }

    @Test
    void musicaConPlataformaEsIncoherente() {
        assertThrows(ValidacionNegocioException.class, () -> service.crear(request(Categoria.MUSICA, "PS5")));
    }

    @Test
    void retirarInformaLosQueYaEstabanRetirados() {
        Producto activo = Fixtures.producto(1L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO);
        Producto retirado = Fixtures.producto(2L, "1000", Categoria.MUSICA, EstadoComercial.RETIRADO);
        when(repo.findAllById(anyIterable())).thenReturn(List.of(activo, retirado));

        RetiroResponse r = service.retirar(new RetiroRequest(List.of(1L, 2L, 3L)));

        assertEquals(List.of(1L), r.retirados());
        assertEquals(List.of(2L), r.yaRetirados());
        assertEquals(List.of(3L), r.noEncontrados());
        assertEquals(EstadoComercial.RETIRADO, activo.getEstado());
    }

    @Test
    void retirarProductosYaRetiradosNoRepiteOperacion() {
        Producto retirado = Fixtures.producto(2L, "1000", Categoria.MUSICA, EstadoComercial.RETIRADO);
        when(repo.findAllById(anyIterable())).thenReturn(List.of(retirado));
        assertThrows(OperacionNoPermitidaException.class, () -> service.retirar(new RetiroRequest(List.of(2L))));
    }

    @Test
    void detallePublicoDeRetiradoDa410ConMensaje() {
        when(repo.findById(5L)).thenReturn(Optional.of(
                Fixtures.producto(5L, "1000", Categoria.MUSICA, EstadoComercial.RETIRADO)));
        RecursoRetiradoException ex = assertThrows(RecursoRetiradoException.class, () -> service.obtenerPublico(5L));
        assertEquals("Este producto ya no se encuentra disponible.", ex.getMessage());
    }

    @Test
    void detallePublicoAgotadoSeMuestraPeroNoEsComprable() {
        when(repo.findById(5L)).thenReturn(Optional.of(
                Fixtures.producto(5L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO)));
        when(inventory.disponibilidad(anyCollection())).thenReturn(
                Map.of(5L, new InventarioDto(5L, 0, 5, "AGOTADO")));
        ProductoPublicoResponse r = service.obtenerPublico(5L);
        assertEquals(DisponibilidadCatalogo.AGOTADO, r.disponibilidad());
        assertFalse(r.comprable());
    }

    @Test
    void detallePublicoConStockEsComprable() {
        when(repo.findById(5L)).thenReturn(Optional.of(
                Fixtures.producto(5L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO)));
        when(inventory.disponibilidad(anyCollection())).thenReturn(
                Map.of(5L, new InventarioDto(5L, 3, 5, "STOCK_CRITICO")));
        assertTrue(service.obtenerPublico(5L).comprable());
    }

    // ---------- HU-09 / HU-11 ----------

    @Test
    void actualizarYDetalleAdmin() {
        Producto p = Fixtures.producto(5L, "1000", Categoria.VIDEOJUEGOS, EstadoComercial.ACTIVO);
        when(repo.findById(5L)).thenReturn(Optional.of(p));
        when(inventory.disponibilidad(anyCollection())).thenReturn(Map.of(5L, new InventarioDto(5L, 9, 5, "DISPONIBLE")));
        ProductoAdminResponse r = service.actualizar(5L, new ProductoRequest("Juego", null, new BigDecimal("4990"),
                Categoria.VIDEOJUEGOS, "Acción", "Físico", " PS5 ", null));
        assertEquals("PS5", r.plataforma());
        assertEquals(9, r.stockActual());
        assertEquals(DisponibilidadCatalogo.DISPONIBLE, service.obtenerAdmin(5L).disponibilidad());
    }

    @Test
    void productoInexistenteDa404() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerAdmin(9L));
    }

    @Test
    void retirarProductosInexistentesDa404() {
        when(repo.findAllById(anyIterable())).thenReturn(List.of());
        assertThrows(RecursoNoEncontradoException.class, () -> service.retirar(new RetiroRequest(List.of(8L))));
    }

    // ---------- HU-52 en el detalle ----------

    @Test
    void detalleMuestraPrecioDeOferta() {
        Producto p = Fixtures.producto(5L, "10000", Categoria.MUSICA, EstadoComercial.ACTIVO);
        when(repo.findById(5L)).thenReturn(Optional.of(p));
        when(promoRepo.findActivas(any())).thenReturn(List.of(
                Fixtures.promo(1L, TipoDescuento.PORCENTAJE, "20", null, p)));
        ProductoPublicoResponse r = service.obtenerPublico(5L);
        assertEquals(0, new BigDecimal("8000").compareTo(r.precioOferta()));
        assertEquals("Promo 1", r.promocion());
    }

    // ---------- HU-11..16: listados y filtros ----------

    @SuppressWarnings("unchecked")
    private void repoDevuelve(Producto... productos) {
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(productos), PageRequest.of(0, 20), productos.length));
    }

    private FiltroProducto filtro(DisponibilidadCatalogo disponibilidad, String min, String max) {
        return new FiltroProducto(null, null, null, null, null, null,
                min == null ? null : new BigDecimal(min), max == null ? null : new BigDecimal(max), disponibilidad);
    }

    @Test
    void listadoPublicoConDisponibilidad() {
        repoDevuelve(Fixtures.producto(1L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO));
        when(inventory.disponibilidad(anyCollection())).thenReturn(Map.of(1L, new InventarioDto(1L, 9, 5, "DISPONIBLE")));
        PageResponse<ProductoPublicoResponse> r = service.listarPublico(filtro(null, null, null), 0, 20);
        assertTrue(r.contenido().get(0).comprable());
        assertEquals(1, r.totalElementos());
    }

    @Test
    void listadoAdminConInventoryCaidoNoAfirmaStock() {
        repoDevuelve(Fixtures.producto(1L, "1000", Categoria.MUSICA, EstadoComercial.RETIRADO));
        when(inventory.disponibilidad(anyCollection())).thenThrow(new ServicioExternoNoDisponibleException("caído"));
        ProductoAdminResponse r = service.listarAdmin(filtro(null, null, null), 0, 20).contenido().get(0);
        assertNull(r.stockActual());
        assertEquals(DisponibilidadCatalogo.NO_CONFIRMADA, r.disponibilidad());
    }

    @Test
    void filtroDisponibleSinProductosDevuelvePaginaVacia() {
        when(inventory.productIdsPorDisponibilidad("DISPONIBLE")).thenReturn(List.of());
        assertEquals(0, service.listarPublico(filtro(DisponibilidadCatalogo.DISPONIBLE, null, null), 0, 20)
                .totalElementos());
        assertEquals(0, service.listarAdmin(filtro(DisponibilidadCatalogo.DISPONIBLE, null, null), 0, 20)
                .totalElementos());
    }

    @Test
    void filtrosDeDisponibilidadConsultanInventory() {
        repoDevuelve(Fixtures.producto(1L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO));
        when(inventory.productIdsPorDisponibilidad("STOCK_CRITICO")).thenReturn(List.of(1L));
        when(inventory.productIdsPorDisponibilidad("DISPONIBLE")).thenReturn(List.of(2L));
        assertEquals(1, service.listarPublico(filtro(DisponibilidadCatalogo.STOCK_CRITICO, null, null), 0, 20)
                .totalElementos());
        assertEquals(1, service.listarPublico(filtro(DisponibilidadCatalogo.AGOTADO, null, null), 0, 20)
                .totalElementos());
    }

    @Test
    void filtroDisponibilidadInvalidoOInventoryCaido() {
        assertThrows(ValidacionNegocioException.class,
                () -> service.listarPublico(filtro(DisponibilidadCatalogo.NO_CONFIRMADA, null, null), 0, 20));
        when(inventory.productIdsPorDisponibilidad("DISPONIBLE")).thenThrow(new ServicioExternoNoDisponibleException("x"));
        assertThrows(ServicioExternoNoDisponibleException.class,
                () -> service.listarPublico(filtro(DisponibilidadCatalogo.DISPONIBLE, null, null), 0, 20));
    }

    @Test
    void rangosDePrecioInvalidos() {
        assertThrows(ValidacionNegocioException.class, () -> service.listarPublico(filtro(null, "-1", null), 0, 20));
        assertThrows(ValidacionNegocioException.class, () -> service.listarPublico(filtro(null, null, "-1"), 0, 20));
        assertThrows(ValidacionNegocioException.class, () -> service.listarPublico(filtro(null, "10", "5"), 0, 20));
    }
}
