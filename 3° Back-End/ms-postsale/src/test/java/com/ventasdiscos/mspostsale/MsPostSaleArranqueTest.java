package com.ventasdiscos.mspostsale;

import com.ventasdiscos.mspostsale.client.AccountsClient;
import com.ventasdiscos.mspostsale.client.InventoryClient;
import com.ventasdiscos.mspostsale.client.NotificationsClient;
import com.ventasdiscos.mspostsale.client.OrdersClient;
import com.ventasdiscos.mspostsale.config.ReposicionReintentoConfig;
import com.ventasdiscos.mspostsale.repository.SolicitudPostVentaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lote 4: arranca el microservicio completo con Boot 4 y H2 y comprueba que la entidad única se mapea en la tabla
 * ORIGINAL solicitudes_postventa (sin tablas hijas de la herencia en una base nueva) y que no vuelve X-Internal-Key.
 */
@SpringBootTest
class MsPostSaleArranqueTest {

    @Autowired
    private ApplicationContext contexto;
    @Autowired
    private SolicitudPostVentaRepository repositorio;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoBean
    private OrdersClient orders;
    @MockitoBean
    private InventoryClient inventory;
    @MockitoBean
    private NotificationsClient notificaciones;
    @MockitoBean
    private AccountsClient cuentas;

    @Test
    void contextLoads_ConBoot4YH2() {
        assertNotNull(repositorio);
        // En las pruebas el reintento programado está desactivado (postsale.reposiciones.reintento-habilitado=false)
        assertTrue(contexto.getBeansOfType(ReposicionReintentoConfig.class).isEmpty());
    }

    @Test
    void laEntidadUnicaUsaLaTablaOriginalConSusColumnasNuevas() {
        List<String> columnas = jdbc.queryForList("select lower(column_name) from information_schema.columns "
                + "where lower(table_name) = 'solicitudes_postventa'", String.class);
        for (String c : List.of("id", "pedido_id", "usuario_id", "fecha_solicitud", "tipo", "estado", "motivo_solicitud",
                "motivo_resolucion", "fecha_resolucion", "solicitud_origen_id", "usuario_resolucion_id")) {
            assertTrue(columnas.contains(c), "falta la columna " + c + " en " + columnas);
        }
        List<String> recepcion = jdbc.queryForList("select lower(column_name) from information_schema.columns "
                + "where lower(table_name) = 'recepciones_devolucion'", String.class);
        assertTrue(recepcion.contains("usuario_recepcion_id"));
        // En una base NUEVA ya no se crean las tablas hijas de la herencia
        List<String> tablas = jdbc.queryForList("select lower(table_name) from information_schema.tables", String.class);
        assertFalse(tablas.contains("solicitudes_cancelacion"));
        assertFalse(tablas.contains("solicitudes_devolucion"));
        assertFalse(tablas.contains("solicitudes_reembolso"));
    }

    @Test
    void sinXInternalKeyEnElCodigoNiLaConfiguracion() throws Exception {
        try (var archivos = Files.walk(Path.of("src/main"))) {
            for (Path p : archivos.filter(Files::isRegularFile).toList()) {
                assertFalse(Files.readString(p).contains("X-Internal-Key"), "X-Internal-Key en " + p);
            }
        }
    }
}
