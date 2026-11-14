package com.ventasdiscos.msaccounts;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ventasdiscos.msaccounts.security.Servicios;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Lote 6 · DOC-05: JWT de servicio de prueba, como los que firma el MS que llama (sin levantarlo).
 * El secreto coincide con security.service-jwt.secret de src/test/resources/application.properties.
 */
public final class TokensServicioPrueba {

    public static final String SECRETO = "test-service-secret-de-al-menos-32-caracteres";
    /** security.jwt.secret de pruebas (el de los USUARIOS). */
    private static final String SECRETO_USUARIOS = "test-secret-de-al-menos-32-caracteres-xx";

    private TokensServicioPrueba() {
    }

    /** "Bearer ..." de {@code emisor} para llamar a ESTE microservicio. */
    public static String bearer(String emisor) {
        return "Bearer " + token(emisor, Servicios.PROPIO, 300, SECRETO);
    }

    /** JWT de USUARIO administrador (como los de MS-Accounts): NO debe servir en rutas internas. */
    public static String usuarioAdmin() {
        return usuario(1L, "ADMINISTRADOR");
    }

    /** "Bearer ..." de un JWT de USUARIO como los que emite MS-Accounts (cierre RN-44). */
    public static String usuario(long userId, String rol) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId)).claim("rol", rol).issuer("ms-accounts")
                .expirationTime(new Date(System.currentTimeMillis() + 600_000))
                .build();
        return "Bearer " + firmar(claims, SECRETO_USUARIOS);
    }

    public static String token(String emisor, String audiencia, long segundos, String secreto) {
        long ahora = System.currentTimeMillis();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(emisor).subject(emisor).audience(audiencia)
                .issueTime(new Date(ahora))
                .expirationTime(new Date(ahora + segundos * 1000))
                .build();
        return firmar(claims, secreto);
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método firmar (firmar).
    private static String firmar(JWTClaimsSet claims, String secreto) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secreto.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
