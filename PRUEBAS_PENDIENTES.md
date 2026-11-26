# Ñackson Store — Estado de validación

Fecha de actualización: 26-11-2026.

## Verificaciones realizadas

- [x] Compilación TypeScript y Vite.
- [x] 101 módulos transformados correctamente.
- [x] Suite frontend: 98/98 pruebas aprobadas.
- [x] Ocho archivos de pruebas aprobados.
- [x] Corrección del entorno de pruebas de PageFrame.
- [x] Arranque previo de los seis microservicios.
- [x] Arranque previo del API Gateway.
- [x] Conexión local con MariaDB/XAMPP.
- [x] Redirección de visitantes sin sesión desde rutas protegidas.
- [x] Acceso al panel con una cuenta de administrador.
- [x] Persistencia de la sesión administrativa al recargar.
- [x] Compilación y pruebas después de la limpieza del frontend.

## Backend y bases de datos

- [x] Compilación limpia con Maven y Java 25.
- [x] Ejecutar suites de los seis microservicios y Gateway.
- [ ] Probar los scripts SQL en una instalación controlada.
- [ ] Verificar los seis esquemas y relaciones.
- [ ] Revisar compatibilidad de solicitudes históricas de Postventa.
- [ ] Validar instalación desde una copia limpia del repositorio.

## Seguridad

- [ ] Verificar acceso directo a endpoints sin JWT.
- [ ] Probar JWT inválido, expirado y con emisor incorrecto.
- [ ] Confirmar que un cliente autenticado no accede a administración.
- [ ] Confirmar que un administrador no ejecuta compras como cliente.
- [ ] Comprobar protección de datos pertenecientes a otros usuarios.
- [ ] Revisar persistencia de la sesión de cliente.
- [ ] Revisar y rotar credenciales históricas expuestas.

## Funcionalidades

- [ ] Registro y validación del RUT mediante módulo 11.
- [ ] Inicio y cierre de sesión.
- [ ] Catálogo, búsqueda, filtros y favoritos.
- [ ] Carrito invitado y transferencia al iniciar sesión.
- [ ] Confirmar regla de cantidades al fusionar carritos.
- [ ] Persistencia del carrito.
- [ ] Compra con pago simulado aprobado y rechazado.
- [ ] Actualización y protección del stock.
- [ ] Promociones, descuentos y ruleta.
- [ ] Método de pago y detalle histórico del pedido.
- [ ] Despacho y seguimiento.
- [ ] Notificaciones.
- [ ] Soporte.
- [ ] Cancelación, devolución y reembolso.
- [ ] Registro de recepción y responsable administrativo.

## Interfaz

- [ ] Validación completa en escritorio y dispositivos móviles.
- [ ] Temas, colores, opacidad y brillo.
- [ ] Persistencia de preferencias.
- [ ] Continuidad de la radio durante la navegación.
- [ ] Disponibilidad de fuentes musicales externas.
- [ ] Legibilidad y accesibilidad.

## Documentación académica

- [ ] Contrastar observaciones originales del profesor.
- [ ] Actualizar diagramas UML correspondientes.
- [ ] Revisar ERS.
- [ ] Revisar documentación Scrum.
- [ ] Verificar cumplimiento de la rúbrica.

Las 98 pruebas aprobadas corresponden a la suite del
frontend y no certifican por sí solas la integración
completa con los microservicios.


## Resultados finales de pruebas automatizadas

Fecha: 26-11-2026.

| Proyecto | Aprobadas | Fallos | Errores |
|---|---:|---:|---:|
| Frontend | 98 | 0 | 0 |
| API Gateway | 119 | 0 | 0 |
| Accounts | 132 | 0 | 0 |
| Catalog | 216 | 0 | 0 |
| Ordenes | 125 | 0 | 0 |
| Notifications | 51 | 0 | 0 |
| PostSale | 111 | 0 | 0 |
| Soporte | 99 | 0 | 0 |
| TOTAL | 951 | 0 | 0 |

Backend: 853 pruebas aprobadas.
Frontend: 98 pruebas aprobadas.
Los siete proyectos Java finalizaron con BUILD SUCCESS.
No hubo pruebas omitidas.

Quedan pendientes las pruebas funcionales integrales,
la validacion de migraciones en otras instalaciones
y la revision de la documentacion academica.
