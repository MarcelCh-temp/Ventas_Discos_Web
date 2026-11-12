# api-gateway (Lote 6)

Punto de entrada **único** del frontend, en el puerto **8080** (ERS 11.10, 12.1, 12.2). Spring Boot 4.1.0 + Spring Cloud 2025.1.2 (**Spring Cloud Gateway Server Web MVC**). Sin WebFlux, Eureka ni Kafka.

| Qué hace | Dónde |
|---|---|
| Rutas hacia los 6 microservicios | `src/main/resources/application.properties` (`spring.cloud.gateway.server.webmvc.routes`) |
| Nunca expone `/api/v1/internal/**` (404) y clasifica rutas públicas / con sesión / administrativas | `seguridad/ReglasAcceso` |
| Valida el JWT de usuario (firma, expiración, emisor `ms-accounts`, rol) y exige `ADMINISTRADOR` en las rutas administrativas | `seguridad/JwtUsuarioValidator`, `seguridad/SeguridadGatewayFilter` |
| CORS solo aquí, con orígenes exactos (`CORS_ORIGINS`, sin `*`) | `config/CorsGatewayFilter` |
| Genera o conserva `X-Correlation-Id` y lo reenvía al MS (DOC-06) | `trazabilidad/CorrelationIdFilter` |
| Errores propios en formato `ApiError` con `correlationId` (400, 401, 403, 404, 502, 503, 504) | `error/ApiErrorJson`, `error/FalloReenvioFilter` |
| Tiempos máximos hacia los MS (ERS 12.7): conexión 2 s, lectura 30 s; sin reintentos | `spring.http.clients.*` en `application.properties` (`GATEWAY_CONNECT_TIMEOUT`, `GATEWAY_READ_TIMEOUT`) |

El Gateway es la **primera capa**: cada microservicio vuelve a validar el token, el rol, la propiedad del recurso y el estado de la cuenta.

Configuración: `.env.example` y `../seguimiento/INSTRUCCIONES_CONFIGURACION_LOTE_6.md`. Contratos: `../seguimiento/CONTRATOS_API_LOTE_6.md`.

**Estado: IMPLEMENTADO · SIN EJECUTAR.**
