package com.ventasdiscos.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lote 6 · API Gateway (puerto 8080): punto de entrada ÚNICO del frontend (ERS 11.10, 12.1, 12.2).
 * Spring Cloud Gateway Server Web MVC (decisión 1/4). Las rutas hacia los 6 MS están en application.properties.
 * Las llamadas MS -> MS NO pasan por aquí y las rutas /api/v1/internal/** nunca se exponen.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    // Ejecuta main como parte de las responsabilidades de esta clase.
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
