package com.ventasdiscos.mspostsale.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Lote 4 (copia de ms-ordenes, Lote 3): botón "Authorize" en Swagger para pegar el token (igual que ms-accounts). */
@Configuration
@OpenAPIDefinition(info = @Info(title = "MS-PostSale (ms-postsale)", version = "v1"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
