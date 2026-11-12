package com.ventasdiscos.msnotifications.config;

import com.ventasdiscos.msnotifications.security.LlamadaInternaInterceptor;
import com.ventasdiscos.msnotifications.security.ServicioJwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Cierre (RN-44): cliente HTTP hacia MS-Accounts, igual que en los demás MS: tiempos máximos de espera (ERS 12.7) y
 * LlamadaInternaInterceptor, que agrega el JWT de servicio (DOC-05) y propaga X-Correlation-Id (DOC-06).
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(ServicioJwtService servicioJwt) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add(new LlamadaInternaInterceptor(servicioJwt));
        return restTemplate;
    }
}
