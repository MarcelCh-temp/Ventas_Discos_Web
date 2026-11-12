package ms_soporte.ms_soporte.config;

import ms_soporte.ms_soporte.security.LlamadaInternaInterceptor;
import ms_soporte.ms_soporte.security.ServicioJwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Lote 6: cada RestTemplate lleva LlamadaInternaInterceptor, que agrega el JWT de servicio (DOC-05) a las rutas
 * /api/v1/internal/** y propaga X-Correlation-Id (DOC-06). Los clientes no cambian.
 */
@Configuration
public class RestTemplateConfig {

    // Lote 1: este bean ya existía. Se agregan tiempos máximos de espera (igual que ms-catalog):
    // si MS-Notifications no responde, Soporte no se queda colgado.
    @Bean
    public RestTemplate restTemplate(ServicioJwtService servicioJwt) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add(new LlamadaInternaInterceptor(servicioJwt));
        return restTemplate;
    }
}
