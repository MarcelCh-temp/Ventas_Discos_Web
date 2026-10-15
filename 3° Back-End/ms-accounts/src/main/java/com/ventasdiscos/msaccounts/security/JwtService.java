package com.ventasdiscos.msaccounts.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ventasdiscos.msaccounts.exception.NoAutenticadoException;
import com.ventasdiscos.msaccounts.model.Role;
import com.ventasdiscos.msaccounts.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Emite y valida el JWT (HS256) con userId + rol.
 * Para qué: los demás MS validan el token con la misma clave SIN llamar a Accounts
 * en cada petición (prompt §10).
 */
@Service
public class JwtService {

    private final byte[] secret;
    private final long expiracionMinutos;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.expiration-minutes}") long expiracionMinutos) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalStateException("security.jwt.secret debe tener al menos 32 caracteres");
        }
        this.expiracionMinutos = expiracionMinutos;
    }

    /** HU-02: token emitido tras validar credenciales. El rol viene de la cuenta (RN-02). */
    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("rol", usuario.getRol().name())
                .issuer("ms-accounts")
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }

    public long getExpiracionSegundos() {
        return expiracionMinutos * 60;
    }

    /** Valida firma y expiración. Cualquier problema -> 401. */
    public AuthUser validar(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secret))) {
                throw new NoAutenticadoException("Token inválido");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expira = claims.getExpirationTime();
            if (expira == null || expira.before(new Date())) {
                throw new NoAutenticadoException("Tu sesión expiró, inicia sesión nuevamente");
            }
            String rol = claims.getStringClaim("rol");
            if (rol == null || claims.getSubject() == null) {
                throw new NoAutenticadoException("Token inválido");
            }
            return new AuthUser(Long.valueOf(claims.getSubject()), Role.valueOf(rol));
        } catch (ParseException | JOSEException | IllegalArgumentException e) {
            throw new NoAutenticadoException("Token inválido");
        }
    }
}
