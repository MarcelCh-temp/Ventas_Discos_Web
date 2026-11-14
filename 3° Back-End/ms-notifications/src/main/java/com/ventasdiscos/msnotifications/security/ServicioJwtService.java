package com.ventasdiscos.msnotifications.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ventasdiscos.msnotifications.exception.AccesoDenegadoException;
import com.ventasdiscos.msnotifications.exception.NoAutenticadoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Lote 6 · DOC-05: JWT DE SERVICIO para las llamadas MS -> MS (rutas /api/v1/internal/**).
 * <ul>
 *   <li>Firma HS256 con SERVICE_JWT_SECRET, que debe ser DISTINTO de JWT_SECRET (el de los usuarios).</li>
 *   <li>Claims: iss = sub = servicio emisor, aud = servicio receptor, iat = emisión, exp = iat + 5 minutos.</li>
 *   <li>Se genera un token nuevo en cada llamada (no se guarda ni se reutiliza).</li>
 *   <li>El receptor valida firma, algoritmo, expiración, emisor conocido, audiencia = él mismo y que el emisor
 *       esté autorizado para la ruta (anotación {@link ServiciosPermitidos}).</li>
 *   <li>Un JWT de usuario (trae "rol") nunca sirve aquí; y un JWT de servicio no sirve en rutas de clientes porque
 *       está firmado con otro secreto.</li>
 * </ul>
 * Límite conocido (REGISTRO_LOTE_1_CORREGIDO §12.3): con un secreto compartido HS256, un servicio comprometido podría
 * hacerse pasar por otro SERVICIO (no por un usuario). No se usan claves asimétricas en el alcance del curso.
 * Nunca se registra el token en los logs.
 */
@Service
public class ServicioJwtService {

    /** Vigencia aprobada (Lote 6, decisión 4/4): 5 minutos. */
    // Constante VIGENCIA_SEGUNDOS: valor compartido que no cambia durante la ejecución.
    public static final long VIGENCIA_SEGUNDOS = 300;
    /** Diferencia de reloj tolerada solo para un iat "en el futuro" (los MS corren en el mismo equipo). */
    // Constante TOLERANCIA_RELOJ_SEGUNDOS: valor compartido que no cambia durante la ejecución.
    static final long TOLERANCIA_RELOJ_SEGUNDOS = 30;

    // Constante TOKEN_INVALIDO: valor compartido que no cambia durante la ejecución.
    static final String TOKEN_INVALIDO = "Token de servicio inválido";
    // Constante TOKEN_EXPIRADO: valor compartido que no cambia durante la ejecución.
    static final String TOKEN_EXPIRADO = "El token de servicio expiró";

    // Atributo secreto: dato o dependencia utilizada por esta clase.
    private final byte[] secreto;
    // Atributo reloj: dato o dependencia utilizada por esta clase.
    private final Clock reloj;

    @Autowired
    public ServicioJwtService(@Value("${security.service-jwt.secret}") String secreto,
                              @Value("${security.jwt.secret}") String secretoUsuarios) {
        this(secreto, secretoUsuarios, Clock.systemUTC());
    }

    /** Para pruebas: permite fijar el reloj. */
    ServicioJwtService(String secreto, String secretoUsuarios, Clock reloj) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("security.service-jwt.secret (SERVICE_JWT_SECRET) debe tener al menos 32 caracteres");
        }
        if (secreto.equals(secretoUsuarios)) {
            throw new IllegalStateException("SERVICE_JWT_SECRET debe ser distinto de JWT_SECRET (DOC-05)");
        }
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.reloj = reloj;
    }

    /** Token nuevo para llamar a {@code audiencia} (uno por llamada). */
    // Ejecuta emitir como parte de las responsabilidades de esta clase.
    public String emitir(String audiencia) {
        if (audiencia == null || !Servicios.TODOS.contains(audiencia)) {
            throw new IllegalArgumentException("Servicio destino desconocido: " + audiencia);
        }
        Instant ahora = reloj.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(Servicios.PROPIO)
                .subject(Servicios.PROPIO)
                .audience(audiencia)
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plusSeconds(VIGENCIA_SEGUNDOS)))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secreto));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token de servicio", e);
        }
    }

    /**
     * Valida el token recibido en una ruta interna y devuelve el servicio emisor.
     * 401 (NoAutenticadoException) si el token no es un JWT de servicio válido para este MS;
     * 403 (AccesoDenegadoException) si es válido pero el emisor no está autorizado para la operación.
     */
    public String validar(String token, Collection<String> emisoresPermitidos) {
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
        String emisor = c.getIssuer();
        if (emisor == null || !emisor.equals(c.getSubject()) || !Servicios.TODOS.contains(emisor)) {
            throw new NoAutenticadoException(TOKEN_INVALIDO);
        }
        if (c.getClaim("rol") != null) {
            // Un JWT de usuario nunca autoriza una ruta interna.
            throw new NoAutenticadoException(TOKEN_INVALIDO);
        }
        List<String> audiencia = c.getAudience();
        if (audiencia == null || !audiencia.contains(Servicios.PROPIO)) {
            throw new NoAutenticadoException(TOKEN_INVALIDO);
        }
        Date iat = c.getIssueTime();
        Date exp = c.getExpirationTime();
        if (iat == null || exp == null) {
            throw new NoAutenticadoException(TOKEN_INVALIDO);
        }
        Instant ahora = reloj.instant();
        if (!exp.toInstant().isAfter(ahora)) {
            throw new NoAutenticadoException(TOKEN_EXPIRADO);
        }
        if (iat.toInstant().isAfter(ahora.plusSeconds(TOLERANCIA_RELOJ_SEGUNDOS))
                || Duration.between(iat.toInstant(), exp.toInstant()).getSeconds() > VIGENCIA_SEGUNDOS) {
            throw new NoAutenticadoException(TOKEN_INVALIDO);
        }
        if (emisoresPermitidos == null || !emisoresPermitidos.contains(emisor)) {
            throw new AccesoDenegadoException("El servicio " + emisor + " no está autorizado para esta operación interna");
        }
        return emisor;
    }
}
