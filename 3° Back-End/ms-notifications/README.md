
# MS-Notifications | Ventas Discos

## Descripción

MS-Notifications es el microservicio encargado de gestionar las notificaciones de los usuarios de la plataforma Ventas Discos.

Permite recibir eventos de otros microservicios, registrar notificaciones, consultarlas, filtrarlas y actualizar su estado de lectura.

## Tecnologías

- Java 25
- Spring Boot 4.0.8
- Spring Data JPA
- Hibernate
- MariaDB
- Maven
- Lombok
- Jakarta Bean Validation
- Swagger / OpenAPI

## Arquitectura

El proyecto está organizado en las siguientes capas:

- Controller: endpoints REST.
- Service: lógica de negocio.
- Repository: acceso a datos.
- Model: entidades JPA y enumeraciones.
- DTO: solicitudes y respuestas.
- Exception: manejo centralizado de errores.

## Base de datos

Nombre: bd_ms_notifications

Tablas:

- notificaciones
- eventos_notificacion_procesados

La segunda tabla permite registrar los eventos procesados y evitar duplicados mediante una restricción única sobre origen y evento_id.

La propiedad spring.jpa.hibernate.ddl-auto=update permite crear o actualizar las tablas al iniciar Spring Boot.

También existe el archivo:

db/db_ms_notifications.sql

## Configuración

Archivo: src/main/resources/application.properties

Puerto: 8086

Variables de conexión disponibles:

- DB_URL
- DB_USERNAME
- DB_PASSWORD

## Ejecución

Desde la carpeta ms-notifications:

    .\mvnw.cmd clean compile
    .\mvnw.cmd spring-boot:run

## Endpoints

### POST - Crear notificación desde un evento

POST /api/v1/internal/notifications/events

Recibe los datos de un evento y registra la notificación correspondiente.

### GET - Consultar notificaciones

GET /api/v1/notifications/me?usuarioId=1

Obtiene las notificaciones asociadas al usuario indicado.

Filtros opcionales:

- type
- from
- to

### GET - Contar notificaciones no leídas

GET /api/v1/notifications/me/unread-count?usuarioId=1

### PATCH - Marcar una notificación como leída

PATCH /api/v1/notifications/1/read?usuarioId=1

### PATCH - Marcar todas como leídas

PATCH /api/v1/notifications/read-all?usuarioId=1

No se implementaron operaciones PUT ni DELETE.

## Tipos de notificación

- PEDIDO_CREADO
- PEDIDO_ACTUALIZADO
- SOPORTE_CREADO
- SOPORTE_RESPONDIDO
- SOPORTE_CERRADO
- CANCELACION_CREADA
- CANCELACION_RESUELTA
- DEVOLUCION_CREADA
- DEVOLUCION_RESUELTA
- REEMBOLSO_REGISTRADO

## Validaciones

Se utilizan las validaciones de Jakarta Bean Validation y las validaciones de negocio implementadas en los servicios.

## Swagger

Swagger UI:

http://localhost:8086/swagger-ui.html

OpenAPI:

http://localhost:8086/v3/api-docs

## Documentación de pruebas

- docs/ejemplos-notifications.json
- docs/SWAGGER_EJEMPLOS.md

Los documentos contienen solicitudes de ejemplo y resultados esperados para comprobar el funcionamiento del microservicio.
