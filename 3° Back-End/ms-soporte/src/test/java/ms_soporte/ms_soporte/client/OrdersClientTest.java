package ms_soporte.ms_soporte.client;

import ms_soporte.ms_soporte.client.OrdersClient.PedidoOrders;
import ms_soporte.ms_soporte.exception.RecursoNoEncontradoException;
import ms_soporte.ms_soporte.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cierre · RF-SOP-01 / HU-59: Support -> Orders. RestTemplate simulado: no se levanta Orders. (Sin ejecutar.) */
class OrdersClientTest {

    private static final String URL = "http://127.0.0.1:8084/api/v1/internal/orders/{id}";
    private RestTemplate restTemplate;
    private OrdersClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new OrdersClient(restTemplate, "http://127.0.0.1:8084");
    }

    private static PedidoOrders pedido(Long userId) {
        return new PedidoOrders(5L, "AK-000005", userId, "ENTREGADO", null, BigDecimal.TEN, List.of());
    }

    private static HttpClientErrorException error(HttpStatus status) {
        return HttpClientErrorException.create(status, status.getReasonPhrase(), new HttpHeaders(),
                new byte[0], StandardCharsets.UTF_8);
    }

    @Test
    void pedidoPropio_permiteCrear() {
        when(restTemplate.getForObject(URL, PedidoOrders.class, 5L)).thenReturn(pedido(100L));
        assertDoesNotThrow(() -> client.exigirPedidoPropio(5L, 100L));
    }

    @Test
    void pedidoDeOtroCliente_404ConElMismoMensajeQueUnoInexistente() {
        when(restTemplate.getForObject(URL, PedidoOrders.class, 5L)).thenReturn(pedido(200L));
        RecursoNoEncontradoException ajeno = assertThrows(RecursoNoEncontradoException.class,
                () -> client.exigirPedidoPropio(5L, 100L));
        when(restTemplate.getForObject(URL, PedidoOrders.class, 6L)).thenThrow(error(HttpStatus.NOT_FOUND));
        RecursoNoEncontradoException inexistente = assertThrows(RecursoNoEncontradoException.class,
                () -> client.exigirPedidoPropio(6L, 100L));
        assertEquals(inexistente.getMessage(), ajeno.getMessage());
    }

    @Test
    void ordersNoDisponible_503TrasUnReintento() {
        when(restTemplate.getForObject(URL, PedidoOrders.class, 5L)).thenThrow(new ResourceAccessException("Connection refused"));
        assertThrows(ServicioNoDisponibleException.class, () -> client.exigirPedidoPropio(5L, 100L));
        verify(restTemplate, times(OrdersClient.MAX_INTENTOS)).getForObject(URL, PedidoOrders.class, 5L);
    }

    @Test
    void error5xxSeReintentaYLuegoResponde() {
        when(restTemplate.getForObject(URL, PedidoOrders.class, 5L))
                .thenThrow(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "x", new HttpHeaders(),
                        new byte[0], StandardCharsets.UTF_8))
                .thenReturn(pedido(100L));
        assertDoesNotThrow(() -> client.exigirPedidoPropio(5L, 100L));
    }

    @Test
    void respuestaSinDuenoUOtro4xx_503NuncaSeAsumeValido() {
        when(restTemplate.getForObject(URL, PedidoOrders.class, 5L)).thenReturn(pedido(null));
        assertThrows(ServicioNoDisponibleException.class, () -> client.exigirPedidoPropio(5L, 100L));
        when(restTemplate.getForObject(URL, PedidoOrders.class, 7L)).thenThrow(error(HttpStatus.FORBIDDEN));
        assertThrows(ServicioNoDisponibleException.class, () -> client.exigirPedidoPropio(7L, 100L));
    }
}
