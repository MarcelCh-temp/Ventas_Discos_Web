package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.dto.PricingRequest;
import com.ventasdiscos.mscatalog.dto.PricingResponse;
import com.ventasdiscos.mscatalog.exception.ValidacionNegocioException;
import com.ventasdiscos.mscatalog.model.Producto;
import com.ventasdiscos.mscatalog.model.Promocion;
import com.ventasdiscos.mscatalog.repository.ProductoRepository;
import com.ventasdiscos.mscatalog.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Cotiza un carrito para MS-Orders (HU-18, HU-24, HU-25, HU-52).
 * Catalog es dueño del precio y las promociones; Orders guarda el resultado como snapshot.
 * La compra mínima se evalúa sobre el subtotal SIN descuentos de los productos comprables.
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private final ProductoRepository productoRepository;
    private final PromocionRepository promocionRepository;
    private final CalculadoraPromociones calculadora;

    @Transactional(readOnly = true)
    public PricingResponse cotizar(PricingRequest req) {
        Set<Long> vistos = new HashSet<>();
        for (PricingRequest.Item item : req.items()) {
            if (!vistos.add(item.productId())) {
                throw new ValidacionNegocioException("Producto repetido en el carrito: " + item.productId());
            }
        }
        Map<Long, Producto> productos = new HashMap<>();
        productoRepository.findAllById(vistos).forEach(p -> productos.put(p.getId(), p));
        List<Promocion> activas = promocionRepository.findActivas(LocalDate.now());

        // 1) subtotal base de lo comprable (para evaluar compra mínima)
        BigDecimal subtotalBase = BigDecimal.ZERO;
        for (PricingRequest.Item item : req.items()) {
            Producto p = productos.get(item.productId());
            if (p != null && p.estaActivo()) {
                subtotalBase = subtotalBase.add(p.getPrecioBase().multiply(BigDecimal.valueOf(item.cantidad())));
            }
        }

        // 2) líneas con la mejor promoción aplicable
        List<PricingResponse.Linea> lineas = new ArrayList<>();
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        boolean todosComprables = true;
        for (PricingRequest.Item item : req.items()) {
            Producto p = productos.get(item.productId());
            if (p == null || !p.estaActivo()) {
                todosComprables = false;
                lineas.add(new PricingResponse.Linea(item.productId(), p == null ? null : p.getNombre(),
                        item.cantidad(), null, null, null, BigDecimal.ZERO, null, false,
                        p == null ? "Producto no encontrado" : ProductoService.MSG_RETIRADO));
                continue;
            }
            BigDecimal precio = p.getPrecioBase();
            Optional<Promocion> promo = calculadora.mejorPromocion(p, activas, subtotalBase);
            BigDecimal descuento = promo.map(pr -> pr.descuentoUnitario(precio)).orElse(BigDecimal.ZERO);
            BigDecimal precioFinal = precio.subtract(descuento);
            BigDecimal cantidad = BigDecimal.valueOf(item.cantidad());
            descuentoTotal = descuentoTotal.add(descuento.multiply(cantidad));
            lineas.add(new PricingResponse.Linea(p.getId(), p.getNombre(), item.cantidad(), precio, descuento,
                    precioFinal, precioFinal.multiply(cantidad), promo.map(Promocion::getId).orElse(null),
                    true, null));
        }
        return new PricingResponse(lineas, subtotalBase, descuentoTotal, subtotalBase.subtract(descuentoTotal),
                todosComprables);
    }
}
