package ms_ordenes.ms_ordenes.client;

import ms_ordenes.ms_ordenes.client.AccountsClient.EstadoOperacion;
import ms_ordenes.ms_ordenes.exception.AccesoDenegadoException;
import ms_ordenes.ms_ordenes.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lote 5 (HU-78, RN-37, RN-44): Orders -> Accounts. RestTemplate simulado: no se levanta Accounts. */
class AccountsClientTest {

    private static final String URL = "http://127.0.0.1:8081/api/v1/internal/accounts/{userId}/operation-status";
    private RestTemplate restTemplate;
    private AccountsClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new AccountsClient(restTemplate, "http://127.0.0.1:8081");
    }

    private void responde(String estado, Boolean puedeOperar) {
        when(restTemplate.getForObject(URL, EstadoOperacion.class, 10L)).thenReturn(new EstadoOperacion(10L, estado, puedeOperar));
    }

    @Test
    void cuentaActivaPuedeOperar() {
        responde("ACTIVA", true);
        assertDoesNotThrow(() -> client.exigirCuentaActiva(10L));
    }

    @Test
    void enEliminacionEliminadaONoReconocidaSeRechazan403() {
        responde("EN_ELIMINACION", false);
        assertThrows(AccesoDenegadoException.class, () -> client.exigirCuentaActiva(10L));
        responde("ELIMINADA", false);
        assertThrows(AccesoDenegadoException.class, () -> client.exigirCuentaActiva(10L));
        responde(null, false);
        assertThrows(AccesoDenegadoException.class, () -> client.exigirCuentaActiva(10L));
    }

    @Test
    void cuentaInexistenteSeRechaza403() {
        when(restTemplate.getForObject(URL, EstadoOperacion.class, 10L))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        assertThrows(AccesoDenegadoException.class, () -> client.exigirCuentaActiva(10L));
    }

    @Test
    void sinRespuestaSeReintentaYDa503() {
        when(restTemplate.getForObject(URL, EstadoOperacion.class, 10L))
                .thenThrow(new ResourceAccessException("Connection refused"));
        assertThrows(ServicioNoDisponibleException.class, () -> client.exigirCuentaActiva(10L));
        verify(restTemplate, times(AccountsClient.MAX_INTENTOS)).getForObject(URL, EstadoOperacion.class, 10L);
    }

    @Test
    void respuestaSinElDatoNoSeAsumeActiva() {
        responde("ACTIVA", null);
        assertThrows(ServicioNoDisponibleException.class, () -> client.exigirCuentaActiva(10L));
    }

    @Test
    void falloPasajeroSeRecupera() {
        when(restTemplate.getForObject(URL, EstadoOperacion.class, 10L))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
                .thenReturn(new EstadoOperacion(10L, "ACTIVA", true));
        assertDoesNotThrow(() -> client.exigirCuentaActiva(10L));
    }
}
