package com.ventasdiscos.apigateway;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Lote 6: tokens de prueba con el mismo formato que emite MS-Accounts (y uno de servicio, que NO debe servir). */
public final class TokensPrueba {

    /** Debe coincidir con security.jwt.secret de GatewayITTest. */
    public static final String SECRETO_USUARIOS = "test-secret-de-al-menos-32-caracteres-xx";
    public static final String SECRETO_SERVICIOS = "test-service-secret-de-al-menos-32-caracteres";

    private TokensPrueba() {
    }

    public static String usuario(long userId, String rol, String emisor, long segundosVigencia, String secreto) {
        JWTClaimsSet.Builder b = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .claim("rol", rol)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + segundosVigencia * 1000));
        if (emisor != null) {
            b.issuer(emisor);
        }
        return firmar(b.build(), secreto);
    }

    public static String cliente(long userId) {
        return "Bearer " + usuario(userId, "CLIENTE", "ms-accounts", 600, SECRETO_USUARIOS);
    }

    public static String admin() {
        return "Bearer " + usuario(1L, "ADMINISTRADOR", "ms-accounts", 600, SECRETO_USUARIOS);
    }

    /** JWT de servicio (DOC-05): firmado con OTRO secreto, sin rol y con aud. */
    public static String servicio(String emisor, String audiencia) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(emisor).subject(emisor).audience(audiencia)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 300_000))
                .build();
        return "Bearer " + firmar(claims, SECRETO_SERVICIOS);
    }

    public static String firmar(JWTClaimsSet claims, String secreto) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secreto.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
