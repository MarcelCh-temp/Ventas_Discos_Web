
# MS-Notifications | Pruebas con Swagger

## Iniciar el microservicio

Ejecutar desde ms-notifications:

    .\mvnw.cmd spring-boot:run

Abrir Swagger:

http://localhost:8086/swagger-ui.html

Seleccionar el endpoint, presionar Try it out,
ingresar los datos y presionar Execute.

---

## 1. POST - CREATE
### Registrar una notificación

**Endpoint:**

POST /api/v1/internal/notifications/events

**URL completa:**

http://localhost:8086/api/v1/internal/notifications/events

**Request Body:**

```json
{
  "eventoId": "pedido-001-creado",
  "origen": "ORDERS",
  "usuarioId": 1,
  "titulo": "Pedido creado",
  "mensaje": "Tu pedido fue registrado correctamente",
  "tipo": "PEDIDO_CREADO",
  "referenciaId": 10
}
```

**Respuesta esperada: HTTP 201 Created**

```json
{
  "processed": true,
  "message": "Notificacion registrada"
}
```

**Comprobar:** se registra la notificación y el evento procesado.

---

## 2. POST - CREATE
### Comprobar un evento duplicado

**Endpoint:**

POST /api/v1/internal/notifications/events

**URL completa:**

http://localhost:8086/api/v1/internal/notifications/events

**Request Body:**

Repetir el JSON de la prueba 1 con el mismo
eventoId y origen.

**Respuesta esperada: HTTP 200 OK**

```json
{
  "processed": false,
  "message": "Evento procesado anteriormente"
}
```

**Comprobar:** no se crea una segunda notificación.

---

## 3. POST - CREATE
### Validar datos incorrectos

**Endpoint:**

POST /api/v1/internal/notifications/events

**URL completa:**

http://localhost:8086/api/v1/internal/notifications/events

**Request Body:**

```json
{
  "eventoId": "",
  "origen": "ORDERS",
  "usuarioId": -1,
  "titulo": "",
  "mensaje": "Prueba de validacion",
  "tipo": "PEDIDO_CREADO",
  "referenciaId": 10
}
```

**Respuesta esperada: HTTP 400 Bad Request**

**Comprobar:** la solicitud se rechaza.

---

## 4. GET - READ
### Consultar notificaciones de un usuario

**Endpoint:**

GET /api/v1/notifications/me

**URL completa:**

http://localhost:8086/api/v1/notifications/me?usuarioId=1

**Parámetro:**

usuarioId = 1

**Request Body:** ninguno.

**Respuesta esperada: HTTP 200 OK**

Ejemplo de una notificación:

```json
[
  {
    "id": 1,
    "usuarioId": 1,
    "titulo": "Pedido creado",
    "mensaje": "Tu pedido fue registrado correctamente",
    "tipo": "PEDIDO_CREADO",
    "fechaCreacion": "2026-10-15T08:30:00",
    "leida": false,
    "referenciaId": 10
  }
]
```

**Comprobar:** se muestran las notificaciones
correspondientes al usuarioId indicado.

---

## 5. GET - READ
### Filtrar notificaciones por tipo

**Endpoint:**

GET /api/v1/notifications/me

**URL completa:**

http://localhost:8086/api/v1/notifications/me?usuarioId=1&type=PEDIDO_CREADO

**Parámetros:**

usuarioId = 1

type = PEDIDO_CREADO

**Request Body:** ninguno.

**Respuesta esperada: HTTP 200 OK**

**Comprobar:** los resultados corresponden
al tipo PEDIDO_CREADO.

---

## 6. GET - READ
### Filtrar notificaciones por fechas

**Endpoint:**

GET /api/v1/notifications/me

**URL completa:**

http://localhost:8086/api/v1/notifications/me?usuarioId=1&from=2026-10-01T00:00:00&to=2026-10-31T23:59:59

**Parámetros:**

usuarioId = 1

from = 2026-10-01T00:00:00

to = 2026-10-31T23:59:59

**Request Body:** ninguno.

**Respuesta esperada: HTTP 200 OK**

**Comprobar:** las fechas pertenecen
al intervalo solicitado.

---

## 7. GET - READ
### Filtrar por tipo y fechas

**Endpoint:**

GET /api/v1/notifications/me

**URL completa:**

http://localhost:8086/api/v1/notifications/me?usuarioId=1&type=PEDIDO_CREADO&from=2026-10-01T00:00:00&to=2026-10-31T23:59:59

**Parámetros:**

usuarioId = 1

type = PEDIDO_CREADO

from = 2026-10-01T00:00:00

to = 2026-10-31T23:59:59

**Request Body:** ninguno.

**Respuesta esperada: HTTP 200 OK**

**Comprobar:** los resultados cumplen
los filtros indicados.

---

## 8. GET - READ
### Contar notificaciones no leídas

**Endpoint:**

GET /api/v1/notifications/me/unread-count

**URL completa:**

http://localhost:8086/api/v1/notifications/me/unread-count?usuarioId=1

**Parámetro:**

usuarioId = 1

**Request Body:** ninguno.

**Respuesta esperada: HTTP 200 OK**

```json
{
  "unreadCount": 1
}
```

**Comprobar:** la cantidad coincide con
las notificaciones no leídas.

---

## 9. PATCH - UPDATE
### Marcar una notificación como leída

**Endpoint:**

PATCH /api/v1/notifications/{id}/read

**URL de ejemplo:**

http://localhost:8086/api/v1/notifications/1/read?usuarioId=1

**Parámetros:**

id = 1

usuarioId = 1

**Request Body:** ninguno.

**Respuesta esperada: HTTP 204 No Content**

**Comprobar:** la notificación conserva
su registro y leida cambia a true.

---

## 10. PATCH - UPDATE
### Marcar todas las notificaciones como leídas

**Endpoint:**

PATCH /api/v1/notifications/read-all

**URL completa:**

http://localhost:8086/api/v1/notifications/read-all?usuarioId=1

**Parámetro:**

usuarioId = 1

**Request Body:** ninguno.

**Respuesta esperada: HTTP 200 OK**

```json
{
  "updatedCount": 1
}
```

**Comprobar:** las notificaciones pendientes
quedan marcadas como leídas.

---

## Comprobación en MariaDB

Consultar las notificaciones:

```sql
SELECT *
FROM notificaciones
ORDER BY fecha_creacion DESC;
```

Consultar los eventos registrados:

```sql
SELECT *
FROM eventos_notificacion_procesados;
```

Consultar las notificaciones no leídas:

```sql
SELECT *
FROM notificaciones
WHERE usuario_id = 1
AND leida = 0;
```
