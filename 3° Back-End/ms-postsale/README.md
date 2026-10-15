# MS-PostSale | Ventas Discos

Microservicio de postventa en Java 25, Spring Boot 4.0.8, JPA y MariaDB. Puerto 8085.

## Alcance y origen

Entidades y enumeraciones tomadas de `PAGINA WEB/Diagramas/MS_POSTSALE_UML.mmd`.
Operaciones de cancelación, devolución, recepción de productos y registro de solicitudes de reembolso basadas en EP-13, HU y reglas RN-18 a RN-26 del ERS.
No se procesan pagos ni reembolsos monetarios reales.

## Ejecución

Desde `3° Back-End/ms-postsale`:

```powershell
mvn clean compile
mvn spring-boot:run
```

Para Maven Wrapper, copiar los archivos `mvnw.cmd`, `mvnw` y `.mvn/` desde los otros microservicios del repositorio. Este ZIP incluye `pom.xml`, pero no el wrapper.

Base de datos MariaDB requerida: `bd_ms_postsale`; Hibernate crea/actualiza las tablas con `ddl-auto=update` una vez creada la base.
Swagger: http://localhost:8085/swagger-ui.html

## Dependencias reales

- Orders: GET `/api/ordenes/{id}`. **El Orders recibido no expone productos/cantidades ni confirmación de descuento de stock.** Por eso solicitud de devolución y aprobación de cancelación están bloqueadas con una respuesta explícita hasta que se amplíe el contrato real; no se falsifican validaciones RN-22/RN-26.
- Inventory: POST `/internal/inventory/operations`, contrato real provisional del MS de Marcel.
- Notifications: POST `/api/v1/internal/notifications/events` sin API key, según la última corrección de Notifications.

## Estado de la implementación

Incluye estructura de modelos, DTO, Controller, Repository, Service, client, excepciones y documentación de pruebas. Las recepciones parciales permiten más de un registro y validan cantidades acumuladas.

**Reposición de stock:** después de guardar una recepción, se realiza la operación en Inventory usando `operacionId` idempotente. El estado se confirma en PostSale solo al recibir respuesta favorable; si hay error queda PENDIENTE y se registra un error para conciliación manual. No existe aún un reintento automático, por lo que no debe interpretarse una operación pendiente como stock repuesto. El proyecto no se ha compilado aquí con Java 25 ni probado contra MariaDB.


## Revision interna del 15-10-2026

- Se validan atributos obligatorios y cantidades positivas en las entidades correspondientes al UML.
- Las recepciones usan bloqueo pesimista sobre la solicitud para verificar cantidades acumuladas en transacciones concurrentes.
- Los eventos a Notifications se intentan solo despues del commit local. Si el servicio receptor falla, el error queda en logs; **no hay reintento automatico persistente implementado**.
- La confirmacion de reposiciones en Inventory y las validaciones de Orders requieren pruebas con las versiones definitivas de esos microservicios.
- No se ha certificado compilacion con Java 25 ni pruebas funcionales integradas en este entorno.

## Consulta y filtrado de solicitudes (revision interna)

- `GET /post-sale/requests/me?usuarioId=1`: listar cancelaciones y devoluciones del cliente.
- `GET /post-sale/requests/{id}?usuarioId=1&tipo=CANCELACION`: detalle; `tipo=DEVOLUCION` cuando corresponda. Si hay dos solicitudes de distintos tipos con el mismo ID, se pide `tipo` para resolver la ambiguedad.
- `GET /admin/post-sale/requests`: listado con filtros opcionales `pedidoId`, `usuarioId`, `tipo` (`CANCELACION`/`DEVOLUCION`) y `estado`. Solo usa datos existentes en el modelo.

No se incorporaron reembolsos a los listados de cancelacion/devolucion, porque el backlog base los describe por separado.

**Estado de verificacion:** se corrigio el codigo fuente, pero todavia no se ha compilado ni ejecutado este ZIP en Java 25 ni se han probado los endpoints. No declarar conformidad al 100 % antes de esas pruebas y de disponer de los contratos definitivos de Orders.


## Organización del código

Las solicitudes se validan en DTO independientes dentro de `dto/`. La lógica de creación y resolución se distribuye entre `CancelacionService`, `DevolucionService` y `ReembolsoService`; `PostSaleService` conserva las consultas y coordina las llamadas de los Controllers. `PostSaleSupport` reúne únicamente operaciones compartidas ya existentes. Se conservan las rutas de la versión consolidada y el parámetro `tipo` del detalle.

**Pendientes previos no resueltos por esta reorganización:** la aprobación de cancelaciones y la validación completa de productos dependen del contrato real de Orders; hace falta compilar y ejecutar pruebas con Java 25 y MariaDB.
