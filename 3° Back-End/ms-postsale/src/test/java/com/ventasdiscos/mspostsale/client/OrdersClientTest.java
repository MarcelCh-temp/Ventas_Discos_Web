package com.ventasdiscos.mspostsale.client;

import com.ventasdiscos.mspostsale.client.OrdersDtos.CancelacionOrders;
import com.ventasdiscos.mspostsale.client.OrdersDtos.PedidoOrders;
import com.ventasdiscos.mspostsale.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mspostsale.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mspostsale.exception.ServicioNoDisponibleException;
import com.ventasdiscos.mspostsale.exception.ValidacionNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lote 4: PostSale -> contratos internos de Orders (Lote 3). RestTemplate simulado: no se levanta Orders. */
class OrdersClientTest {

    private static final String PEDIDO = "http://127.0.0.1:8084/api/v1/internal/orders/{id}";
    private static final String CANCELAR = "http://127.0.0.1:8084/api/v1/internal/orders/{id}/cancel";
    private RestTemplate restTemplate;
    private OrdersClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new OrdersClient(restTemplate, "http://127.0.0.1:8084");
    }

    private static HttpClientErrorException error(HttpStatus status, String mensaje) {
        String body = "{\"status\":" + status.value() + ",\"mensaje\":\"" + mensaje + "\"}";
        return HttpClientErrorException.create(status, status.getReasonPhrase(), new HttpHeaders(),
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    @Test
    void consultaElPedidoConLaRutaInternaNueva() {
        PedidoOrders p = new PedidoOrders(15L, "PED-000015", 10L, "ENTREGADO", null, null,
                List.of(new OrdersDtos.ItemPedido(2L, "Juego", 2)));
        when(restTemplate.getForObject(PEDIDO, PedidoOrders.class, 15L)).thenReturn(p);
        PedidoOrders r = client.obtener(15L);
        assertEquals(10L, r.userId());
        assertEquals(2, r.cantidadComprada(2L));
        assertEquals(0, r.cantidadComprada(9L));
    }

    @Test
    void pedidoInexistenteEs404YNoSeReintenta() {
        when(restTemplate.getForObject(PEDIDO, PedidoOrders.class, 15L)).thenThrow(error(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        assertThrows(RecursoNoEncontradoException.class, () -> client.obtener(15L));
        verify(restTemplate, times(1)).getForObject(PEDIDO, PedidoOrders.class, 15L);
    }

    @Test
    void ordersCaidoSeReintentaUnNumeroLimitadoYDa503() {
        when(restTemplate.getForObject(PEDIDO, PedidoOrders.class, 15L))
                .thenThrow(new ResourceAccessException("Connection refused"));
        assertThrows(ServicioNoDisponibleException.class, () -> client.obtener(15L));
        verify(restTemplate, times(OrdersClient.MAX_INTENTOS)).getForObject(PEDIDO, PedidoOrders.class, 15L);
    }

    @Test
    void falloPasajeroSeRecuperaConElReintento() {
        PedidoOrders p = new PedidoOrders(15L, null, 10L, "CONFIRMADO", null, null, null);
        when(restTemplate.getForObject(PEDIDO, PedidoOrders.class, 15L))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY)).thenReturn(p);
        assertEquals("#15", client.obtener(15L).referencia());
        assertEquals(0, client.obtener(15L).lineas().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void cancelarEnviaLaSolicitudYElMotivo() {
        when(restTemplate.postForObject(eq(CANCELAR), any(), eq(CancelacionOrders.class), eq(15L)))
                .thenReturn(new CancelacionOrders(15L, "PED-000015", "CANCELADO", false, false));
        CancelacionOrders r = client.cancelar(15L, 7L, "Aprobada");
        assertEquals("CANCELADO", r.estado());
        ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
        verify(restTemplate).postForObject(eq(CANCELAR), cuerpo.capture(), eq(CancelacionOrders.class), eq(15L));
        Map<String, Object> enviado = (Map<String, Object>) cuerpo.getValue();
        assertEquals(7L, enviado.get("solicitudId"));
        assertEquals("Aprobada", enviado.get("motivo"));
    }

    @Test
    void cancelarUnPedidoDespachadoEs409ConElMensajeDeOrders() {
        when(restTemplate.postForObject(eq(CANCELAR), any(), eq(CancelacionOrders.class), anyLong()))
                .thenThrow(error(HttpStatus.CONFLICT, "Un pedido DESPACHADO no se puede cancelar"));
        OperacionNoPermitidaException e = assertThrows(OperacionNoPermitidaException.class,
                () -> client.cancelar(15L, 7L, "x"));
        assertEquals("Un pedido DESPACHADO no se puede cancelar", e.getMessage());
        verify(restTemplate, times(1)).postForObject(eq(CANCELAR), any(), eq(CancelacionOrders.class), anyLong());
    }

    @Test
    void otrosRechazosDeOrdersSon400() {
        when(restTemplate.postForObject(eq(CANCELAR), any(), eq(CancelacionOrders.class), anyLong()))
                .thenThrow(error(HttpStatus.BAD_REQUEST, "El motivo es obligatorio"));
        assertThrows(ValidacionNegocioException.class, () -> client.cancelar(15L, 7L, "x"));
    }
}
