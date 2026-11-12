package com.ventasdiscos.msaccounts.client;

import com.ventasdiscos.msaccounts.client.DependenciasCuentaClient.DependenciaNoConfirmadaException;
import com.ventasdiscos.msaccounts.client.DependenciasCuentaClient.DepuracionResponse;
import com.ventasdiscos.msaccounts.client.DependenciasCuentaClient.ExisteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lote 5 (HU-78): Accounts -> rutas internas de los otros MS. RestTemplate simulado: no se levanta ningún MS. */
class DependenciasCuentaClientTest {

    private static final String PEDIDOS = "http://o/api/v1/internal/orders/users/{userId}/active-exists";
    private static final String TICKETS = "http://s/api/v1/internal/support/users/{userId}/open-exists";
    private static final String POSTVENTA = "http://p/api/v1/internal/post-sale/users/{userId}/pending-exists";
    private static final String FAVORITOS = "http://c/api/v1/internal/catalog/users/{userId}/favorites/purge";
    private static final String NOTIFICACIONES = "http://n/api/v1/internal/notifications/users/{userId}/purge";

    private RestTemplate restTemplate;
    private DependenciasCuentaClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new DependenciasCuentaClient(restTemplate, "http://o", "http://s", "http://p", "http://c", "http://n");
    }

    @Test
    void lasTresConsultasUsanLasRutasDelErs() {
        when(restTemplate.getForObject(PEDIDOS, ExisteResponse.class, 10L)).thenReturn(new ExisteResponse(10L, true));
        when(restTemplate.getForObject(TICKETS, ExisteResponse.class, 10L)).thenReturn(new ExisteResponse(10L, false));
        when(restTemplate.getForObject(POSTVENTA, ExisteResponse.class, 10L)).thenReturn(new ExisteResponse(10L, false));
        assertTrue(client.tienePedidosActivos(10L));
        assertFalse(client.tieneTicketsAbiertos(10L));
        assertFalse(client.tieneSolicitudesPendientes(10L));
    }

    @Test
    void sinRespuestaSeReintentaLimitadoYNoSeAsumeLibre() {
        when(restTemplate.getForObject(PEDIDOS, ExisteResponse.class, 10L)).thenThrow(new ResourceAccessException("Connection refused"));
        DependenciaNoConfirmadaException e = assertThrows(DependenciaNoConfirmadaException.class,
                () -> client.tienePedidosActivos(10L));
        assertEquals("Orders", e.getServicio());
        verify(restTemplate, times(DependenciasCuentaClient.MAX_INTENTOS)).getForObject(PEDIDOS, ExisteResponse.class, 10L);
    }

    @Test
    void un4xxOUnaRespuestaIncompletaTampocoSeInterpretaComoSinPendientes() {
        when(restTemplate.getForObject(TICKETS, ExisteResponse.class, 10L)).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        assertThrows(DependenciaNoConfirmadaException.class, () -> client.tieneTicketsAbiertos(10L));
        verify(restTemplate, times(1)).getForObject(TICKETS, ExisteResponse.class, 10L);
        when(restTemplate.getForObject(POSTVENTA, ExisteResponse.class, 10L)).thenReturn(new ExisteResponse(10L, null));
        assertThrows(DependenciaNoConfirmadaException.class, () -> client.tieneSolicitudesPendientes(10L));
    }

    @Test
    void lasDepuracionesSonIdempotentesYSeReintentanSiFallanALaPrimera() {
        when(restTemplate.postForObject(FAVORITOS, null, DepuracionResponse.class, 10L))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
                .thenReturn(new DepuracionResponse(10L, 3));
        assertEquals(3, client.depurarFavoritos(10L));
        when(restTemplate.postForObject(NOTIFICACIONES, null, DepuracionResponse.class, 10L))
                .thenReturn(new DepuracionResponse(10L, 0));
        assertEquals(0, client.depurarNotificaciones(10L));      // repetir: 0, sin efectos duplicados
    }

    @Test
    void depuracionSinConfirmarLanzaExcepcion() {
        when(restTemplate.postForObject(NOTIFICACIONES, null, DepuracionResponse.class, 10L))
                .thenThrow(new ResourceAccessException("Connection refused"));
        DependenciaNoConfirmadaException e = assertThrows(DependenciaNoConfirmadaException.class,
                () -> client.depurarNotificaciones(10L));
        assertEquals("Notifications", e.getServicio());
    }
}
