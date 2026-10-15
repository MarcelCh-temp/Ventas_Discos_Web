package com.ventasdiscos.msinventory.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ventasdiscos.msinventory.exception.AccesoDenegadoException;
import com.ventasdiscos.msinventory.exception.NoAutenticadoException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El MS acepta solo tokens firmados por Accounts, vigentes y con userId + rol (RNF-06). */
class JwtServiceTest {

    private static final String SECRET = "test-secret-de-al-menos-32-caracteres-xx";
    private final JwtService jwt = new JwtService(SECRET);

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
    void tokenValidoEntregaUsuarioYRol() throws Exception {
        AuthUser u = jwt.validar(token(SECRET, "7", "CLIENTE", enUnaHora()));
        assertEquals(7L, u.userId());
        assertEquals("CLIENTE", u.rol());
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
    void tokenSinExpiracionSinRolOConSubjectInvalidoSeRechaza() throws Exception {
        String sinExp = token(SECRET, "7", "CLIENTE", null);
        String sinRol = token(SECRET, "7", null, enUnaHora());
        String subjectTexto = token(SECRET, "abc", "CLIENTE", enUnaHora());
        assertThrows(NoAutenticadoException.class, () -> jwt.validar(sinExp));
        assertThrows(NoAutenticadoException.class, () -> jwt.validar(sinRol));
        assertThrows(NoAutenticadoException.class, () -> jwt.validar(subjectTexto));
    }

    @Test
    void textoQueNoEsTokenSeRechaza() {
        assertThrows(NoAutenticadoException.class, () -> jwt.validar("esto-no-es-un-jwt"));
    }

    @Test
    void claveCortaNoPermiteArrancar() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corta"));
    }

    @Test
    void soloAdministradorPasaRequireAdmin() {
        AuthUser admin = new AuthUser(1L, AuthUser.ADMINISTRADOR);
        AuthUser cliente = new AuthUser(2L, "CLIENTE");
        assertTrue(admin.esAdministrador());
        assertFalse(cliente.esAdministrador());
        assertDoesNotThrow(admin::requireAdmin);
        assertThrows(AccesoDenegadoException.class, cliente::requireAdmin);
    }
}
