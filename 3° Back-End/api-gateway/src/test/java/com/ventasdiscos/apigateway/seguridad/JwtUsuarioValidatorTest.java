package com.ventasdiscos.apigateway.seguridad;

import com.nimbusds.jwt.JWTClaimsSet;
import com.ventasdiscos.apigateway.TokensPrueba;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static com.ventasdiscos.apigateway.TokensPrueba.SECRETO_USUARIOS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lote 6 · Decisión 3/4: el Gateway acepta solo JWT de usuario emitidos por MS-Accounts. */
class JwtUsuarioValidatorTest {

    private final JwtUsuarioValidator validator = new JwtUsuarioValidator(SECRETO_USUARIOS);

    @Test
    void aceptaClienteYAdministrador() {
        UsuarioToken c = validator.validar(TokensPrueba.usuario(7, "CLIENTE", "ms-accounts", 60, SECRETO_USUARIOS));
        assertEquals(7L, c.userId());
        assertFalse(c.esAdministrador());
        UsuarioToken a = validator.validar(TokensPrueba.usuario(1, "ADMINISTRADOR", "ms-accounts", 60, SECRETO_USUARIOS));
        assertTrue(a.esAdministrador());
    }

    @Test
    void tokenExpirado_401ConMensajeDeSesion() {
        NoAutenticadoException e = assertThrows(NoAutenticadoException.class, () ->
                validator.validar(TokensPrueba.usuario(7, "CLIENTE", "ms-accounts", -5, SECRETO_USUARIOS)));
        assertEquals(JwtUsuarioValidator.SESION_EXPIRADA, e.getMessage());
    }

    @Test
    void firmaConOtroSecreto_401() {
        assertThrows(NoAutenticadoException.class, () -> validator.validar(
                TokensPrueba.usuario(7, "CLIENTE", "ms-accounts", 60, "otro-secreto-de-al-menos-32-caracteres-zz")));
    }

    @Test
    void jwtDeServicioNoSirveComoJwtDeUsuario() {
        String servicio = TokensPrueba.servicio("ms-orders", "ms-catalog").substring(7);
        assertThrows(NoAutenticadoException.class, () -> validator.validar(servicio));
        // Aun firmado con el secreto de usuarios, un token con forma de servicio (sin rol, con aud) se rechaza
        JWTClaimsSet forma = new JWTClaimsSet.Builder().issuer("ms-orders").subject("ms-orders").audience("ms-catalog")
                .expirationTime(new Date(System.currentTimeMillis() + 60_000)).build();
        assertThrows(NoAutenticadoException.class, () -> validator.validar(TokensPrueba.firmar(forma, SECRETO_USUARIOS)));
    }

    @Test
    void emisorRolOSujetoInvalidos_401() {
        assertThrows(NoAutenticadoException.class, () ->
                validator.validar(TokensPrueba.usuario(7, "CLIENTE", null, 60, SECRETO_USUARIOS)));
        assertThrows(NoAutenticadoException.class, () ->
                validator.validar(TokensPrueba.usuario(7, "CLIENTE", "ms-orders", 60, SECRETO_USUARIOS)));
        assertThrows(NoAutenticadoException.class, () ->
                validator.validar(TokensPrueba.usuario(7, "SUPERUSUARIO", "ms-accounts", 60, SECRETO_USUARIOS)));
        JWTClaimsSet sinNumero = new JWTClaimsSet.Builder().subject("abc").claim("rol", "CLIENTE").issuer("ms-accounts")
                .expirationTime(new Date(System.currentTimeMillis() + 60_000)).build();
        assertThrows(NoAutenticadoException.class, () -> validator.validar(TokensPrueba.firmar(sinNumero, SECRETO_USUARIOS)));
        JWTClaimsSet sinExp = new JWTClaimsSet.Builder().subject("7").claim("rol", "CLIENTE").issuer("ms-accounts").build();
        assertThrows(NoAutenticadoException.class, () -> validator.validar(TokensPrueba.firmar(sinExp, SECRETO_USUARIOS)));
    }

    @Test
    void textoQueNoEsJwt_401() {
        assertThrows(NoAutenticadoException.class, () -> validator.validar("no-es-un-jwt"));
        // alg "none": cabecera {"alg":"none"} sin firma
        assertThrows(NoAutenticadoException.class, () ->
                validator.validar("eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIiwicm9sIjoiQURNSU5JU1RSQURPUiJ9."));
    }

    @Test
    void secretoCorto_noArranca() {
        assertThrows(IllegalStateException.class, () -> new JwtUsuarioValidator("corto"));
    }
}
