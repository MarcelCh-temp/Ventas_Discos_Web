package com.ventasdiscos.msaccounts.config;

import com.ventasdiscos.msaccounts.security.LlamadaInternaInterceptor;
import com.ventasdiscos.msaccounts.security.ServicioJwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Lote 5 (HU-78): cliente HTTP hacia Orders, Support, PostSale, Catalog y Notifications, igual que en los demás MS,
 * con tiempos máximos de espera (ERS 12.7): si otro MS no responde, Accounts no se queda colgado.
 * Lote 6: cada RestTemplate lleva LlamadaInternaInterceptor, que agrega el JWT de servicio (DOC-05) a las rutas
 * /api/v1/internal/** y propaga X-Correlation-Id (DOC-06). Los clientes no cambian.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    // Ejecuta rest template como parte de las responsabilidades de esta clase.
    public RestTemplate restTemplate(ServicioJwtService servicioJwt) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add(new LlamadaInternaInterceptor(servicioJwt));
        return restTemplate;
    }
}
