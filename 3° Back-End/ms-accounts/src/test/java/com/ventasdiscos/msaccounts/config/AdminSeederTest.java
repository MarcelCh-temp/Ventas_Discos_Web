package com.ventasdiscos.msaccounts.config;

import com.ventasdiscos.msaccounts.model.Role;
import com.ventasdiscos.msaccounts.model.Usuario;
import com.ventasdiscos.msaccounts.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El Administrador inicial solo se crea con variables de entorno válidas y una sola vez. */
class AdminSeederTest {

    private UsuarioRepository repo;
    private AdminSeeder seeder;

    @BeforeEach
    void setUp() {
        repo = mock(UsuarioRepository.class);
        seeder = new AdminSeeder(repo, new BCryptPasswordEncoder(4));
    }

    private void configurar(String rut, String correo, String password) {
        ReflectionTestUtils.setField(seeder, "rut", rut);
        ReflectionTestUtils.setField(seeder, "correo", correo);
        ReflectionTestUtils.setField(seeder, "password", password);
    }

    @Test
    void sinVariablesNoHaceNada() {
        configurar("", "", "");
        seeder.run();
        configurar("11111111-1", "", "x");
        seeder.run();
        configurar("11111111-1", "admin@tienda.cl", "");
        seeder.run();
        verify(repo, never()).save(any());
    }

    @Test
    void siYaExisteNoLoDuplica() {
        configurar("11111111-1", "admin@tienda.cl", "Admin12345");
        when(repo.existsByCorreo("admin@tienda.cl")).thenReturn(true);
        seeder.run();
        verify(repo, never()).save(any());
    }

    @Test
    void rutInvalidoNoCreaAdmin() {
        configurar("11111111-2", "admin@tienda.cl", "Admin12345");
        seeder.run();
        verify(repo, never()).save(any());
    }

    @Test
    void creaAdministradorConRolCorrecto() {
        configurar("11.111.111-1", "Admin@Tienda.cl", "Admin12345");
        seeder.run();
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        assertEquals(Role.ADMINISTRADOR, captor.getValue().getRol());
        assertEquals("admin@tienda.cl", captor.getValue().getCorreo());
        assertEquals("11111111-1", captor.getValue().getRut());
    }

    @Test
    void claveCortaNoCreaAdmin() {
        configurar("11111111-1", "admin@tienda.cl", "1234");
        seeder.run();
        verify(repo, never()).save(any());
    }
}
