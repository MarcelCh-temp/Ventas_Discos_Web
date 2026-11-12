package com.ventasdiscos.msaccounts.config;

import com.ventasdiscos.msaccounts.service.EliminacionCuentaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Lote 5 · HU-78 / ERS 13.7 (decisión del equipo, mismo criterio que el reintento aprobado en el Lote 4): cada cierto
 * tiempo retoma las cuentas EN_ELIMINACION (servicio caído, token del Cliente expirado o proceso interrumpido),
 * vuelve a verificar y repite las depuraciones idempotentes. Usa la programación de tareas de Spring (sin
 * dependencias nuevas). Intervalo: accounts.eliminacion.reintento-ms (5 min). Se apaga con ELIMINACION_REINTENTO=false.
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "accounts.eliminacion.reintento-habilitado", havingValue = "true", matchIfMissing = true)
public class EliminacionReintentoConfig {

    private final EliminacionCuentaService eliminacionService;

    public EliminacionReintentoConfig(EliminacionCuentaService eliminacionService) {
        this.eliminacionService = eliminacionService;
    }

    @Scheduled(fixedDelayString = "${accounts.eliminacion.reintento-ms:300000}",
            initialDelayString = "${accounts.eliminacion.reintento-ms:300000}")
    public void reintentar() {
        try {
            int revisadas = eliminacionService.reintentarPendientes();
            if (revisadas > 0) {
                log.info("Reintento de eliminaciones: {} cuentas EN_ELIMINACION revisadas", revisadas);
            }
        } catch (RuntimeException e) {
            log.error("Falló el reintento de eliminaciones; se volverá a intentar más tarde", e);
        }
    }
}
