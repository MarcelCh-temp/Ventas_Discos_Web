package com.ventasdiscos.apigateway.seguridad;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lote 6: las rutas declaradas en src/main/resources/application.properties son exactamente las que conoce
 * ReglasAcceso, apuntan a los puertos aprobados y ninguna expone /internal. Así, si alguien agrega una ruta en un
 * lugar y no en el otro, esta prueba falla.
 */
class RutasConfiguradasTest {

    private static final String PREFIJO = "spring.cloud.gateway.server.webmvc.routes[";

    private static Properties propiedades() throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(Path.of("src/main/resources/application.properties"), StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }

    @Test
    void lasRutasCoincidenConLasReglasDeAcceso() throws IOException {
        Properties p = propiedades();
        Set<String> bases = new TreeSet<>();
        for (String clave : p.stringPropertyNames()) {
            if (clave.startsWith(PREFIJO) && clave.endsWith("].predicates[0]")) {
                String valor = p.getProperty(clave);
                assertTrue(valor.startsWith("Path="), clave);
                for (String patron : valor.substring("Path=".length()).split(",")) {
                    assertTrue(patron.endsWith("/**"), patron);
                    assertFalse(patron.toLowerCase().contains("internal"), "ruta interna expuesta: " + patron);
                    bases.add(patron.substring(0, patron.length() - 3));
                }
            }
        }
        assertEquals(new TreeSet<>(ReglasAcceso.BASES_ENRUTADAS), bases);
    }

    @Test
    void seisMicroserviciosEnSusPuertosAprobados() throws IOException {
        Properties p = propiedades();
        Map<String, String> uris = new TreeMap<>();
        for (int i = 0; i < 6; i++) {
            uris.put(p.getProperty(PREFIJO + i + "].id"), p.getProperty(PREFIJO + i + "].uri"));
        }
        assertEquals(Map.of(
                "ms-accounts", "${ACCOUNTS_URL:http://127.0.0.1:8081}",
                "ms-catalog", "${CATALOG_URL:http://127.0.0.1:8082}",
                "ms-orders", "${ORDERS_URL:http://127.0.0.1:8084}",
                "ms-postsale", "${POSTSALE_URL:http://127.0.0.1:8085}",
                "ms-notifications", "${NOTIFICATIONS_URL:http://127.0.0.1:8086}",
                "ms-support", "${SUPPORT_URL:http://127.0.0.1:8087}"), uris);
        assertEquals(null, p.getProperty(PREFIJO + "6].id"), "no hay rutas extra (MS-Inventory no se enruta)");
        assertEquals("${PORT:8080}", p.getProperty("server.port"));
        assertFalse(p.getProperty("gateway.cors.allowed-origins").contains("*"));
        // Cierre (ERS 12.7): tiempos máximos acotados con las propiedades oficiales de Spring Boot 4
        assertEquals("${GATEWAY_CONNECT_TIMEOUT:2s}", p.getProperty("spring.http.clients.connect-timeout"));
        assertEquals("${GATEWAY_READ_TIMEOUT:30s}", p.getProperty("spring.http.clients.read-timeout"));
    }
}
