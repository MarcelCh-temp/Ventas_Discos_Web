package com.ventasdiscos.msaccounts;

import com.jayway.jsonpath.JsonPath;
import com.ventasdiscos.msaccounts.model.Role;
import com.ventasdiscos.msaccounts.model.Usuario;
import com.ventasdiscos.msaccounts.repository.UsuarioRepository;
import com.ventasdiscos.msaccounts.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de punta a punta (HTTP -> Controller -> Service -> H2) de EP-01. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CuentaITTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private JwtService jwtService;

    private static final String REGISTRO = """
            {"rut":"12.345.678-5","nombres":"Ana","apellidos":"Pérez",
             "correo":"ana@mail.com","confirmacionCorreo":"ana@mail.com",
             "password":"Secreta123","confirmacionPassword":"Secreta123",
             "rol":"ADMINISTRADOR"}
            """;

    @Test
    void registroIgnoraRolEnviadoYCreaCliente_201() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/registro").contentType(MediaType.APPLICATION_JSON).content(REGISTRO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registroConCamposVaciosIndicaQueCorregir_400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/registro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.rut").exists())
                .andExpect(jsonPath("$.errores.correo").exists());
    }

    @Test
    void registroDuplicado_409() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/registro").contentType(MediaType.APPLICATION_JSON).content(REGISTRO))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/accounts/registro").contentType(MediaType.APPLICATION_JSON).content(REGISTRO))
                .andExpect(status().isConflict());
    }

    @Test
    void perfilSinToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginYPerfilPropio_200_yStatsComoCliente_403() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/registro").contentType(MediaType.APPLICATION_JSON).content(REGISTRO))
                .andExpect(status().isCreated());

        String login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ANA@mail.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(login, "$.token");

        mockMvc.perform(get("/api/v1/accounts/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@mail.com"));

        mockMvc.perform(get("/api/v1/accounts/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginIncorrecto_401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"nadie@mail.com\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    /** Registra a Ana y devuelve "Bearer <token>" de su login. */
    private String registrarYLoguear() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/registro").contentType(MediaType.APPLICATION_JSON).content(REGISTRO))
                .andExpect(status().isCreated());
        String login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@mail.com\",\"password\":\"Secreta123\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(login, "$.token");
    }

    @Test
    void editarPerfilYCambiarPassword_200() throws Exception {
        String token = registrarYLoguear();
        mockMvc.perform(patch("/api/v1/accounts/me").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombres\":\"Ana María\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Ana María"));
        mockMvc.perform(put("/api/v1/accounts/me/password").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"Secreta123\",\"nuevaPassword\":\"Nueva12345\",\"confirmacionPassword\":\"Nueva12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());

        // AC-04: la contraseña anterior deja de servir y la nueva sí
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@mail.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@mail.com\",\"password\":\"Nueva12345\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void recuperarPasswordYCorreo_200() throws Exception {
        registrarYLoguear();
        mockMvc.perform(post("/api/v1/auth/recuperacion/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@mail.com\",\"rut\":\"12345678-5\",\"nuevaPassword\":\"Nueva12345\",\"confirmacionPassword\":\"Nueva12345\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/recuperacion/correo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rut\":\"12345678-5\",\"nuevoCorreo\":\"ana2@mail.com\",\"confirmacionCorreo\":\"ana2@mail.com\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana2@mail.com\",\"password\":\"Nueva12345\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void estadisticasComoAdministrador_200() throws Exception {
        Usuario admin = new Usuario();
        admin.setRut("11111111-1");
        admin.setNombres("Admin");
        admin.setApellidos("Tienda");
        admin.setCorreo("admin@tienda.cl");
        admin.setPasswordHash("x");
        admin.setRol(Role.ADMINISTRADOR);
        String token = "Bearer " + jwtService.generar(usuarioRepository.save(admin));

        mockMvc.perform(get("/api/v1/accounts/stats").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsuarios").exists());
    }

    @Test
    void tokenMalFormado_401_yJsonInvalido_400_yRutaInexistente_404() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me").header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{malo"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isNotFound());
    }

    /**
     * AC5 / AC-01: si dos registros simultáneos pasan ambos el existsBy..., el UNIQUE de la BD
     * rechaza el segundo (aquí se simula guardando directo, saltándose la validación del Service).
     */
    @Test
    void restriccionUnicaDeLaBdRechazaRutYCorreoDuplicados() {
        usuarioRepository.saveAndFlush(usuario("22222222-2", "uno@mail.com"));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> usuarioRepository.saveAndFlush(usuario("22222222-2", "otro@mail.com")));
    }

    @Test
    void restriccionUnicaDeLaBdRechazaCorreoDuplicado() {
        usuarioRepository.saveAndFlush(usuario("33333333-3", "repetido@mail.com"));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> usuarioRepository.saveAndFlush(usuario("44444444-4", "repetido@mail.com")));
    }

    private static Usuario usuario(String rut, String correo) {
        Usuario u = new Usuario();
        u.setRut(rut);
        u.setNombres("N");
        u.setApellidos("A");
        u.setCorreo(correo);
        u.setPasswordHash("hash");
        u.setRol(Role.CLIENTE);
        return u;
    }
}
