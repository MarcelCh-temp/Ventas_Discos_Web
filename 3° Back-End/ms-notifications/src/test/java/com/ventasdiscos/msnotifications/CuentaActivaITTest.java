package com.ventasdiscos.msnotifications;

import com.ventasdiscos.msnotifications.client.AccountsClient;
import com.ventasdiscos.msnotifications.exception.AccesoDenegadoException;
import com.ventasdiscos.msnotifications.exception.ServicioNoDisponibleException;
import com.ventasdiscos.msnotifications.security.Servicios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cierre · RN-44 y RF-CTA-08 CA-06 en ms-notifications: toda petición de un CLIENTE pasa por la comprobación de cuenta ACTIVA
 * (AccountsClient simulado). Lo que distingue EN_ELIMINACION, ELIMINADA o un estado desconocido lo prueba
 * CuentaActivaInterceptorTest con el cliente real. (Escrita en el cierre; sin ejecutar.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class CuentaActivaITTest {

    private static final long CLIENTE = 77L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountsClient cuentas;

    private static String cliente() {
        return TokensServicioPrueba.usuario(CLIENTE, "CLIENTE");
    }

    private MockHttpServletRequestBuilder escritura() {
        MockHttpServletRequestBuilder b = patch("/api/v1/notifications/read-all").header("Authorization", cliente());
        return b;
    }

    @Test
    void clienteActivaConsultaNormalmente() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", cliente())).andExpect(status().isOk());
        verify(cuentas, atLeastOnce()).exigirCuentaActiva(CLIENTE);
    }

    @Test
    void cuentaNoActiva_GetAutenticadoBloqueado403() throws Exception {
        doThrow(new AccesoDenegadoException("Tu cuenta no puede realizar nuevas operaciones")).when(cuentas)
                .exigirCuentaActiva(CLIENTE);
        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", cliente()).header("X-Correlation-Id", "cierre-12345678"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.correlationId").value("cierre-12345678"));
    }

    @Test
    void cuentaNoActiva_EscrituraAutenticadaBloqueada403() throws Exception {
        doThrow(new AccesoDenegadoException("Tu cuenta no puede realizar nuevas operaciones")).when(cuentas)
                .exigirCuentaActiva(CLIENTE);
        mockMvc.perform(escritura()).andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void accountsNoDisponible_503() throws Exception {
        doThrow(new ServicioNoDisponibleException("No fue posible confirmar el estado de tu cuenta")).when(cuentas)
                .exigirCuentaActiva(CLIENTE);
        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", cliente())).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
        mockMvc.perform(escritura()).andExpect(status().isServiceUnavailable());
    }

    @Test
    void comunicacionInternaConJwtDeServicioNoSeAfecta() throws Exception {
        doThrow(new AccesoDenegadoException("Tu cuenta no puede realizar nuevas operaciones")).when(cuentas)
                .exigirCuentaActiva(anyLong());
        mockMvc.perform(post("/api/v1/internal/notifications/users/77/purge").header("Authorization", TokensServicioPrueba.bearer(Servicios.ACCOUNTS)))
                .andExpect(status().isOk());
        verify(cuentas, never()).exigirCuentaActiva(anyLong());
    }

    @Test
    void administradorConservaSusPermisos() throws Exception {
        doThrow(new AccesoDenegadoException("Tu cuenta no puede realizar nuevas operaciones")).when(cuentas)
                .exigirCuentaActiva(anyLong());
        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", TokensServicioPrueba.usuarioAdmin()))
                .andExpect(status().isOk());
        verify(cuentas, never()).exigirCuentaActiva(anyLong());
    }
}
