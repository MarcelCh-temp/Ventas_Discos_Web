package ms_ordenes.ms_ordenes;

import ms_ordenes.ms_ordenes.client.AccountsClient;
import ms_ordenes.ms_ordenes.client.CatalogClient;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.*;
import ms_ordenes.ms_ordenes.client.NotificacionesClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class CarritoConcurrenciaTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean CatalogClient catalog;
    @MockitoBean AccountsClient accounts;
    @MockitoBean NotificacionesClient notifications;

    @Test
    void dosSolicitudesSimultaneasCreanUnSoloCarrito() throws Exception {
        long userId = 987654321L;

        when(catalog.cotizar(anyList())).thenAnswer(inv -> {
            List<ItemSolicitado> items = inv.getArgument(0);
            List<LineaCotizada> lineas = items.stream().map(i ->
                new LineaCotizada(
                    i.productId(), "Producto de prueba", i.cantidad(),
                    new BigDecimal("1000"), BigDecimal.ZERO,
                    new BigDecimal("1000"),
                    new BigDecimal("1000").multiply(BigDecimal.valueOf(i.cantidad())),
                    null, true, null
                )
            ).toList();

            BigDecimal total = lineas.stream()
                .map(LineaCotizada::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new Cotizacion(lineas, total, BigDecimal.ZERO, total, true);
        });

        when(catalog.disponibilidad(anyList())).thenAnswer(inv -> {
            List<ItemSolicitado> items = inv.getArgument(0);
            return items.stream()
                .map(i -> new Disponibilidad(
                    i.productId(), i.cantidad(), "DISPONIBLE", true, true
                ))
                .toList();
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);

        try {
            Callable<Integer> solicitud = () -> {
                inicio.await();
                return mvc.perform(
                    post("/api/v1/cart/items")
                        .header("Authorization", TestTokens.cliente(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"cantidad\":1}")
                ).andReturn().getResponse().getStatus();
            };

            Future<Integer> primera = executor.submit(solicitud);
            Future<Integer> segunda = executor.submit(solicitud);

            inicio.countDown();

            int estado1 = primera.get(15, TimeUnit.SECONDS);
            int estado2 = segunda.get(15, TimeUnit.SECONDS);

            Integer carritos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM carrito WHERE user_id = ?",
                Integer.class, userId
            );

            Integer lineasProducto = jdbc.queryForObject(
                "SELECT COUNT(*) FROM item_carrito i " +
                "JOIN carrito c ON c.id = i.carrito_id " +
                "WHERE c.user_id = ? AND i.product_id = ?",
                Integer.class, userId, 1L
            );

            Integer unidadesProducto = jdbc.queryForObject(
                "SELECT COALESCE(SUM(i.cantidad), 0) FROM item_carrito i " +
                "JOIN carrito c ON c.id = i.carrito_id " +
                "WHERE c.user_id = ? AND i.product_id = ?",
                Integer.class, userId, 1L
            );

            assertEquals(1, carritos, "Debe existir solo un carrito por usuario");
            assertEquals(1, lineasProducto, "Debe existir una sola línea para el producto");
            assertEquals(2, unidadesProducto, "Las dos solicitudes deben sumar dos unidades");
            assertEquals(200, estado1, "Primera solicitud");
            assertEquals(200, estado2, "Segunda solicitud");
        } finally {
            executor.shutdownNow();
        }
    }
}
