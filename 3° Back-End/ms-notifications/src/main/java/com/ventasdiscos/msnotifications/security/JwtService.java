package com.ventasdiscos.msnotifications.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ventasdiscos.msnotifications.exception.NoAutenticadoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;

/**
 * Lote 1: solo VALIDA el JWT emitido por MS-Accounts (misma clave compartida).
 * Copia del JwtService de ms-catalog: no se agrega ningún mecanismo nuevo.
 */
@Service
public class JwtService {

    // Atributo secret: dato o dependencia utilizada por esta clase.
    private final byte[] secret;

    // Ejecuta jwt service como parte de las responsabilidades de esta clase.
    public JwtService(@Value("${security.jwt.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalStateException("security.jwt.secret debe tener al menos 32 caracteres");
        }
    }

    // Ejecuta validar como parte de las responsabilidades de esta clase.
    public AuthUser validar(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))) {
                throw new NoAutenticadoException("Token inválido");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (!"ms-accounts".equals(claims.getIssuer()) || !claims.getAudience().isEmpty()) {
                throw new NoAutenticadoException("Token inválido");
            }
            Date expira = claims.getExpirationTime();
            if (expira == null || !expira.after(new Date())) {
                throw new NoAutenticadoException("Tu sesión expiró, inicia sesión nuevamente");
            }
            String rol = claims.getStringClaim("rol");
            if (!("CLIENTE".equals(rol) || "ADMINISTRADOR".equals(rol)) || claims.getSubject() == null) {
                throw new NoAutenticadoException("Token inválido");
            }
            Long userId = Long.valueOf(claims.getSubject());
            if (userId <= 0) {
                throw new NoAutenticadoException("Token inválido");
            }
            return new AuthUser(userId, rol);
        } catch (ParseException | JOSEException | IllegalArgumentException e) {
            throw new NoAutenticadoException("Token inválido");
        }
    }
}
