package com.ventasdiscos.msaccounts.config;

import com.ventasdiscos.msaccounts.security.AuthUserArgumentResolver;
import com.ventasdiscos.msaccounts.security.ServicioInternoInterceptor;
import com.ventasdiscos.msaccounts.security.ServicioJwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registra el resolver de AuthUser y el prefijo de versión /api/v1 (misma convención que los MS de
 * referencia del curso).
 * Lote 6 (decisión 2/4): el CORS de este MS se DESHABILITA; ahora lo administra solo el API Gateway (8080),
 * para no duplicar cabeceras. Se agrega el interceptor del JWT de servicio (DOC-05).
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthUserArgumentResolver authUserArgumentResolver;

    private final ObjectProvider<ServicioJwtService> servicioJwt;

    /** Solo NUESTROS controllers llevan /api/v1 (Swagger y Actuator quedan en sus rutas normales). */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1", c -> c.isAnnotationPresent(RestController.class)
                && c.getPackageName().startsWith("com.ventasdiscos"));
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authUserArgumentResolver);
    }

    /**
     * Lote 6 · DOC-05: el interceptor revisa TODAS las peticiones y exige JWT de servicio en las rutas internas
     * (por URL, por controller /internal o por @ServiciosPermitidos). Se crea aquí (no como @Component) y recibe el
     * servicio con ObjectProvider: en pruebas @WebMvcTest, donde no existe ServicioJwtService, las rutas internas
     * quedan cerradas (401) en vez de romper el contexto.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ServicioInternoInterceptor(servicioJwt));
    }
}
