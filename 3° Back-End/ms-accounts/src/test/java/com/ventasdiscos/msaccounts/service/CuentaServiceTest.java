package com.ventasdiscos.msaccounts.service;

import com.ventasdiscos.msaccounts.dto.ActualizarPerfilRequest;
import com.ventasdiscos.msaccounts.dto.CambioPasswordRequest;
import com.ventasdiscos.msaccounts.dto.LoginRequest;
import com.ventasdiscos.msaccounts.dto.LoginResponse;
import com.ventasdiscos.msaccounts.dto.PerfilResponse;
import com.ventasdiscos.msaccounts.dto.RecuperarCorreoRequest;
import com.ventasdiscos.msaccounts.dto.RecuperarPasswordRequest;
import com.ventasdiscos.msaccounts.dto.RegistroRequest;
import com.ventasdiscos.msaccounts.exception.NoAutenticadoException;
import com.ventasdiscos.msaccounts.exception.RecursoDuplicadoException;
import com.ventasdiscos.msaccounts.exception.RecursoNoEncontradoException;
import com.ventasdiscos.msaccounts.exception.ValidacionNegocioException;
import com.ventasdiscos.msaccounts.model.Role;
import com.ventasdiscos.msaccounts.model.Usuario;
import com.ventasdiscos.msaccounts.repository.UsuarioRepository;
import com.ventasdiscos.msaccounts.security.AuthUser;
import com.ventasdiscos.msaccounts.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de reglas de negocio de EP-01 (Repository simulado con Mockito). */
class CuentaServiceTest {

    private UsuarioRepository repo;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // costo bajo: tests rápidos
    private final JwtService jwt = new JwtService("test-secret-de-al-menos-32-caracteres-xx", 60);
    private CuentaService service;

    @BeforeEach
    void setUp() {
        repo = mock(UsuarioRepository.class);
        service = new CuentaService(repo, encoder, jwt);
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(1L);
            }
            return u;
        });
    }

    private RegistroRequest registroValido() {
        return new RegistroRequest("12.345.678-5", "Ana", "Pérez", "Ana@Mail.com", "ana@mail.com",
                "Secreta123", "Secreta123");
    }

    private Usuario usuarioExistente() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setRut("12345678-5");
        u.setNombres("Ana");
        u.setApellidos("Pérez");
        u.setCorreo("ana@mail.com");
        u.setPasswordHash(encoder.encode("Secreta123"));
        u.setRol(Role.CLIENTE);
        return u;
    }

    // ---------- HU-01 ----------

    @Test
    void registroCreaSiempreClienteConPasswordHasheada() {
        PerfilResponse resp = service.registrar(registroValido());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals(Role.CLIENTE, guardado.getRol());
        assertEquals("ana@mail.com", guardado.getCorreo());
        assertEquals("12345678-5", guardado.getRut());
        assertNotEquals("Secreta123", guardado.getPasswordHash());
        assertEquals(Role.CLIENTE, resp.rol());
    }

    @Test
    void registroRechazaCorreoQueNoCoincide() {
        RegistroRequest req = new RegistroRequest("12345678-5", "Ana", "Pérez", "ana@mail.com", "otra@mail.com",
                "Secreta123", "Secreta123");
        assertThrows(ValidacionNegocioException.class, () -> service.registrar(req));
        verify(repo, never()).save(any());
    }

    @Test
    void registroRechazaPasswordQueNoCoincide() {
        RegistroRequest req = new RegistroRequest("12345678-5", "Ana", "Pérez", "ana@mail.com", "ana@mail.com",
                "Secreta123", "Distinta123");
        assertThrows(ValidacionNegocioException.class, () -> service.registrar(req));
    }

    @Test
    void registroRechazaRutConDvIncorrecto() {
        RegistroRequest req = new RegistroRequest("12345678-9", "Ana", "Pérez", "ana@mail.com", "ana@mail.com",
                "Secreta123", "Secreta123");
        assertThrows(ValidacionNegocioException.class, () -> service.registrar(req));
    }

    @Test
    void registroRechazaRutDuplicado() {
        when(repo.existsByRut("12345678-5")).thenReturn(true);
        assertThrows(RecursoDuplicadoException.class, () -> service.registrar(registroValido()));
    }

    @Test
    void registroRechazaCorreoDuplicado() {
        when(repo.existsByCorreo("ana@mail.com")).thenReturn(true);
        assertThrows(RecursoDuplicadoException.class, () -> service.registrar(registroValido()));
    }

    // ---------- HU-02 ----------

    @Test
    void loginCorrectoEntregaTokenConRolDeLaCuenta() {
        Usuario u = usuarioExistente();
        u.setRol(Role.ADMINISTRADOR);
        when(repo.findByCorreo("ana@mail.com")).thenReturn(Optional.of(u));

        LoginResponse resp = service.login(new LoginRequest("ANA@mail.com", "Secreta123"));

        assertEquals(Role.ADMINISTRADOR, resp.rol());
        AuthUser desdeToken = jwt.validar(resp.token());
        assertEquals(1L, desdeToken.userId());
        assertEquals(Role.ADMINISTRADOR, desdeToken.rol());
    }

    @Test
    void loginConPasswordIncorrectaDa401() {
        when(repo.findByCorreo("ana@mail.com")).thenReturn(Optional.of(usuarioExistente()));
        assertThrows(NoAutenticadoException.class, () -> service.login(new LoginRequest("ana@mail.com", "mala")));
    }

    @Test
    void loginConCuentaInexistenteDa401() {
        when(repo.findByCorreo("nadie@mail.com")).thenReturn(Optional.empty());
        assertThrows(NoAutenticadoException.class, () -> service.login(new LoginRequest("nadie@mail.com", "x")));
    }

    // ---------- HU-05 ----------

    @Test
    void edicionRechazaCorreoDeOtraCuenta() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuarioExistente()));
        when(repo.existsByCorreoAndIdNot("pedro@mail.com", 1L)).thenReturn(true);
        assertThrows(RecursoDuplicadoException.class,
                () -> service.actualizarPerfil(1L, new ActualizarPerfilRequest(null, null, "pedro@mail.com")));
    }

    @Test
    void edicionSinCamposDa400() {
        assertThrows(ValidacionNegocioException.class,
                () -> service.actualizarPerfil(1L, new ActualizarPerfilRequest(null, null, null)));
    }

    @Test
    void edicionActualizaSoloCamposEnviados() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuarioExistente()));
        PerfilResponse resp = service.actualizarPerfil(1L, new ActualizarPerfilRequest("Ana María", null, null));
        assertEquals("Ana María", resp.nombres());
        assertEquals("Pérez", resp.apellidos());
    }

    // ---------- HU-06 ----------

    @Test
    void cambioPasswordRechazaActualIncorrecta() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuarioExistente()));
        assertThrows(ValidacionNegocioException.class,
                () -> service.cambiarPassword(1L, new CambioPasswordRequest("mala", "Nueva12345", "Nueva12345")));
        verify(repo, never()).save(any());
    }

    @Test
    void cambioPasswordCorrecto() {
        Usuario u = usuarioExistente();
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        service.cambiarPassword(1L, new CambioPasswordRequest("Secreta123", "Nueva12345", "Nueva12345"));
        assertEquals(true, encoder.matches("Nueva12345", u.getPasswordHash()));
    }

    // ---------- HU-07 ----------

    @Test
    void recuperarPasswordExigeQueCorreoYRutSeanDeLaMismaCuenta() {
        when(repo.findByCorreo("ana@mail.com")).thenReturn(Optional.of(usuarioExistente()));
        RecuperarPasswordRequest req = new RecuperarPasswordRequest("ana@mail.com", "11111111-1",
                "Nueva12345", "Nueva12345");
        assertThrows(RecursoNoEncontradoException.class, () -> service.recuperarPassword(req));
    }

    @Test
    void recuperarCorreoRechazaCorreoEnUso() {
        when(repo.findByRut("12345678-5")).thenReturn(Optional.of(usuarioExistente()));
        when(repo.existsByCorreoAndIdNot("pedro@mail.com", 1L)).thenReturn(true);
        assertThrows(RecursoDuplicadoException.class, () -> service.recuperarCorreo(
                new RecuperarCorreoRequest("12345678-5", "pedro@mail.com", "pedro@mail.com")));
    }

    @Test
    void recuperarCorreoActualiza() {
        Usuario u = usuarioExistente();
        when(repo.findByRut("12345678-5")).thenReturn(Optional.of(u));
        service.recuperarCorreo(new RecuperarCorreoRequest("12.345.678-5", "nueva@mail.com", "NUEVA@mail.com"));
        assertEquals("nueva@mail.com", u.getCorreo());
    }

    // ---------- HU-04 ----------

    @Test
    void perfilPropio() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuarioExistente()));
        assertEquals("ana@mail.com", service.obtenerPerfil(1L).correo());
    }

    @Test
    void perfilDeCuentaInexistenteDa404() {
        when(repo.findById(5L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPerfil(5L));
    }

    @Test
    void edicionCambiaApellidosYCorreoLibre() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuarioExistente()));
        PerfilResponse resp = service.actualizarPerfil(1L,
                new ActualizarPerfilRequest(null, " Soto ", "NUEVO@mail.com"));
        assertEquals("Soto", resp.apellidos());
        assertEquals("nuevo@mail.com", resp.correo());
    }

    @Test
    void cambioPasswordRechazaConfirmacionDistinta() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuarioExistente()));
        assertThrows(ValidacionNegocioException.class,
                () -> service.cambiarPassword(1L, new CambioPasswordRequest("Secreta123", "Nueva12345", "Otra12345")));
    }

    @Test
    void recuperarPasswordCorrecto() {
        Usuario u = usuarioExistente();
        when(repo.findByCorreo("ana@mail.com")).thenReturn(Optional.of(u));
        service.recuperarPassword(new RecuperarPasswordRequest("ana@mail.com", "12.345.678-5", "Nueva12345", "Nueva12345"));
        assertEquals(true, encoder.matches("Nueva12345", u.getPasswordHash()));
    }

    @Test
    void recuperarPasswordConRutInvalidoOConfirmacionDistinta() {
        assertThrows(ValidacionNegocioException.class, () -> service.recuperarPassword(
                new RecuperarPasswordRequest("ana@mail.com", "12345678-9", "Nueva12345", "Nueva12345")));
        when(repo.findByCorreo("ana@mail.com")).thenReturn(Optional.of(usuarioExistente()));
        assertThrows(ValidacionNegocioException.class, () -> service.recuperarPassword(
                new RecuperarPasswordRequest("ana@mail.com", "12345678-5", "Nueva12345", "Otra12345")));
    }

    @Test
    void recuperarCorreoConRutNoRegistradoDa404() {
        when(repo.findByRut("11111111-1")).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.recuperarCorreo(
                new RecuperarCorreoRequest("11111111-1", "x@mail.com", "x@mail.com")));
    }

    @Test
    void recuperarCorreoConConfirmacionDistintaDa400() {
        when(repo.findByRut("12345678-5")).thenReturn(Optional.of(usuarioExistente()));
        assertThrows(ValidacionNegocioException.class, () -> service.recuperarCorreo(
                new RecuperarCorreoRequest("12345678-5", "x@mail.com", "y@mail.com")));
    }

    // ---------- HU-45 ----------

    @Test
    void estadisticasCuentaUsuarios() {
        when(repo.count()).thenReturn(10L);
        when(repo.countByRol(Role.CLIENTE)).thenReturn(9L);
        assertEquals(10L, service.estadisticas().totalUsuarios());
        assertEquals(9L, service.estadisticas().totalClientes());
    }
}
