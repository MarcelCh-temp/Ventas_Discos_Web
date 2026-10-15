package com.ventasdiscos.mscatalog.client;

import com.ventasdiscos.mscatalog.exception.ServicioExternoNoDisponibleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Único lugar donde Catalog habla HTTP con MS-Inventory (RestTemplate @Bean de RestTemplateConfig).
 * Timeout corto (configurado en el @Bean): si Inventory no responde, lanzamos ServicioExternoNoDisponibleException y
 * CADA caso de uso decide si degrada (mostrar NO_CONFIRMADA) o corta (filtro por disponibilidad).
 */
@Slf4j
@Component
public class InventoryClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public InventoryClient(RestTemplate restTemplate, @Value("${services.inventory.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl + "/api/v1";
    }

    /** Disponibilidad de varios productos en UNA llamada (evita N+1, prompt "Paginación"). */
    public Map<Long, InventarioDto> disponibilidad(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        String ids = productIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            InventarioDto[] resp = restTemplate.getForObject(
                    baseUrl + "/internal/inventory/availability?productIds=" + ids, InventarioDto[].class);
            if (resp == null) {
                return Map.of();
            }
            return Arrays.stream(resp).collect(Collectors.toMap(InventarioDto::productId, Function.identity()));
        } catch (RestClientException e) {
            throw noDisponible(e);
        }
    }

    /** Ids de productos con cierta disponibilidad (DISPONIBLE, STOCK_CRITICO, AGOTADO). */
    public List<Long> productIdsPorDisponibilidad(String disponibilidad) {
        try {
            Long[] resp = restTemplate.getForObject(
                    baseUrl + "/internal/inventory/product-ids?disponibilidad=" + disponibilidad, Long[].class);
            return resp == null ? List.of() : List.of(resp);
        } catch (RestClientException e) {
            throw noDisponible(e);
        }
    }

    /** Avisa a Inventory que existe un producto nuevo (se registra con stock 0). */
    public void registrarProducto(Long productId) {
        try {
            restTemplate.postForEntity(baseUrl + "/internal/inventory/products/" + productId, null, Void.class);
        } catch (RestClientException e) {
            throw noDisponible(e);
        }
    }

    private ServicioExternoNoDisponibleException noDisponible(RestClientException e) {
        log.warn("MS-Inventory no disponible: {}", e.getMessage());
        return new ServicioExternoNoDisponibleException(
                "No fue posible consultar el inventario en este momento. Intenta nuevamente");
    }
}
