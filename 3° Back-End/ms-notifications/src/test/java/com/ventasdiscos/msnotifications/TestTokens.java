package com.ventasdiscos.msnotifications;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Lote 1.1: genera tokens como los que emite MS-Accounts, para probar sin levantarlo (igual que en ms-soporte). */
public final class TestTokens {

    /** Debe coincidir con security.jwt.secret de src/test/resources/application.properties. */
    private static final String SECRET = "test-secret-de-al-menos-32-caracteres-xx";

    private TestTokens() {
    }

    public static String cliente(long userId) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(userId))
                    .claim("rol", "CLIENTE")
                    .expirationTime(new Date(System.currentTimeMillis() + 600_000))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
            return "Bearer " + jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
