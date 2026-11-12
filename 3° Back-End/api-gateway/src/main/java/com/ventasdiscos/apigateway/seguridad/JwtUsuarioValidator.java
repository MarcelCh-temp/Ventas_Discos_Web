package com.ventasdiscos.apigateway.seguridad;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Lote 6 · Decisión 3/4: valida el JWT DE USUARIO en el Gateway, con el mismo contrato que emite MS-Accounts
 * (JwtService de Accounts): HS256 con JWT_SECRET, sub = userId numérico, claim "rol" (CLIENTE o ADMINISTRADOR),
 * iss = "ms-accounts" y exp vigente.
 * Un JWT DE SERVICIO no sirve aquí: está firmado con otro secreto (SERVICE_JWT_SECRET, que el Gateway ni siquiera
 * conoce), no trae "rol" y trae "aud". Los mensajes son los mismos que usan los MS.
 */
@Component
public class JwtUsuarioValidator {

    static final String EMISOR = "ms-accounts";
    static final Set<String> ROLES = Set.of("CLIENTE", "ADMINISTRADOR");
    static final String TOKEN_INVALIDO = "Token inválido";
    static final String SESION_EXPIRADA = "Tu sesión expiró, inicia sesión nuevamente";

    private final byte[] secreto;

    public JwtUsuarioValidator(@Value("${security.jwt.secret}") String secreto) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("security.jwt.secret (JWT_SECRET) debe tener al menos 32 caracteres");
        }
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    /** Usuario del token, o NoAutenticadoException (401). */
    public UsuarioToken validar(String token) {
        JWTClaimsSet c;
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secreto))) {
                throw new NoAutenticadoException(TOKEN_INVALIDO);
            }
            c = jwt.getJWTClaimsSet();
        } catch (ParseException | JOSEException e) {
            throw new NoAutenticadoException(TOKEN_INVALIDO);
        }
        Date exp = c.getExpirationTime();
        if (exp == null || !exp.after(new Date())) {
            throw new NoAutenticadoException(SESION_EXPIRADA);
        }
        Object rol = c.getClaim("rol");
        List<String> audiencia = c.getAudience();
        if (!EMISOR.equals(c.getIssuer()) || !(rol instanceof String r) || !ROLES.contains(r)
                || (audiencia != null && !audiencia.isEmpty())) {
            throw new NoAutenticadoException(TOKEN_INVALIDO);
        }
        try {
            return new UsuarioToken(Long.valueOf(c.getSubject()), r);
        } catch (NumberFormatException e) {
            throw new NoAutenticadoException(TOKEN_INVALIDO);
        }
    }
}
