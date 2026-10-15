
# Validaciones de MS-PostSale

## 1. Objetivo

Documentar las validaciones de entrada,
persistencia y reglas de negocio de MS-PostSale.

Las validaciones buscan mantener la integridad
de las solicitudes de cancelación, devolución,
recepción y reembolso.

---

## 2. Validaciones en DTO

Los DTO se encuentran separados en archivos
individuales dentro de la carpeta dto.

### Anotaciones utilizadas

- @NotNull: impide valores nulos obligatorios.
- @NotBlank: impide textos vacíos.
- @NotEmpty: impide listas vacías.
- @Positive: exige valores numéricos positivos.
- @Valid: activa la validación de objetos anidados.

Los Controllers utilizan @Valid y @Validated
para procesar las validaciones de entrada.

### Ejemplos

- pedidoId y usuarioId deben ser válidos.
- motivoSolicitud no debe estar vacío.
- cantidadSolicitada debe ser positiva.
- cantidadRecibida debe ser positiva.
- Las listas obligatorias no deben estar vacías.
- Los elementos anidados también deben validarse.

---

## 3. Validaciones en Model

Las entidades JPA declaran campos obligatorios,
relaciones y restricciones de persistencia.

Se emplean anotaciones como:

- @NotNull
- @NotBlank
- @Positive
- @Column(nullable = false)
- @JoinColumn(nullable = false)

Los identificadores generados por la base de datos
no requieren un valor previo a su creación.

Los campos de resolución pueden permanecer vacíos
hasta que se tome una decisión administrativa.

Las fechas de creación se asignan cuando corresponde
mediante los mecanismos definidos en las entidades.

---

## 4. Reglas de negocio

### Cancelaciones

- Validar la existencia del pedido.
- Verificar la pertenencia al usuario.
- Validar que el estado permita cancelación.
- Evitar solicitudes activas duplicadas cuando
  corresponda según las reglas del ERS.
- Registrar motivo y estado de la solicitud.
- Registrar la resolución administrativa.

### Devoluciones

- Verificar que el pedido sea elegible.
- Comprobar que los productos fueron comprados.
- Validar cantidades solicitadas.
- Evitar solicitudes incompatibles o duplicadas
  conforme a las reglas acordadas.
- Registrar la resolución administrativa.

### Recepciones

- Registrar los productos recibidos.
- Guardar las cantidades y condiciones.
- Controlar las cantidades acumuladas.
- Evitar recepciones superiores a lo solicitado.
- Reponer stock únicamente cuando corresponde.

### Reembolsos

- Vincular el reembolso con una solicitud válida.
- Registrar su estado y trazabilidad.
- No realizar movimientos monetarios reales
  dentro de MS-PostSale.

---

## 5. Integración con otros microservicios

### MS-Orders

Permite verificar datos y estados de pedidos.

Las validaciones que requieren información
de productos y cantidades dependen de que
Orders exponga los datos correspondientes.

### MS-Inventory

Interviene cuando corresponde realizar
una reposición de stock.

La actualización debe verificarse en una
prueba de integración.

### MS-Notifications

Recibe los eventos notificables generados
por los cambios de las solicitudes.

La entrega y registro de los eventos deben
comprobarse mediante pruebas.

---

## 6. Base de datos

Base de datos: bd_ms_postsale

Motor local: MariaDB 10.4.32

Persistencia: Spring Data JPA / Hibernate

Se verificó:

- Conexión a MariaDB.
- Generación automática de tablas.
- Registro de ocho claves foráneas.
- Inicialización de JPA.

Se utiliza un dialecto adaptado para la
compatibilidad con MariaDB 10.4.

---

## 7. Estado de las validaciones

### Verificado

- Compilación Java.
- Inicialización de Spring Boot.
- Conexión y estructura relacional de MariaDB.
- Exposición de endpoints mediante Swagger.

### Pendiente de comprobar

- Respuestas HTTP ante datos incorrectos.
- Comportamiento de las reglas de negocio.
- Integración real con Orders.
- Actualización de stock en Inventory.
- Notificaciones automáticas.
- Conformidad integral con ERS, HU y UML.

---

## 8. Observaciones

Este documento describe las validaciones
previstas y la estructura utilizada.

No sustituye las pruebas funcionales ni
la revisión del ERS del proyecto.

No se agregan reglas nuevas sin confirmación
de los requerimientos vigentes.
