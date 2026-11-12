package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.client.AccountsClient;
import com.ventasdiscos.mscatalog.dto.FavoritoResponse;
import com.ventasdiscos.mscatalog.exception.AccesoDenegadoException;
import com.ventasdiscos.mscatalog.exception.ServicioExternoNoDisponibleException;
import com.ventasdiscos.mscatalog.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mscatalog.exception.RecursoDuplicadoException;
import com.ventasdiscos.mscatalog.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mscatalog.model.Categoria;
import com.ventasdiscos.mscatalog.model.Disponibilidad;
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
import static org.mockito.Mockito.doThrow;
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
    private AccountsClient cuentas;
    private FavoritoService service;

    @BeforeEach
    void setUp() {
        repo = mock(FavoritoRepository.class);
        productoService = mock(ProductoService.class);
        cuentas = mock(AccountsClient.class);      // Lote 5 (HU-78): por defecto no lanza nada = cuenta ACTIVA
        service = new FavoritoService(repo, productoService, cuentas);
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
        when(productoService.existencias(anyCollection())).thenReturn(
                Map.of(1L, Fixtures.existencia(1L, 0, Disponibilidad.AGOTADO)));
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
        when(productoService.existencias(anyCollection())).thenReturn(Map.of(
                1L, Fixtures.existencia(1L, 5, Disponibilidad.STOCK_CRITICO),
                2L, Fixtures.existencia(2L, 0, Disponibilidad.AGOTADO),
                4L, Fixtures.existencia(4L, 9, Disponibilidad.DISPONIBLE)));

        List<FavoritoResponse> r = service.listar(7L);

        assertEquals("Este producto ya no se encuentra disponible.", r.get(0).mensaje());
        assertFalse(r.get(0).comprable());
        assertEquals("Agotado", r.get(1).mensaje());
        assertEquals("Disponibilidad no confirmada en este momento", r.get(2).mensaje());
        assertNull(r.get(3).mensaje());
        assertTrue(r.get(3).comprable());
    }

    // ===================== Lote 5 · Integraciones de HU-78 =====================

    /** RN-37 / RN-44 / T-06: cuenta EN_ELIMINACION, ELIMINADA o no habilitada -> 403 y no se guarda. */
    @Test
    void cuentaQueNoPuedeOperarNoAgregaFavoritos() {
        when(productoService.buscar(1L)).thenReturn(Fixtures.producto(1L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO));
        doThrow(new AccesoDenegadoException("Tu cuenta no puede realizar nuevas operaciones")).when(cuentas).exigirCuentaActiva(7L);
        assertThrows(AccesoDenegadoException.class, () -> service.agregar(7L, 1L));
        verify(repo, never()).save(any());
    }

    /** Accounts no responde -> 503 y no se guarda (nunca se asume ACTIVA). */
    @Test
    void accountsCaidoNoAgregaFavoritos() {
        when(productoService.buscar(1L)).thenReturn(Fixtures.producto(1L, "1000", Categoria.MUSICA, EstadoComercial.ACTIVO));
        doThrow(new ServicioExternoNoDisponibleException("caído")).when(cuentas).exigirCuentaActiva(7L);
        assertThrows(ServicioExternoNoDisponibleException.class, () -> service.agregar(7L, 1L));
        verify(repo, never()).save(any());
    }

    /** RN-43: la depuración devuelve cuántos borró; repetirla borra 0 (idempotente). */
    @Test
    void depuracionIdempotente() {
        when(repo.borrarPorUsuario(7L)).thenReturn(3).thenReturn(0);
        assertEquals(3, service.depurarUsuario(7L).eliminados());
        assertEquals(0, service.depurarUsuario(7L).eliminados());
    }
}
