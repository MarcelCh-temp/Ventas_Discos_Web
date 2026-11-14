package com.ventasdiscos.msaccounts;

import com.ventasdiscos.msaccounts.model.Role;
import com.ventasdiscos.msaccounts.model.Usuario;
import com.ventasdiscos.msaccounts.repository.UsuarioRepository;
import com.ventasdiscos.msaccounts.security.JwtService;
import com.ventasdiscos.msaccounts.security.Servicios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cierre del backend · decisión 3/4 (RF-PED-01, RF-PED-02, RF-SOP-05): GET /api/v1/admin/accounts?ids=...
 * Consulta agrupada de clientes para las vistas administrativas. (Escrita en el cierre; sin ejecutar.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminCuentaITTest {

    private static final String RUTA = "/api/v1/admin/accounts";
    private static final AtomicLong SECUENCIA = new AtomicLong(31_000_000);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JdbcTemplate jdbc;

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método rutValido (rut valido).
    private static String rutValido(long cuerpo) {
        String c = String.valueOf(cuerpo);
        int suma = 0;
        int m = 2;
        for (int i = c.length() - 1; i >= 0; i--) {
            suma += (c.charAt(i) - '0') * m;
            m = m == 7 ? 2 : m + 1;
        }
        int resto = 11 - (suma % 11);
        char dv = resto == 11 ? '0' : resto == 10 ? 'K' : (char) ('0' + resto);
        return c + "-" + dv;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método crear (crear).
    private Usuario crear(Role rol, String nombres) {
        long n = SECUENCIA.incrementAndGet();
        Usuario u = new Usuario();
        u.setRut(rutValido(n));
        u.setNombres(nombres);
        u.setApellidos("Pérez");
        u.setCorreo("admin-consulta-" + n + "@mail.com");
        u.setPasswordHash(passwordEncoder.encode("Secreta123"));
        u.setRol(rol);
        return usuarios.save(u);
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método token (token).
    private String token(Usuario u) {
        return "Bearer " + jwtService.generar(u);
    }

    @Test
    void administradorConsultaVariosClientesEnElOrdenPedidoSinDatosSensibles() throws Exception {
        Usuario admin = crear(Role.ADMINISTRADOR, "Admin");
        Usuario a = crear(Role.CLIENTE, "Ana");
        Usuario b = crear(Role.CLIENTE, "Beto");
        mockMvc.perform(get(RUTA).param("ids", b.getId() + "," + a.getId() + "," + b.getId())
                        .header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(b.getId()))
                .andExpect(jsonPath("$[0].nombres").value("Beto"))
                .andExpect(jsonPath("$[0].apellidos").value("Pérez"))
                .andExpect(jsonPath("$[0].correo").value(b.getCorreo()))
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"))
                .andExpect(jsonPath("$[1].id").value(a.getId()))
                .andExpect(jsonPath("$[0].rut").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString(a.getRut()))));
    }

    @Test
    void cuentaEnEliminacionYEliminadaConSuEstadoRealYSinReconstruirDatos() throws Exception {
        Usuario admin = crear(Role.ADMINISTRADOR, "Admin");
        Usuario enEliminacion = crear(Role.CLIENTE, "Carla");
        jdbc.update("update usuario set estado = 'EN_ELIMINACION' where id = ?", enEliminacion.getId());
        Usuario eliminada = crear(Role.CLIENTE, "Diego");
        eliminada.iniciarEliminacion();
        eliminada.completarEliminacion();
        usuarios.save(eliminada);
        mockMvc.perform(get(RUTA).param("ids", enEliminacion.getId() + "," + eliminada.getId())
                        .header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("EN_ELIMINACION"))
                .andExpect(jsonPath("$[1].estado").value("ELIMINADA"))
                .andExpect(jsonPath("$[1].nombres").value("Cuenta"))
                .andExpect(jsonPath("$[1].apellidos").value("eliminada"))
                .andExpect(content().string(not(containsString("Diego"))));
    }

    @Test
    void idsInexistentesNoAparecen() throws Exception {
        Usuario admin = crear(Role.ADMINISTRADOR, "Admin");
        Usuario a = crear(Role.CLIENTE, "Elena");
        mockMvc.perform(get(RUTA).param("ids", a.getId() + ",999999999").header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(a.getId()));
        mockMvc.perform(get(RUTA).param("ids", "999999998").header("Authorization", token(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void masDe100IdsOParametrosInvalidos_400ConApiError() throws Exception {
        String admin = token(crear(Role.ADMINISTRADOR, "Admin"));
        String ciento = LongStream.rangeClosed(1, 101).mapToObj(String::valueOf).collect(Collectors.joining(","));
        mockMvc.perform(get(RUTA).param("ids", ciento).header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.ids").exists())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        String cien = LongStream.rangeClosed(1, 100).mapToObj(String::valueOf).collect(Collectors.joining(","));
        mockMvc.perform(get(RUTA).param("ids", cien).header("Authorization", admin)).andExpect(status().isOk());
        mockMvc.perform(get(RUTA).param("ids", "1,abc").header("Authorization", admin)).andExpect(status().isBadRequest());
        mockMvc.perform(get(RUTA).param("ids", "0,-3").header("Authorization", admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.ids").exists());
        mockMvc.perform(get(RUTA).header("Authorization", admin)).andExpect(status().isBadRequest());
    }

    @Test
    void clienteSinPermisos_403() throws Exception {
        Usuario cliente = crear(Role.CLIENTE, "Fabi");
        mockMvc.perform(get(RUTA).param("ids", String.valueOf(cliente.getId())).header("Authorization", token(cliente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void tokenAusenteInvalidoODeServicio_401() throws Exception {
        mockMvc.perform(get(RUTA).param("ids", "1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA).param("ids", "1").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA).param("ids", "1").header("Authorization", TokensServicioPrueba.bearer(Servicios.ORDERS)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administradorQueNoEstaActivo_403() throws Exception {
        Usuario admin = crear(Role.ADMINISTRADOR, "Admin");
        jdbc.update("update usuario set estado = 'ELIMINADA' where id = ?", admin.getId());
        mockMvc.perform(get(RUTA).param("ids", "1").header("Authorization", token(admin)))
                .andExpect(status().isForbidden());
    }
}
