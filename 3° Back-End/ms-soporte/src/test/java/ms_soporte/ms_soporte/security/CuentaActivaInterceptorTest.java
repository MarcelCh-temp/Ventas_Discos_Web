package ms_soporte.ms_soporte.security;

import ms_soporte.ms_soporte.client.AccountsClient;
import ms_soporte.ms_soporte.client.AccountsClient.EstadoOperacion;
import ms_soporte.ms_soporte.exception.AccesoDenegadoException;
import ms_soporte.ms_soporte.exception.NoAutenticadoException;
import ms_soporte.ms_soporte.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Cierre · RN-44 y RF-CTA-08 CA-06: con el AccountsClient REAL (RestTemplate simulado), el interceptor deja pasar
 * solo cuentas ACTIVAS y bloquea lecturas y escrituras de cuentas EN_ELIMINACION, ELIMINADA o desconocidas.
 * (Escrita en el cierre; sin ejecutar.)
 */
class CuentaActivaInterceptorTest {

    private static final String URL = "http://accounts/api/v1/internal/accounts/{userId}/operation-status";

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final AccountsClient accounts = new AccountsClient(restTemplate, "http://accounts");
    private final JwtService jwt = mock(JwtService.class);
    private final CuentaActivaInterceptor interceptor = new CuentaActivaInterceptor(proveedor(jwt), proveedor(accounts));

    /** Controller de prueba: un endpoint del cliente (recibe AuthUser) y uno público. */
    static class Rutas {
        public void delCliente(AuthUser usuario) {
        }

        public void publica() {
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> proveedor(T valor) {
        ObjectProvider<T> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(valor);
        return p;
    }

    private static HandlerMethod delCliente() throws Exception {
        return new HandlerMethod(new Rutas(), Rutas.class.getMethod("delCliente", AuthUser.class));
    }

    // Comprobación de seguridad o identidad necesaria para permitir la operación. Método publica (publica).
    private static HandlerMethod publica() throws Exception {
        return new HandlerMethod(new Rutas(), Rutas.class.getMethod("publica"));
    }

    // Comprobación de seguridad o identidad necesaria para permitir la operación. Método peticion (peticion).
    private static MockHttpServletRequest peticion(String metodo, String token) {
        MockHttpServletRequest r = new MockHttpServletRequest(metodo, "/api/v1/recurso");
        if (token != null) {
            r.addHeader("Authorization", "Bearer " + token);
        }
        return r;
    }

    // Comprobación de seguridad o identidad necesaria para permitir la operación. Método pasar (pasar).
    private boolean pasar(MockHttpServletRequest r, HandlerMethod h) throws Exception {
        return interceptor.preHandle(r, new MockHttpServletResponse(), h);
    }

    // Comprobación de seguridad o identidad necesaria para permitir la operación. Método estado (estado).
    private void estado(String estado, Boolean puedeOperar) {
        when(restTemplate.getForObject(URL, EstadoOperacion.class, 7L)).thenReturn(new EstadoOperacion(7L, estado, puedeOperar));
    }

    @BeforeEach
    void tokens() {
        when(jwt.validar("cliente")).thenReturn(new AuthUser(7L, "CLIENTE"));
        when(jwt.validar("admin")).thenReturn(new AuthUser(1L, "ADMINISTRADOR"));
        when(jwt.validar("malo")).thenThrow(new NoAutenticadoException("Token inválido"));
    }

    @AfterEach
    void limpiar() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void clienteActivaContinuaEnLecturaYEscritura() throws Exception {
        estado("ACTIVA", true);
        for (String metodo : new String[]{"GET", "POST", "PUT", "PATCH", "DELETE"}) {
            assertTrue(pasar(peticion(metodo, "cliente"), delCliente()));
        }
    }

    @Test
    void clienteEnEliminacion_403EnLecturaYEscritura() throws Exception {
        estado("EN_ELIMINACION", false);
        assertThrows(AccesoDenegadoException.class, () -> pasar(peticion("GET", "cliente"), delCliente()));
        assertThrows(AccesoDenegadoException.class, () -> pasar(peticion("POST", "cliente"), delCliente()));
    }

    @Test
    void clienteEliminada_403EnLecturaYEscritura() throws Exception {
        estado("ELIMINADA", false);
        assertThrows(AccesoDenegadoException.class, () -> pasar(peticion("GET", "cliente"), delCliente()));
        assertThrows(AccesoDenegadoException.class, () -> pasar(peticion("DELETE", "cliente"), delCliente()));
    }

    @Test
    void estadoDesconocidoOCuentaInexistente_403() throws Exception {
        estado(null, false);
        assertThrows(AccesoDenegadoException.class, () -> pasar(peticion("GET", "cliente"), delCliente()));
        when(restTemplate.getForObject(URL, EstadoOperacion.class, 7L)).thenThrow(HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8));
        assertThrows(AccesoDenegadoException.class, () -> pasar(peticion("GET", "cliente"), delCliente()));
    }

    @Test
    void accountsNoDisponibleOSinRespuestaClara_503NuncaActiva() throws Exception {
        when(restTemplate.getForObject(URL, EstadoOperacion.class, 7L)).thenThrow(new ResourceAccessException("Connection refused"));
        assertThrows(ServicioNoDisponibleException.class, () -> pasar(peticion("GET", "cliente"), delCliente()));
        org.mockito.Mockito.doReturn(new EstadoOperacion(7L, "ACTIVA", null)).when(restTemplate).getForObject(URL, EstadoOperacion.class, 7L);
        assertThrows(ServicioNoDisponibleException.class, () -> pasar(peticion("GET", "cliente"), delCliente()));
        CuentaActivaInterceptor sinCliente = new CuentaActivaInterceptor(proveedor(jwt), proveedor(null));
        assertThrows(ServicioNoDisponibleException.class, () -> sinCliente.preHandle(peticion("GET", "cliente"),
                new MockHttpServletResponse(), delCliente()));
    }

    @Test
    void endpointPublicoAdministradorYTokenAusenteOInvalidoNoConsultanAccounts() throws Exception {
        assertTrue(pasar(peticion("GET", null), publica()));
        assertTrue(pasar(peticion("GET", "cliente"), publica()));
        assertTrue(pasar(peticion("GET", "admin"), delCliente()));
        assertTrue(pasar(peticion("PATCH", "admin"), delCliente()));
        assertTrue(pasar(peticion("GET", null), delCliente()));   // el resolver de AuthUser responde 401
        assertTrue(pasar(peticion("GET", "malo"), delCliente()));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void llamadaInternaConJwtDeServicioNoSeRevisa() throws Exception {
        MockHttpServletRequest conServicio = peticion("POST", "cliente");
        conServicio.setAttribute(ServicioInternoInterceptor.ATRIBUTO_EMISOR, Servicios.ACCOUNTS);
        assertTrue(pasar(conServicio, delCliente()));
        MockHttpServletRequest interna = new MockHttpServletRequest("GET", "/api/v1/internal/cualquier/ruta");
        interna.addHeader("Authorization", "Bearer cliente");
        assertTrue(pasar(interna, delCliente()));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void unaSolaConsultaAAccountsPorPeticion() throws Exception {
        estado("ACTIVA", true);
        MockHttpServletRequest r = peticion("POST", "cliente");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(r));
        assertTrue(pasar(r, delCliente()));
        accounts.exigirCuentaActiva(7L);   // la comprobación previa a guardar (Lote 5) no repite la llamada
        verify(restTemplate, times(1)).getForObject(URL, EstadoOperacion.class, 7L);
    }
}
