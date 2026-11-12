package com.ventasdiscos.apigateway.seguridad;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Locale;

/**
 * Lote 6 · Decisión 3/4: clasificación de cada ruta que llega al Gateway, según los contratos de los Lotes 1 a 5
 * (CONTRATOS_API_LOTE_6 §2), no solo por su nombre.
 * <ol>
 *   <li>/api/v1/internal/** (sin importar mayúsculas) → INTERNA: nunca se expone (404).</li>
 *   <li>Lo que no pertenece a ninguna ruta del Gateway → NO_EXPUESTA (404).</li>
 *   <li>/api/v1/admin/** y GET /api/v1/accounts/stats (admin aunque no tenga el prefijo) → ADMINISTRADOR.
 *       Se revisa ANTES que las públicas, para que ninguna regla pública pueda abrir una ruta administrativa.</li>
 *   <li>Lista cerrada de endpoints públicos (método + ruta exactos) → PUBLICA, sin token.</li>
 *   <li>Todo lo demás → CLIENTE: exige un JWT de usuario válido (cliente o administrador).</li>
 * </ol>
 * Si en el futuro se agrega una ruta nueva y nadie la declara pública, queda protegida por defecto.
 * El Gateway es la PRIMERA capa: cada MS sigue validando el token, el rol, la propiedad del recurso y el estado de la
 * cuenta (Lote 5).
 */
public final class ReglasAcceso {

    public enum Acceso { PUBLICA, CLIENTE, ADMINISTRADOR, INTERNA, NO_EXPUESTA }

    /** Debe coincidir con los predicados Path= de application.properties (lo comprueba RutasConfiguradasTest). */
    static final List<String> BASES_ENRUTADAS = List.of(
            // MS-Accounts (8081)
            "/api/v1/accounts", "/api/v1/auth", "/api/v1/admin/accounts",
            // MS-Catalog (8082), incluye el inventario integrado (ERS V2.0)
            "/api/v1/products", "/api/v1/offers", "/api/v1/favorites", "/api/v1/inventory",
            "/api/v1/admin/products", "/api/v1/admin/inventory", "/api/v1/admin/promotions",
            // MS-Orders (ms-ordenes, 8084)
            "/api/v1/cart", "/api/v1/checkout", "/api/v1/orders", "/api/v1/admin/orders",
            // MS-PostSale (8085)
            "/api/v1/post-sale", "/api/v1/admin/post-sale",
            // MS-Notifications (8086)
            "/api/v1/notifications",
            // MS-Support (ms-soporte, 8087)
            "/api/v1/support", "/api/v1/admin/support");

    private static final PathPatternParser PARSER = PathPatternParser.defaultInstance;

    private record Regla(String metodo, PathPattern patron) {
        boolean coincide(String metodoPedido, PathContainer ruta) {
            String m = "HEAD".equals(metodoPedido) ? "GET" : metodoPedido;
            return metodo.equals(m) && patron.matches(ruta);
        }
    }

    /** Endpoints públicos de los contratos aprobados (sin AuthUser en el controller). */
    private static final List<Regla> PUBLICAS = List.of(
            // Accounts: registro, inicio de sesión y recuperación de acceso (HU-01, HU-02, HU-04, HU-05)
            regla("POST", "/api/v1/accounts/register"),
            regla("POST", "/api/v1/auth/login"),
            regla("POST", "/api/v1/auth/recovery/password"),
            regla("POST", "/api/v1/auth/recovery/email"),
            // Catalog: catálogo público, ofertas y disponibilidad pública (sin stock exacto)
            regla("GET", "/api/v1/products"),
            regla("GET", "/api/v1/products/{id}"),
            regla("GET", "/api/v1/offers"),
            regla("GET", "/api/v1/inventory/{productId}/availability"),
            regla("POST", "/api/v1/inventory/availability/query"),
            // Orders: opciones de entrega y pago (informativas)
            regla("GET", "/api/v1/checkout/delivery-methods"),
            regla("GET", "/api/v1/checkout/payment-methods"));

    private ReglasAcceso() {
    }

    private static Regla regla(String metodo, String patron) {
        return new Regla(metodo, PARSER.parse(patron));
    }

    public static Acceso clasificar(String metodo, String path) {
        String minusculas = path.toLowerCase(Locale.ROOT);
        if (minusculas.equals("/api/v1/internal") || minusculas.startsWith("/api/v1/internal/")) {
            return Acceso.INTERNA;
        }
        if (!enrutada(path)) {
            return Acceso.NO_EXPUESTA;
        }
        if (minusculas.startsWith("/api/v1/admin/")
                || minusculas.equals("/api/v1/accounts/stats") || minusculas.startsWith("/api/v1/accounts/stats/")) {
            return Acceso.ADMINISTRADOR;
        }
        PathContainer ruta = PathContainer.parsePath(path);
        for (Regla r : PUBLICAS) {
            if (r.coincide(metodo, ruta)) {
                return Acceso.PUBLICA;
            }
        }
        return Acceso.CLIENTE;
    }

    static boolean enrutada(String path) {
        for (String base : BASES_ENRUTADAS) {
            if (path.equals(base) || path.startsWith(base + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rutas escritas de forma rara para intentar saltarse las reglas: cualquier carácter codificado con "%" (por
     * ejemplo /api/v1/%61dmin o %2e%2e), ";", "\\", barras dobles o segmentos "." y "..". Ningún contrato del ERS
     * los usa en la RUTA (los filtros van en la query, que no se revisa aquí), así que se rechazan (400) antes de
     * clasificar. Así la ruta que clasifica el Gateway es exactamente la que recibirá el microservicio.
     * Se evalúa sobre la URL SIN decodificar (getRequestURI).
     */
    public static boolean rutaSospechosa(String uriCruda) {
        if (uriCruda == null || uriCruda.isEmpty() || !uriCruda.startsWith("/")) {
            return true;
        }
        String u = uriCruda.toLowerCase(Locale.ROOT);
        if (u.contains("%") || u.contains(";") || u.contains("\\") || u.contains("//")) {
            return true;
        }
        for (String segmento : u.split("/")) {
            if (segmento.equals(".") || segmento.equals("..")) {
                return true;
            }
        }
        return false;
    }
}
