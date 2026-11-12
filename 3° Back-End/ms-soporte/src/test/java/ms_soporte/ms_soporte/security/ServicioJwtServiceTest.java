package ms_soporte.ms_soporte.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import ms_soporte.ms_soporte.exception.AccesoDenegadoException;
import ms_soporte.ms_soporte.exception.NoAutenticadoException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Lote 6 · DOC-05: JWT de servicio (firma, emisor, audiencia, expiración y autorización por servicio). */
class ServicioJwtServiceTest {

    private static final String SECRETO = "secreto-de-servicio-de-prueba-de-32-o-mas";
    private static final String SECRETO_USUARIOS = "secreto-de-usuarios-de-prueba-de-32-o-mas";
    private static final Instant AHORA = Instant.parse("2026-11-11T12:00:00Z");

    private final ServicioJwtService servicio = new ServicioJwtService(SECRETO, SECRETO_USUARIOS, reloj(AHORA));

    private static Clock reloj(Instant instante) {
        return Clock.fixed(instante, ZoneOffset.UTC);
    }

    private static String firmar(JWTClaimsSet claims, String secreto) throws JOSEException {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secreto.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    private static JWTClaimsSet.Builder base(String emisor, String audiencia) {
        return new JWTClaimsSet.Builder().issuer(emisor).subject(emisor).audience(audiencia)
                .issueTime(Date.from(AHORA)).expirationTime(Date.from(AHORA.plusSeconds(300)));
    }

    @Test
    void emiteConLasClaimsAprobadas() throws Exception {
        String token = servicio.emitir(Servicios.CATALOG);
        JWTClaimsSet c = SignedJWT.parse(token).getJWTClaimsSet();
        assertEquals(Servicios.PROPIO, c.getIssuer());
        assertEquals(Servicios.PROPIO, c.getSubject());
        assertEquals(List.of(Servicios.CATALOG), c.getAudience());
        assertEquals(AHORA.getEpochSecond(), c.getIssueTime().toInstant().getEpochSecond());
        assertEquals(AHORA.plusSeconds(300).getEpochSecond(), c.getExpirationTime().toInstant().getEpochSecond());
        assertEquals(null, c.getClaim("rol"));
        assertThrows(IllegalArgumentException.class, () -> servicio.emitir("ms-desconocido"));
    }

    @Test
    void tokenNuevoEnCadaLlamada() {
        ServicioJwtService a = new ServicioJwtService(SECRETO, SECRETO_USUARIOS, reloj(AHORA));
        ServicioJwtService b = new ServicioJwtService(SECRETO, SECRETO_USUARIOS, reloj(AHORA.plusSeconds(1)));
        assertEquals(false, a.emitir(Servicios.ACCOUNTS).equals(b.emitir(Servicios.ACCOUNTS)));
    }

    @Test
    void aceptaUnTokenValidoDeUnEmisorAutorizado() throws Exception {
        String token = firmar(base(Servicios.ORDERS, Servicios.PROPIO).build(), SECRETO);
        assertEquals(Servicios.ORDERS, servicio.validar(token, List.of(Servicios.ORDERS, Servicios.POSTSALE)));
        // El propio emitir() también lo valida el receptor correcto
        ServicioJwtService receptor = new ServicioJwtService(SECRETO, SECRETO_USUARIOS, reloj(AHORA.plusSeconds(10)));
        assertEquals(Servicios.PROPIO, receptor.validar(servicio.emitir(Servicios.PROPIO), List.of(Servicios.PROPIO)));
    }

    @Test
    void emisorNoAutorizadoParaLaRuta_403() throws Exception {
        String token = firmar(base(Servicios.SUPPORT, Servicios.PROPIO).build(), SECRETO);
        assertThrows(AccesoDenegadoException.class, () -> servicio.validar(token, List.of(Servicios.ACCOUNTS)));
        assertThrows(AccesoDenegadoException.class, () -> servicio.validar(token, List.of()));
    }

    @Test
    void audienciaDeOtroServicio_401() throws Exception {
        String otro = Servicios.PROPIO.equals(Servicios.CATALOG) ? Servicios.ORDERS : Servicios.CATALOG;
        String token = firmar(base(Servicios.ACCOUNTS, otro).build(), SECRETO);
        assertThrows(NoAutenticadoException.class, () -> servicio.validar(token, List.of(Servicios.ACCOUNTS)));
    }

    @Test
    void expiradoOConVigenciaMayorA5Minutos_401() throws Exception {
        String token = firmar(base(Servicios.ACCOUNTS, Servicios.PROPIO).build(), SECRETO);
        ServicioJwtService tarde = new ServicioJwtService(SECRETO, SECRETO_USUARIOS, reloj(AHORA.plusSeconds(300)));
        NoAutenticadoException e = assertThrows(NoAutenticadoException.class,
                () -> tarde.validar(token, List.of(Servicios.ACCOUNTS)));
        assertEquals(ServicioJwtService.TOKEN_EXPIRADO, e.getMessage());
        String largo = firmar(base(Servicios.ACCOUNTS, Servicios.PROPIO)
                .expirationTime(Date.from(AHORA.plusSeconds(3600))).build(), SECRETO);
        assertThrows(NoAutenticadoException.class, () -> servicio.validar(largo, List.of(Servicios.ACCOUNTS)));
        String futuro = firmar(base(Servicios.ACCOUNTS, Servicios.PROPIO).issueTime(Date.from(AHORA.plusSeconds(120)))
                .expirationTime(Date.from(AHORA.plusSeconds(400))).build(), SECRETO);
        assertThrows(NoAutenticadoException.class, () -> servicio.validar(futuro, List.of(Servicios.ACCOUNTS)));
        String sinFechas = firmar(new JWTClaimsSet.Builder().issuer(Servicios.ACCOUNTS).subject(Servicios.ACCOUNTS)
                .audience(Servicios.PROPIO).build(), SECRETO);
        assertThrows(NoAutenticadoException.class, () -> servicio.validar(sinFechas, List.of(Servicios.ACCOUNTS)));
    }

    @Test
    void jwtDeUsuarioNoSirveEnRutasInternas_401() throws Exception {
        // Firmado con el secreto de USUARIOS (como los de MS-Accounts)
        JWTClaimsSet usuario = new JWTClaimsSet.Builder().subject("7").claim("rol", "ADMINISTRADOR").issuer("ms-accounts")
                .expirationTime(Date.from(AHORA.plusSeconds(600))).build();
        assertThrows(NoAutenticadoException.class,
                () -> servicio.validar(firmar(usuario, SECRETO_USUARIOS), List.of(Servicios.ACCOUNTS)));
        // Aunque alguien lo firme con el secreto de servicio, un token con "rol" se rechaza
        String conRol = firmar(base(Servicios.ACCOUNTS, Servicios.PROPIO).claim("rol", "ADMINISTRADOR").build(), SECRETO);
        assertThrows(NoAutenticadoException.class, () -> servicio.validar(conRol, List.of(Servicios.ACCOUNTS)));
    }

    @Test
    void emisorDesconocidoOInconsistente_401() throws Exception {
        String desconocido = firmar(base("ms-inventory", Servicios.PROPIO).build(), SECRETO);
        assertThrows(NoAutenticadoException.class, () -> servicio.validar(desconocido, List.of("ms-inventory")));
        String distinto = firmar(base(Servicios.ACCOUNTS, Servicios.PROPIO).subject(Servicios.ORDERS).build(), SECRETO);
        assertThrows(NoAutenticadoException.class, () -> servicio.validar(distinto, List.of(Servicios.ACCOUNTS, Servicios.ORDERS)));
        assertThrows(NoAutenticadoException.class, () -> servicio.validar("no-es-un-jwt", List.of(Servicios.ACCOUNTS)));
        // alg "none" (sin firma)
        assertThrows(NoAutenticadoException.class, () -> servicio.validar(
                "eyJhbGciOiJub25lIn0.eyJpc3MiOiJtcy1hY2NvdW50cyJ9.", List.of(Servicios.ACCOUNTS)));
    }

    @Test
    void secretoCortoOIgualAlDeUsuarios_noArranca() {
        assertThrows(IllegalStateException.class, () -> new ServicioJwtService("corto", SECRETO_USUARIOS, reloj(AHORA)));
        assertThrows(IllegalStateException.class, () -> new ServicioJwtService(SECRETO, SECRETO, reloj(AHORA)));
        assertThrows(IllegalStateException.class, () -> new ServicioJwtService(null, SECRETO_USUARIOS, reloj(AHORA)));
    }

    @Test
    void audienciaPorRutaInterna() {
        assertEquals(Servicios.ACCOUNTS, Servicios.audienciaPara("/api/v1/internal/accounts/5/operation-status"));
        assertEquals(Servicios.CATALOG, Servicios.audienciaPara("/api/v1/internal/catalog/pricing"));
        assertEquals(Servicios.CATALOG, Servicios.audienciaPara("/api/v1/internal/inventory/operations"));
        assertEquals(Servicios.ORDERS, Servicios.audienciaPara("/api/v1/internal/orders/7/cancel"));
        assertEquals(Servicios.POSTSALE, Servicios.audienciaPara("/api/v1/internal/post-sale/users/5/pending-exists"));
        assertEquals(Servicios.NOTIFICATIONS, Servicios.audienciaPara("/api/v1/internal/notifications/events"));
        assertEquals(Servicios.SUPPORT, Servicios.audienciaPara("/api/v1/internal/support/users/5/open-exists"));
        assertEquals(null, Servicios.audienciaPara("/api/v1/internal/otra/cosa"));
        assertEquals(null, Servicios.audienciaPara(null));
    }
}
