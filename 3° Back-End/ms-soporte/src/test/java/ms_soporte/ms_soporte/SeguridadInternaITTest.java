package ms_soporte.ms_soporte;

import ms_soporte.ms_soporte.security.Servicios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lote 6 · DOC-05 y DOC-06 en ms-soporte: la ruta interna /internal/support/users/999999/open-exists exige JWT de servicio del emisor autorizado,
 * rechaza el JWT de usuario y un JWT de servicio no sirve en rutas de clientes. Los errores llevan correlationId.
 * (Escrita en el Lote 6; sin ejecutar.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadInternaITTest {

    private static final String INTERNA = "/api/v1/internal/support/users/999999/open-exists";

    @Autowired
    private MockMvc mockMvc;

    private MockHttpServletRequestBuilder interna() {
        return get(INTERNA);
    }

    @Test
    void sinToken_401ConApiErrorYCorrelationId() throws Exception {
        mockMvc.perform(interna().header("X-Correlation-Id", "prueba-12345678"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value(INTERNA))
                .andExpect(jsonPath("$.correlationId").value("prueba-12345678"))
                .andExpect(header().string("X-Correlation-Id", "prueba-12345678"));
        mockMvc.perform(interna().header("Authorization", "Basic abc")).andExpect(status().isUnauthorized());
    }

    @Test
    void jwtDeUsuarioEnRutaInterna_401() throws Exception {
        mockMvc.perform(interna().header("Authorization", TokensServicioPrueba.usuarioAdmin()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void servicioNoAutorizadoParaLaRuta_403() throws Exception {
        mockMvc.perform(interna().header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void tokenParaOtroServicioOFirmadoConOtroSecreto_401() throws Exception {
        String otraAudiencia = Servicios.PROPIO.equals(Servicios.CATALOG) ? Servicios.ORDERS : Servicios.CATALOG;
        mockMvc.perform(interna().header("Authorization", "Bearer " + TokensServicioPrueba.token(
                        Servicios.ACCOUNTS, otraAudiencia, 300, TokensServicioPrueba.SECRETO)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(interna().header("Authorization", "Bearer " + TokensServicioPrueba.token(
                        Servicios.ACCOUNTS, Servicios.PROPIO, 300, "otro-secreto-de-servicio-de-32-caracteres")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(interna().header("Authorization", "Bearer " + TokensServicioPrueba.token(
                        Servicios.ACCOUNTS, Servicios.PROPIO, -10, TokensServicioPrueba.SECRETO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void servicioAutorizadoPasa() throws Exception {
        mockMvc.perform(interna().header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)))
                .andExpect(status().isOk());
    }

    @Test
    void jwtDeServicioEnRutaDeCliente_401() throws Exception {
        mockMvc.perform(get("/api/v1/support/requests/me").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }
}
