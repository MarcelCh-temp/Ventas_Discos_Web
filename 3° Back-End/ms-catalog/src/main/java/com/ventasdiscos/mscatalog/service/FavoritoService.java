package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.client.AccountsClient;
import com.ventasdiscos.mscatalog.dto.DepuracionResponse;
import com.ventasdiscos.mscatalog.dto.FavoritoResponse;
import com.ventasdiscos.mscatalog.dto.InventarioResponse;
import com.ventasdiscos.mscatalog.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mscatalog.exception.RecursoDuplicadoException;
import com.ventasdiscos.mscatalog.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mscatalog.model.DisponibilidadCatalogo;
import com.ventasdiscos.mscatalog.model.Favorito;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.repository.FavoritoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Reglas de EP-11 (HU-56..58). userId siempre sale del token (RNF-07). */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final ProductoService productoService;
    /** Lote 5 (integración de HU-78): confirma con Accounts que la cuenta está ACTIVA antes de agregar. */
    private final AccountsClient accountsClient;

    /** HU-56: sin duplicados; se permite aunque esté agotado, no si ya fue retirado. */
    @Transactional
    public FavoritoResponse agregar(Long userId, Long productId) {
        Producto producto = productoService.buscar(productId);
        if (!producto.estaActivo()) {
            throw new OperacionNoPermitidaException(ProductoService.MSG_RETIRADO);
        }
        if (favoritoRepository.existsByUserIdAndProductoId(userId, productId)) {
            throw new RecursoDuplicadoException("El producto ya está en tus favoritos");
        }
        // Lote 5 (HU-78, RN-37, RN-44, T-06): justo antes de guardar, Accounts confirma que la cuenta del token está
        // ACTIVA. Si no (403) o no responde (503), se lanza la excepción y no se guarda el favorito.
        accountsClient.exigirCuentaActiva(userId);
        favoritoRepository.save(new Favorito(userId, producto));
        return toResponse(producto, productoService.existencias(List.of(producto)));
    }

    /**
     * Lote 5 · Contrato interno para Accounts (HU-78, RN-43, RF-FAV-03): depuración idempotente de los favoritos de
     * una cuenta que se está eliminando. Repetirla no duplica efectos (la segunda vez borra 0).
     */
    @Transactional
    public DepuracionResponse depurarUsuario(Long userId) {
        int eliminados = favoritoRepository.borrarPorUsuario(userId);
        log.info("Favoritos del usuario {} depurados: {}", userId, eliminados);
        return new DepuracionResponse(userId, eliminados);
    }

    /** HU-57: solo un favorito propio. */
    @Transactional
    public void eliminar(Long userId, Long productId) {
        Favorito favorito = favoritoRepository.findByUserIdAndProductoId(userId, productId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto no está en tus favoritos"));
        favoritoRepository.delete(favorito);
    }

    /** HU-58: estado actual de cada producto (RN-29). */
    @Transactional(readOnly = true)
    public List<FavoritoResponse> listar(Long userId) {
        List<Favorito> favoritos = favoritoRepository.findByUserIdOrderByIdDesc(userId);
        // Lote 2: existencias locales (antes HTTP a Inventory)
        Map<Long, InventarioResponse> inv = productoService.existencias(
                favoritos.stream().map(Favorito::getProducto).toList());
        return favoritos.stream().map(f -> toResponse(f.getProducto(), inv)).toList();
    }

    private FavoritoResponse toResponse(Producto p, Map<Long, InventarioResponse> inv) {
        DisponibilidadCatalogo disp = ProductoService.disponibilidad(inv, p.getId());
        String mensaje = null;
        if (!p.estaActivo()) {
            mensaje = ProductoService.MSG_RETIRADO;
        } else if (disp == DisponibilidadCatalogo.AGOTADO) {
            mensaje = "Agotado";
        } else if (disp == DisponibilidadCatalogo.NO_CONFIRMADA) {
            mensaje = "Disponibilidad no confirmada en este momento";
        }
        return new FavoritoResponse(p.getId(), p.getNombre(), p.getImagenUrl(), p.getPrecioBase(), p.getEstado(),
                disp, p.estaActivo() && disp.permiteCompra(), mensaje);
    }
}
