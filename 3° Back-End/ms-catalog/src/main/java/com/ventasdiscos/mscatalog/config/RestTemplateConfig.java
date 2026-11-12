package com.ventasdiscos.mscatalog.config;

import com.ventasdiscos.mscatalog.security.LlamadaInternaInterceptor;
import com.ventasdiscos.mscatalog.security.ServicioJwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Lote 5 (integración de HU-78): Catalog vuelve a necesitar un cliente HTTP, solo para preguntar a MS-Accounts si la
 * cuenta puede agregar favoritos (RN-37, RN-44). Igual que en los demás MS, con tiempos máximos de espera.
 * Lote 6: cada RestTemplate lleva LlamadaInternaInterceptor, que agrega el JWT de servicio (DOC-05) a las rutas
 * /api/v1/internal/** y propaga X-Correlation-Id (DOC-06). Los clientes no cambian.
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
