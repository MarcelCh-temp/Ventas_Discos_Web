package com.ventasdiscos.mspostsale.config;

import com.ventasdiscos.mspostsale.service.DevolucionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Lote 4 · ERS 11.7 ("si Catalog está indisponible durante una reposición requerida, la operación deberá quedar
 * pendiente/reintentable") y ERS §13 ("registro durable para recuperar fallos parciales").
 * Cada cierto tiempo reintenta las OperacionReposicion PENDIENTE con la MISMA operacionId: Catalog no repone dos
 * veces. Usa la programación de tareas que ya trae Spring (no agrega dependencias ni infraestructura).
 * Se desactiva con postsale.reposiciones.reintento-habilitado=false (las pruebas lo desactivan); en ese caso la
 * operación queda PENDIENTE y visible para el Administrador en el detalle de la devolución.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "postsale.reposiciones.reintento-habilitado", havingValue = "true", matchIfMissing = true)
public class ReposicionReintentoConfig {

    private static final Logger log = LoggerFactory.getLogger(ReposicionReintentoConfig.class);

    private final DevolucionService devolucionService;

    public ReposicionReintentoConfig(DevolucionService devolucionService) {
        this.devolucionService = devolucionService;
    }

    @Scheduled(fixedDelayString = "${postsale.reposiciones.reintento-ms:300000}",
            initialDelayString = "${postsale.reposiciones.reintento-ms:300000}")
    public void reintentar() {
        try {
            int intentadas = devolucionService.reintentarReposicionesPendientes();
            if (intentadas > 0) {
                log.info("Reintento de reposiciones pendientes: {} operaciones enviadas a Catalog", intentadas);
            }
        } catch (RuntimeException ex) {
            log.error("Falló el reintento de reposiciones pendientes; se intentará de nuevo más tarde", ex);
        }
    }
}
