# Swagger de MS-PostSale

Abrir http://localhost:8085/swagger-ui.html después de iniciar MariaDB y Spring Boot.

## 1. POST — CREATE: solicitar cancelación
`POST /post-sale/cancellations`
```json
{"pedidoId":1,"usuarioId":1,"motivoSolicitud":"Quiero cancelar el pedido"}
```
Respuesta: 201 si Orders confirma pertenencia y estado cancelable.

## 2. POST — CREATE: solicitar devolución
`POST /post-sale/returns`
```json
{"pedidoId":1,"usuarioId":1,"motivoSolicitud":"Producto no deseado","items":[{"productoId":1,"cantidadSolicitada":1}]}
```
**Actualmente no completable**: Orders debe exponer productos y cantidades para validar RN-22; si no los expone, 503 explícito.

## 3. GET — READ: mis solicitudes
`GET /post-sale/requests/me?usuarioId=1`

## 4. GET — READ: detalle de solicitud
`GET /post-sale/requests/1?usuarioId=1`

## 5. GET — READ: solicitudes para administración
`GET /admin/post-sale/requests`

## 6. POST — UPDATE: resolver cancelación
`POST /admin/post-sale/cancellations/1/resolve`
```json
{"aprobar":false,"motivo":"Solicitud rechazada"}
```
Aprobación bloqueada mientras Orders no exponga estado de descuento e ítems para RN-26.

## 7. POST — UPDATE: resolver devolución
`POST /admin/post-sale/returns/1/resolve`
```json
{"aprobar":true,"motivo":"Solicitud aprobada"}
```

## 8. POST — CREATE: registrar recepción
`POST /admin/post-sale/returns/1/reception`
```json
{"observaciones":"Recepcion parcial","items":[{"itemDevolucionId":1,"cantidadRecibida":1,"condicion":"BUENA"}]}
```
La recepción se registra y valida sumas acumuladas. La reposición se intenta después del commit local y su confirmación debe verificarse en Inventory.

## 9. POST — CREATE: solicitud de reembolso
`POST /post-sale/refunds`
```json
{"usuarioId":1,"tipoOrigen":"DEVOLUCION","solicitudOrigenId":1}
```
Solo registra una solicitud; no mueve dinero.

**Todas las respuestas deben comprobarse ejecutando el servicio.**
