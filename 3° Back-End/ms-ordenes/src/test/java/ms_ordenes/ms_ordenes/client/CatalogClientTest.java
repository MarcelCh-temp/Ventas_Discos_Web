package ms_ordenes.ms_ordenes.client;

import ms_ordenes.ms_ordenes.client.CatalogoDtos.ItemSolicitado;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.RespuestaOperacion;
import ms_ordenes.ms_ordenes.client.CatalogoDtos.ResultadoOperacion;
import ms_ordenes.ms_ordenes.exception.RecursoNoEncontradoException;
import ms_ordenes.ms_ordenes.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lote 3: Orders -> Catalog. RestTemplate simulado: no se levanta Catalog. */
class CatalogClientTest {

    private static final String OPS = "http://127.0.0.1:8082/api/v1/internal/inventory/operations";
    private RestTemplate restTemplate;
    private CatalogClient client;
    private final List<ItemSolicitado> items = List.of(new ItemSolicitado(7L, 2));

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new CatalogClient(restTemplate, "http://127.0.0.1:8082");
    }

    private static HttpClientErrorException error(HttpStatus status, String mensaje) {
        String body = "{\"status\":" + status.value() + ",\"mensaje\":\"" + mensaje + "\"}";
        return HttpClientErrorException.create(status, status.getReasonPhrase(), new HttpHeaders(),
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    @Test
    @SuppressWarnings("unchecked")
    void operacionAplicadaEnviaElPrefijoYLaMismaOperacionId() {
        when(restTemplate.postForEntity(eq(OPS), any(), eq(String.class))).thenReturn(ResponseEntity.status(201).body("{}"));
        RespuestaOperacion r = client.aplicarOperacion("ORDERS-PEDIDO-5-VENTA", "Venta", "SALIDA", items);
        assertEquals(ResultadoOperacion.APLICADA, r.resultado());
        ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
        verify(restTemplate).postForEntity(eq(OPS), cuerpo.capture(), eq(String.class));
        Map<String, Object> enviado = (Map<String, Object>) cuerpo.getValue();
        assertEquals("ORDERS-PEDIDO-5-VENTA", enviado.get("operacionId"));
        assertEquals("SALIDA", ((List<Map<String, Object>>) enviado.get("items")).get(0).get("tipo"));
    }

    /** 200 = Catalog ya la tenía aplicada (idempotencia): también cuenta como aplicada, sin descontar dos veces. */
    @Test
    void operacionYaAplicadaCuentaComoAplicada() {
        when(restTemplate.postForEntity(eq(OPS), any(), eq(String.class))).thenReturn(ResponseEntity.ok("{\"yaAplicada\":true}"));
        assertEquals(ResultadoOperacion.APLICADA,
                client.aplicarOperacion("ORDERS-PEDIDO-5-VENTA", "Venta", "SALIDA", items).resultado());
    }

    /**
     * Revisión Lote 3: un 409 es CONFLICTO (puede ser falta de stock u operacionId repetida); el cliente NO decide
     * que falta stock: eso lo confirma OrdenService con la disponibilidad. Sin reintento.
     */
    @Test
    void respuesta409EsConflictoConMensajeYCodigoSinReintento() {
        when(restTemplate.postForEntity(eq(OPS), any(), eq(String.class)))
                .thenThrow(error(HttpStatus.CONFLICT, "Stock insuficiente para el producto 7"));
        RespuestaOperacion r = client.aplicarOperacion("ORDERS-PEDIDO-5-VENTA", "Venta", "SALIDA", items);
        assertEquals(ResultadoOperacion.CONFLICTO, r.resultado());
        assertEquals("Stock insuficiente para el producto 7", r.mensaje());
        assertEquals(409, r.status());
        verify(restTemplate, times(1)).postForEntity(eq(OPS), any(), eq(String.class));
    }

    /** 400 (datos inválidos, por ejemplo sin prefijo DOC-08) y 404: RECHAZADA, nunca "sin stock". */
    @Test
    void respuestas400y404SonRechazadas() {
        when(restTemplate.postForEntity(eq(OPS), any(), eq(String.class)))
                .thenThrow(error(HttpStatus.BAD_REQUEST, "operacionId no válido"))
                .thenThrow(error(HttpStatus.NOT_FOUND, "Productos no encontrados: [7]"));
        RespuestaOperacion r400 = client.aplicarOperacion("ORDERS-PEDIDO-5-VENTA", "Venta", "SALIDA", items);
        RespuestaOperacion r404 = client.aplicarOperacion("ORDERS-PEDIDO-5-VENTA", "Venta", "SALIDA", items);
        assertEquals(ResultadoOperacion.RECHAZADA, r400.resultado());
        assertEquals(400, r400.status());
        assertEquals(ResultadoOperacion.RECHAZADA, r404.resultado());
        assertEquals(404, r404.status());
    }

    @Test
    void caidaDeCatalogQuedaNoConfirmadaTrasIntentosLimitados() {
        when(restTemplate.postForEntity(eq(OPS), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));
        assertEquals(ResultadoOperacion.NO_CONFIRMADA,
                client.aplicarOperacion("ORDERS-PEDIDO-5-VENTA", "Venta", "SALIDA", items).resultado());
        verify(restTemplate, times(CatalogClient.MAX_INTENTOS)).postForEntity(eq(OPS), any(), eq(String.class));
    }

    @Test
    void consultaConProductoInexistenteDa404() {
        when(restTemplate.postForObject(anyString(), any(), eq(CatalogoDtos.Cotizacion.class)))
                .thenThrow(error(HttpStatus.NOT_FOUND, "Productos no encontrados: [7]"));
        RecursoNoEncontradoException e = assertThrows(RecursoNoEncontradoException.class, () -> client.cotizar(items));
        assertEquals("Productos no encontrados: [7]", e.getMessage());
    }

    @Test
    void consultaConCatalogCaidoDa503TrasReintentar() {
        when(restTemplate.postForObject(anyString(), any(), eq(CatalogoDtos.Cotizacion.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
        assertThrows(ServicioNoDisponibleException.class, () -> client.cotizar(items));
        verify(restTemplate, times(CatalogClient.MAX_INTENTOS))
                .postForObject(anyString(), any(), eq(CatalogoDtos.Cotizacion.class));
    }
}
