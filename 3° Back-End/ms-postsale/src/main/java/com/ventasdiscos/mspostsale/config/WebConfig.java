package com.ventasdiscos.mspostsale.config;

import com.ventasdiscos.mspostsale.security.AuthUserArgumentResolver;
import com.ventasdiscos.mspostsale.security.ServicioInternoInterceptor;
import com.ventasdiscos.mspostsale.security.ServicioJwtService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.ventasdiscos.mspostsale.client.AccountsClient;
import com.ventasdiscos.mspostsale.security.CuentaActivaInterceptor;
import com.ventasdiscos.mspostsale.security.JwtService;

import java.util.List;

/** Lote 4 (copia de ms-ordenes, Lote 3): registra el resolver que entrega el AuthUser (usuario del token) a los controllers.
 * Lote 6 · DOC-05: registra además el interceptor del JWT de servicio para las rutas internas. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthUserArgumentResolver authUserArgumentResolver;
    private final ObjectProvider<ServicioJwtService> servicioJwt;
    private final ObjectProvider<JwtService> jwtService;
    private final ObjectProvider<AccountsClient> accountsClient;

    public WebConfig(AuthUserArgumentResolver authUserArgumentResolver,
                     ObjectProvider<ServicioJwtService> servicioJwt,
                     ObjectProvider<JwtService> jwtService,
                     ObjectProvider<AccountsClient> accountsClient) {
        this.authUserArgumentResolver = authUserArgumentResolver;
        this.servicioJwt = servicioJwt;
        this.jwtService = jwtService;
        this.accountsClient = accountsClient;
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
        // Cierre (RN-44, RF-CTA-08 CA-06): después del JWT de servicio, la cuenta del CLIENTE debe estar ACTIVA
        registry.addInterceptor(new CuentaActivaInterceptor(jwtService, accountsClient));
    }
}
