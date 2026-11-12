package ms_soporte.ms_soporte.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lote 1.1 (ERS 12.3 y 12.7, RN-33): reintentos controlados del aviso a MS-Notifications.
 * RestTemplate simulado: no se levanta Notifications.
 */
class NotificacionesClientTest {

    private RestTemplate restTemplate;
    private NotificacionesClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new NotificacionesClient(restTemplate, "http://127.0.0.1:8086");
    }

    private void enviar() {
        client.notificar("SOPORTE-12-CREADO", 10L, "SOPORTE_CREADO", "Solicitud creada", "Tu solicitud fue registrada.", 12L);
    }

    @Test
    void entregaAlPrimerIntento_UnaSolaLlamada() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        enviar();

        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Void.class));
    }

    // Falla la conexión una vez -> se reintenta con el MISMO eventoId (Notifications no duplica) y se entrega
    @Test
    @SuppressWarnings("unchecked")
    void falloDeConexion_SeReintentaConElMismoEventoId() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection refused"))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        enviar();

        ArgumentCaptor<Object> cuerpos = ArgumentCaptor.forClass(Object.class);
        verify(restTemplate, times(2)).postForEntity(anyString(), cuerpos.capture(), eq(Void.class));
        List<Object> enviados = cuerpos.getAllValues();
        assertEquals("SOPORTE-12-CREADO", ((Map<String, Object>) enviados.get(0)).get("eventoId"));
        assertEquals("SOPORTE-12-CREADO", ((Map<String, Object>) enviados.get(1)).get("eventoId"));
    }

    // Notifications sigue caído: número LIMITADO de intentos y no se lanza error (la solicitud ya está guardada)
    @Test
    void siempreFalla_IntentosLimitadosYSinError() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        assertDoesNotThrow(this::enviar);

        verify(restTemplate, times(NotificacionesClient.MAX_INTENTOS)).postForEntity(anyString(), any(), eq(Void.class));
    }

    // 4xx: los datos fueron rechazados; repetir no sirve -> no se reintenta (ERS 12.7 "no reintentar ciegamente")
    @Test
    void datosRechazados_NoSeReintenta() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertDoesNotThrow(this::enviar);

        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Void.class));
    }
}
