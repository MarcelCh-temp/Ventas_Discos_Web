package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.client.InventarioDto;
import com.ventasdiscos.mscatalog.dto.FavoritoResponse;
import com.ventasdiscos.mscatalog.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mscatalog.exception.RecursoDuplicadoException;
import com.ventasdiscos.mscatalog.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.EstadoComercial;
import com.ventasdiscos.mscatalog.model.Favorito;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.repository.FavoritoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-56/HU-57: sin duplicados, solo favoritos propios. */
class FavoritoServiceTest {

    private FavoritoRepository repo;
    private ProductoService productoService;
    private FavoritoService service;

    @BeforeEach
    void setUp() {
        repo = mock(FavoritoRepository.class);
        productoService = mock(ProductoService.class);
        service = new FavoritoService(repo, productoService);
    }

    @Test
    void noPermiteDuplicado() {
        when(productoService.buscar(1L)).thenReturn(
                Fixtures.producto(1L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO));
        when(repo.existsByUserIdAndProductoId(7L, 1L)).thenReturn(true);
        assertThrows(RecursoDuplicadoException.class, () -> service.agregar(7L, 1L));
        verify(repo, never()).save(any());
    }

    @Test
    void noPermiteAgregarRetirado() {
        when(productoService.buscar(1L)).thenReturn(
                Fixtures.producto(1L, "1000", Categoria.MUSICA, EstadoComercial.RETIRADO));
        assertThrows(OperacionNoPermitidaException.class, () -> service.agregar(7L, 1L));
    }

    @Test
    void eliminarFavoritoAjenoOInexistenteDa404() {
        when(repo.findByUserIdAndProductoId(7L, 1L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(7L, 1L));
    }

    @Test
    void agregarFavoritoValido() {
        when(productoService.buscar(1L)).thenReturn(
                Fixtures.producto(1L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO));
        when(productoService.inventarioSeguro(anyCollection())).thenReturn(
                Map.of(1L, new InventarioDto(1L, 0, 5, "AGOTADO")));
        FavoritoResponse r = service.agregar(7L, 1L);
        assertEquals("Agotado", r.mensaje());
        assertFalse(r.comprable());
        verify(repo).save(any(Favorito.class));
    }

    @Test
    void eliminarFavoritoPropio() {
        Favorito f = new Favorito(7L, Fixtures.producto(1L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO));
        when(repo.findByUserIdAndProductoId(7L, 1L)).thenReturn(Optional.of(f));
        service.eliminar(7L, 1L);
        verify(repo).delete(f);
    }

    @Test
    void listarMuestraEstadoActualDeCadaProducto() {
        Producto retirado = Fixtures.producto(1L, "1000", Categoria.MUSICA, EstadoComercial.RETIRADO);
        Producto agotado = Fixtures.producto(2L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO);
        Producto sinDato = Fixtures.producto(3L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO);
        Producto disponible = Fixtures.producto(4L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO);
        when(repo.findByUserIdOrderByIdDesc(7L)).thenReturn(List.of(
                new Favorito(7L, retirado), new Favorito(7L, agotado), new Favorito(7L, sinDato),
                new Favorito(7L, disponible)));
        when(productoService.inventarioSeguro(anyCollection())).thenReturn(Map.of(
                1L, new InventarioDto(1L, 5, 5, "STOCK_CRITICO"),
                2L, new InventarioDto(2L, 0, 5, "AGOTADO"),
                4L, new InventarioDto(4L, 9, 5, "DISPONIBLE")));

        List<FavoritoResponse> r = service.listar(7L);

        assertEquals("Este producto ya no se encuentra disponible.", r.get(0).mensaje());
        assertFalse(r.get(0).comprable());
        assertEquals("Agotado", r.get(1).mensaje());
        assertEquals("Disponibilidad no confirmada en este momento", r.get(2).mensaje());
        assertNull(r.get(3).mensaje());
        assertTrue(r.get(3).comprable());
    }
}
