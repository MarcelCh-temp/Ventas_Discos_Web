package com.ventasdiscos.mspostsale.config;

import com.ventasdiscos.mspostsale.security.LlamadaInternaInterceptor;
import com.ventasdiscos.mspostsale.security.ServicioJwtService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Lote 4: clientes HTTP hacia los otros MS (igual que ms-ordenes y ms-soporte), con tiempos máximos de espera para
 * que PostSale no se quede colgado si otro MS no responde. Reemplaza al RestClient anterior: ambos son de Spring
 * Web (no es una tecnología nueva) y así los clientes y sus pruebas siguen el patrón ya aprobado en los Lotes 1 y 3.
 * Lote 6: cada RestTemplate lleva LlamadaInternaInterceptor, que agrega el JWT de servicio (DOC-05) a las rutas
 * /api/v1/internal/** y propaga X-Correlation-Id (DOC-06). Los clientes no cambian.
 */
@Configuration
public class RestTemplateConfig {

    /** Catalog y Notifications: respuestas rápidas. */
    @Bean
    @Primary
    // Ejecuta rest template como parte de las responsabilidades de esta clase.
    public RestTemplate restTemplate(ServicioJwtService servicioJwt) {
        return crear(Duration.ofSeconds(2), Duration.ofSeconds(3), servicioJwt);
    }

    /**
     * Orders: al aprobar una cancelación, Orders a su vez repone el stock en Catalog (hasta 2 intentos de ~3 s), así
     * que su respuesta puede tardar más. Con 3 s PostSale cortaría la llamada mientras Orders sigue trabajando.
     */
    @Bean
    @Qualifier("ordersRestTemplate")
    public RestTemplate ordersRestTemplate(ServicioJwtService servicioJwt) {
        return crear(Duration.ofSeconds(2), Duration.ofSeconds(12), servicioJwt);
    }

    // Ejecuta crear como parte de las responsabilidades de esta clase.
    private static RestTemplate crear(Duration conexion, Duration lectura, ServicioJwtService servicioJwt) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(conexion);
        factory.setReadTimeout(lectura);
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add(new LlamadaInternaInterceptor(servicioJwt));
        return restTemplate;
    }
}
