package com.ventasdiscos.mscatalog.security;

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

    public static final String ACCOUNTS = "ms-accounts";
    public static final String CATALOG = "ms-catalog";
    public static final String ORDERS = "ms-orders";
    public static final String POSTSALE = "ms-postsale";
    public static final String NOTIFICATIONS = "ms-notifications";
    public static final String SUPPORT = "ms-support";

    /** Identidad de ESTE microservicio. */
    public static final String PROPIO = CATALOG;

    public static final Set<String> TODOS = Set.of(ACCOUNTS, CATALOG, ORDERS, POSTSALE, NOTIFICATIONS, SUPPORT);

    /** Prefijo común de todas las rutas MS -> MS. El API Gateway nunca las expone. */
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

    private Servicios() {
    }

    /** Servicio dueño de una ruta interna, o null si la ruta no pertenece a ningún contrato conocido. */
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
