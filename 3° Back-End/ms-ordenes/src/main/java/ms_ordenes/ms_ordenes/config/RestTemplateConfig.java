package ms_ordenes.ms_ordenes.config;

import ms_ordenes.ms_ordenes.security.LlamadaInternaInterceptor;
import ms_ordenes.ms_ordenes.security.ServicioJwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Lote 3: cliente HTTP hacia Catalog y Notifications (igual que ms-soporte), con tiempos máximos de espera:
 * si otro MS no responde, Orders no se queda colgado.
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
