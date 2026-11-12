package ms_ordenes.ms_ordenes.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Lote 3 (copia de ms-soporte, Lote 1): botón "Authorize" en Swagger para pegar el token (igual que ms-accounts). */
@Configuration
@OpenAPIDefinition(info = @Info(title = "MS-Orders (ms-ordenes)", version = "v1"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
