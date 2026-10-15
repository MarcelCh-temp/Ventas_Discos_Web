package com.ventasdiscos.mscatalog.client;

import com.ventasdiscos.mscatalog.exception.ServicioExternoNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Comunicación Catalog -> Inventory con un servidor HTTP simulado:
 * llamada correcta y comportamiento ante fallo remoto (prompt §19 "Clientes REST").
 */
class InventoryClientTest {

    private static final String BASE = "http://inventory/api/v1/internal/inventory";
    private MockRestServiceServer server;
    private InventoryClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new InventoryClient(restTemplate, "http://inventory");
    }

    @Test
    void disponibilidadPorLote() {
        server.expect(requestTo(BASE + "/availability?productIds=1,2")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"productId":1,"stockActual":3,"umbralCritico":5,"disponibilidad":"STOCK_CRITICO"},
                         {"productId":2,"stockActual":0,"umbralCritico":5,"disponibilidad":"AGOTADO"}]
                        """, MediaType.APPLICATION_JSON));

        Map<Long, InventarioDto> r = client.disponibilidad(List.of(1L, 2L));

        assertEquals("STOCK_CRITICO", r.get(1L).disponibilidad());
        assertEquals(0, r.get(2L).stockActual());
        server.verify();
    }

    @Test
    void sinIdsNoLlamaAInventory() {
        assertTrue(client.disponibilidad(List.of()).isEmpty());
        server.verify();
    }

    @Test
    void respuestasVaciasSeTratanComoListaVacia() {
        server.expect(requestTo(BASE + "/availability?productIds=1")).andRespond(withNoContent());
        server.expect(requestTo(BASE + "/product-ids?disponibilidad=AGOTADO")).andRespond(withNoContent());
        assertTrue(client.disponibilidad(List.of(1L)).isEmpty());
        assertTrue(client.productIdsPorDisponibilidad("AGOTADO").isEmpty());
    }

    @Test
    void idsPorDisponibilidad() {
        server.expect(requestTo(BASE + "/product-ids?disponibilidad=DISPONIBLE"))
                .andRespond(withSuccess("[4,5]", MediaType.APPLICATION_JSON));
        assertEquals(List.of(4L, 5L), client.productIdsPorDisponibilidad("DISPONIBLE"));
    }

    @Test
    void registrarProducto() {
        server.expect(requestTo(BASE + "/products/9")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());
        client.registrarProducto(9L);
        server.verify();
    }

    @Test
    void inventoryCaidoLanzaServicioNoDisponible() {
        server.expect(requestTo(BASE + "/availability?productIds=1")).andRespond(withServerError());
        server.expect(requestTo(BASE + "/product-ids?disponibilidad=DISPONIBLE")).andRespond(withServerError());
        server.expect(requestTo(BASE + "/products/1")).andRespond(withServerError());
        assertThrows(ServicioExternoNoDisponibleException.class, () -> client.disponibilidad(List.of(1L)));
        assertThrows(ServicioExternoNoDisponibleException.class, () -> client.productIdsPorDisponibilidad("DISPONIBLE"));
        assertThrows(ServicioExternoNoDisponibleException.class, () -> client.registrarProducto(1L));
    }
}
