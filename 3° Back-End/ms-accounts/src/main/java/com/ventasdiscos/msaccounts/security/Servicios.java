package com.ventasdiscos.msaccounts.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lote 6 · DOC-05: identificadores de servicio usados en el JWT de servicio (claims iss/sub = emisor, aud = receptor).
 * Son nombres FIJOS del contrato (no se configuran por variable de entorno, para que un error de configuración no
 * cambie la identidad de un servicio). Para ms-ordenes y ms-soporte se usa el nombre del contrato de rutas
 * (/internal/orders, /internal/support); los nombres de carpeta y paquete NO cambian.
 */
public final class Servicios {

    // Constante ACCOUNTS: valor compartido que no cambia durante la ejecución.
    public static final String ACCOUNTS = "ms-accounts";
    // Constante CATALOG: valor compartido que no cambia durante la ejecución.
    public static final String CATALOG = "ms-catalog";
    // Constante ORDERS: valor compartido que no cambia durante la ejecución.
    public static final String ORDERS = "ms-orders";
    // Constante POSTSALE: valor compartido que no cambia durante la ejecución.
    public static final String POSTSALE = "ms-postsale";
    // Constante NOTIFICATIONS: valor compartido que no cambia durante la ejecución.
    public static final String NOTIFICATIONS = "ms-notifications";
    // Constante SUPPORT: valor compartido que no cambia durante la ejecución.
    public static final String SUPPORT = "ms-support";

    /** Identidad de ESTE microservicio. */
    // Constante PROPIO: valor compartido que no cambia durante la ejecución.
    public static final String PROPIO = ACCOUNTS;

    public static final Set<String> TODOS = Set.of(ACCOUNTS, CATALOG, ORDERS, POSTSALE, NOTIFICATIONS, SUPPORT);

    /** Prefijo común de todas las rutas MS -> MS. El API Gateway nunca las expone. */
    // Constante PREFIJO_INTERNO: valor compartido que no cambia durante la ejecución.
    public static final String PREFIJO_INTERNO = "/api/v1/internal/";

    /** Destino (aud) según la ruta interna llamada (CONTRATOS_API_LOTE_6 §4). */
    private static final List<Map.Entry<String, String>> AUDIENCIA_POR_RUTA = List.of(
            Map.entry("/api/v1/internal/accounts/", ACCOUNTS),
            Map.entry("/api/v1/internal/catalog/", CATALOG),
            Map.entry("/api/v1/internal/inventory/", CATALOG),
            Map.entry("/api/v1/internal/orders/", ORDERS),
            Map.entry("/api/v1/internal/post-sale/", POSTSALE),
            Map.entry("/api/v1/internal/notifications/", NOTIFICATIONS),
            Map.entry("/api/v1/internal/support/", SUPPORT));

    // Ejecuta servicios como parte de las responsabilidades de esta clase.
    private Servicios() {
    }

    /** Servicio dueño de una ruta interna, o null si la ruta no pertenece a ningún contrato conocido. */
    // Ejecuta audiencia para como parte de las responsabilidades de esta clase.
    public static String audienciaPara(String path) {
        if (path == null) {
            return null;
        }
        for (Map.Entry<String, String> e : AUDIENCIA_POR_RUTA) {
            if (path.startsWith(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }
}
