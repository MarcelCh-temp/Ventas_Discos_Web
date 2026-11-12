package com.ventasdiscos.msnotifications.controller;

import com.ventasdiscos.msnotifications.client.AccountsClient;
import com.ventasdiscos.msnotifications.exception.NoAutenticadoException;
import com.ventasdiscos.msnotifications.security.AuthUser;
import com.ventasdiscos.msnotifications.security.JwtService;
import com.ventasdiscos.msnotifications.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lote 1 (HU-54, HU-55): las notificaciones se consultan y marcan con el usuario del TOKEN.
 */
@WebMvcTest(controllers = NotificacionController.class)
class NotificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificacionService notificacionService;

    @MockitoBean
    private JwtService jwtService;

    // Cierre (RN-44): el interceptor de cuenta ACTIVA usa AccountsClient; simulado = ACTIVA
    @MockitoBean
    private AccountsClient accountsClient;

    @BeforeEach
    void setUp() {
        when(jwtService.validar("token-cliente-7")).thenReturn(new AuthUser(7L, "CLIENTE"));
        when(jwtService.validar("token-falso")).thenThrow(new NoAutenticadoException("Token inválido"));
    }

    @Test
    void sinToken_Devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401)) // Lote 1.1: formato ApiError
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void tokenInvalido_Devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listar_UsaElUsuarioDelToken_IgnoraUsuarioIdDeLaUrl() throws Exception {
        when(notificacionService.buscarNotificaciones(eq(7L), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications/me?usuarioId=99")
                        .header("Authorization", "Bearer token-cliente-7"))
                .andExpect(status().isOk());

        verify(notificacionService).buscarNotificaciones(eq(7L), any(), any(), any());
        verify(notificacionService, never()).buscarNotificaciones(eq(99L), any(), any(), any());
    }

    @Test
    void contarNoLeidas_DelUsuarioDelToken() throws Exception {
        when(notificacionService.contarNoLeidas(7L)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/me/unread-count")
                        .header("Authorization", "Bearer token-cliente-7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    void marcarLeida_DelUsuarioDelToken() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/5/read")
                        .header("Authorization", "Bearer token-cliente-7"))
                .andExpect(status().isNoContent());

        verify(notificacionService).marcarComoLeida(5L, 7L);
    }
}
