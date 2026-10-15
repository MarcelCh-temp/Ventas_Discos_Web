package com.ventasdiscos.msaccounts.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ventasdiscos.msaccounts.exception.AccesoDenegadoException;
import com.ventasdiscos.msaccounts.exception.NoAutenticadoException;
import com.ventasdiscos.msaccounts.model.Role;
import com.ventasdiscos.msaccounts.model.Usuario;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HU-02 / RNF-06: el token se emite con userId + rol y solo se aceptan tokens propios y vigentes. */
class JwtServiceTest {

    private static final String SECRET = "test-secret-de-al-menos-32-caracteres-xx";
    private final JwtService jwt = new JwtService(SECRET, 60);

    private static String token(String secret, String subject, String rol, Date expira) throws Exception {
        JWTClaimsSet.Builder b = new JWTClaimsSet.Builder().subject(subject).expirationTime(expira);
        if (rol != null) {
            b.claim("rol", rol);
        }
        SignedJWT s = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), b.build());
        s.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return s.serialize();
    }

    private static Date enUnaHora() {
        return new Date(System.currentTimeMillis() + 3_600_000);
    }

    @Test
    void generaYValidaToken() {
        Usuario u = new Usuario();
        u.setId(9L);
        u.setRol(Role.ADMINISTRADOR);
        AuthUser auth = jwt.validar(jwt.generar(u));
        assertEquals(9L, auth.userId());
        assertEquals(Role.ADMINISTRADOR, auth.rol());
        assertEquals(3600, jwt.getExpiracionSegundos());
        assertTrue(u.esAdministrador());
    }

    @Test
    void tokenFirmadoConOtraClaveSeRechaza() throws Exception {
        String ajeno = token("otra-clave-distinta-de-32-caracteres-xxxx", "7", "CLIENTE", enUnaHora());
        assertThrows(NoAutenticadoException.class, () -> jwt.validar(ajeno));
    }

    @Test
    void tokenExpiradoSeRechaza() throws Exception {
        String viejo = token(SECRET, "7", "CLIENTE", new Date(System.currentTimeMillis() - 1000));
        assertThrows(NoAutenticadoException.class, () -> jwt.validar(viejo));
    }

    @Test
    void tokenIncompletoOInvalidoSeRechaza() throws Exception {
        String sinExp = token(SECRET, "7", "CLIENTE", null);
        String sinRol = token(SECRET, "7", null, enUnaHora());
        String rolInexistente = token(SECRET, "7", "SUPERUSUARIO", enUnaHora());
        assertThrows(NoAutenticadoException.class, () -> jwt.validar(sinExp));
        assertThrows(NoAutenticadoException.class, () -> jwt.validar(sinRol));
        assertThrows(NoAutenticadoException.class, () -> jwt.validar(rolInexistente));
        assertThrows(NoAutenticadoException.class, () -> jwt.validar("esto-no-es-un-jwt"));
    }

    @Test
    void claveCortaNoPermiteArrancar() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corta", 60));
    }

    @Test
    void soloAdministradorPasaRequireAdmin() {
        Usuario cliente = new Usuario();
        cliente.setRol(Role.CLIENTE);
        assertFalse(cliente.esAdministrador());
        assertDoesNotThrow(() -> new AuthUser(1L, Role.ADMINISTRADOR).requireAdmin());
        assertThrows(AccesoDenegadoException.class, () -> new AuthUser(2L, Role.CLIENTE).requireAdmin());
    }
}
