package com.ventasdiscos.mscatalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * CAPA CONFIG: registra el RestTemplate como @Bean para llamar a otros MS (patrón del curso).
 * Diferencia con el ejemplo: lleva TIMEOUT. Sin él, si Inventory se cuelga, Catalog también
 * se quedaría esperando (prompt §12: timeouts y resiliencia).
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(@Value("${services.inventory.timeout-ms}") long timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return new RestTemplate(factory);
    }
}
