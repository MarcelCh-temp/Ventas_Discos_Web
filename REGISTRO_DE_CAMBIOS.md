# Registro de cambios — candidato de integración

- Se preservan seis microservicios, Gateway, estructura CSR, Java/Spring y React/TypeScript, dependencias y documentos académicos existentes.
- Separación de accesos en Gateway, Controllers y React; validación de emisor, firma HS256, rol y usuario en JWT de usuario; sesiones y navegación por rol. Los helpers de prueba de los MS afectados ahora emiten tokens con el emisor real.
- Método de pago simulado persistido en Orden y DTO de detalle; visualización cliente/admin y datos de entrega, descuento, ruleta, envío e historial; pedidos antiguos sin dato se muestran sin inventarlo.
- SQL aditivo para detalle, diagnóstico y preparación de PostSale, reparación de Soporte y verificación de BD. Configuración parametrizada sin migrar ni cambiar motor automáticamente.
- Carrito invitado temporal, transferencia explícita y contador; se eligió el máximo de las cantidades en conflicto para evitar duplicar artículos, pendiente de confirmar esta regla con el equipo.
- Notificaciones conectadas a API, navegación y protección de vistas.
- Identidad base #9FE7F5 / #429EBD / #053F5C / #F7AD19 / #F27F0C; temas propios, selector de colores y opacidad, persistencia local, modo claro/oscuro, menú lateral y estilos adaptables. No es una réplica pixel a pixel de la referencia; requiere revisión en navegador.
- Radio opcional existente reutilizada, controles y continuidad de audio local en navegación; disponibilidad de fuentes externas no comprobada.

## Alcance de verificación

Actualización del 26-11-2026:

- Compilación exitosa del frontend mediante TypeScript y Vite.
- 101 módulos transformados.
- 98 de 98 pruebas automatizadas aprobadas.
- Arranque previo de los seis microservicios y API Gateway.
- Conexión local con MariaDB 10.4.32 mediante XAMPP.
- Acceso administrativo y persistencia de sesión comprobados.
- Redirección de visitantes sin sesión desde rutas protegidas.
- Pendientes las pruebas completas de backend e integración.
- No se declara cumplimiento integral de la rúbrica.

## Limpieza e integración

- Retiro y respaldo externo de 79 archivos .bak.
- Retiro de ocho ZIP temporales.
- Retiro de tres archivos de código obsoleto.
- Retiro de cuatro duplicados de la raíz del frontend.
- Total: 94 archivos retirados.
- Creación de .gitignore general.
- Retiro de seis archivos .env y ocho registros .log del índice.
- Conservación de las configuraciones locales.
- Corrección del entorno de pruebas de PageFrame.
- Incorporación del nuevo diseño global NacksonShell.
- Incorporación de MIGRACION_ESTRUCTURA_POSTSALE.sql.
- Conservación de tablas históricas de Postventa.
- Actualización de instrucciones de ejecución.

## Decisiones y limitaciones

- Se conserva la arquitectura original del equipo.
- No se cambió el motor de base de datos.
- La regla de cantidad máxima al fusionar carritos
  permanece pendiente de aprobación del equipo.
- La nueva migración estructural requiere validación.
- Las credenciales históricas requieren revisión y rotación.
- No se reescribió el historial de Git.
- Los documentos académicos todavía no fueron actualizados.


## Archivos modificados o nuevos
- `2° Front-End/src/App.tsx`
- `2° Front-End/src/components/CarritoLateral.tsx`
- `2° Front-End/src/components/GuestCart.tsx`
- `2° Front-End/src/components/MenuMas.tsx`
- `2° Front-End/src/components/ProtectedRoute.tsx`
- `2° Front-End/src/components/RadioYRegalo.tsx`
- `2° Front-End/src/components/RealCatalog.tsx`
- `2° Front-End/src/components/Shortcuts.tsx`
- `2° Front-End/src/components/StoreLayout.tsx`
- `2° Front-End/src/components/TemaNackson.tsx`
- `2° Front-End/src/main.tsx`
- `2° Front-End/src/pages/IndexPage.tsx`
- `2° Front-End/src/pages/LoginPage.tsx`
- `2° Front-End/src/pages/NotificacionesPage.tsx`
- `2° Front-End/src/pages/PedidosPage.tsx`
- `2° Front-End/src/pages/ProductoPage.tsx`
- `2° Front-End/src/pages/RealAdminPage.tsx`
- `2° Front-End/src/pages/RealCarritoPage.tsx`
- `2° Front-End/src/pages/RealCheckoutPage.tsx`
- `2° Front-End/src/services/http.ts`
- `2° Front-End/src/services/storeApi.ts`
- `2° Front-End/src/state/AuthContext.tsx`
- `2° Front-End/src/state/guestCart.ts`
- `2° Front-End/src/styles/nackson-identidad.css`
- `3° Back-End/api-gateway/src/main/java/com/ventasdiscos/apigateway/seguridad/ReglasAcceso.java`
- `3° Back-End/api-gateway/src/main/java/com/ventasdiscos/apigateway/seguridad/SeguridadGatewayFilter.java`
- `3° Back-End/api-gateway/src/test/java/com/ventasdiscos/apigateway/seguridad/ReglasAccesoTest.java`
- `3° Back-End/ms-accounts/.env.example`
- `3° Back-End/ms-accounts/src/main/java/com/ventasdiscos/msaccounts/security/JwtService.java`
- `3° Back-End/ms-accounts/src/main/resources/application.properties`
- `3° Back-End/ms-catalog/.env.example`
- `3° Back-End/ms-catalog/src/main/java/com/ventasdiscos/mscatalog/controller/FavoritoController.java`
- `3° Back-End/ms-catalog/src/main/java/com/ventasdiscos/mscatalog/security/AuthUser.java`
- `3° Back-End/ms-catalog/src/main/java/com/ventasdiscos/mscatalog/security/JwtService.java`
- `3° Back-End/ms-catalog/src/main/resources/application.properties`
- `3° Back-End/ms-notifications/.env.example`
- `3° Back-End/ms-notifications/src/main/java/com/ventasdiscos/msnotifications/security/JwtService.java`
- `3° Back-End/ms-notifications/src/main/resources/application.properties`
- `3° Back-End/ms-notifications/src/test/java/com/ventasdiscos/msnotifications/TestTokens.java`
- `3° Back-End/ms-ordenes/.env.example`
- `3° Back-End/ms-ordenes/RULETA_SETUP.sql`
- `3° Back-End/ms-ordenes/src/main/java/ms_ordenes/ms_ordenes/controller/CarritoController.java`
- `3° Back-End/ms-ordenes/src/main/java/ms_ordenes/ms_ordenes/controller/CheckoutController.java`
- `3° Back-End/ms-ordenes/src/main/java/ms_ordenes/ms_ordenes/controller/OrdenController.java`
- `3° Back-End/ms-ordenes/src/main/java/ms_ordenes/ms_ordenes/controller/RuletaController.java`
- `3° Back-End/ms-ordenes/src/main/java/ms_ordenes/ms_ordenes/dto/PedidoDetalleResponse.java`
- `3° Back-End/ms-ordenes/src/main/java/ms_ordenes/ms_ordenes/model/MetodoPago.java`
- `3° Back-End/ms-ordenes/src/main/java/ms_ordenes/ms_ordenes/model/Orden.java`
- `3° Back-End/ms-ordenes/src/main/java/ms_ordenes/ms_ordenes/security/AuthUser.java`
- `3° Back-End/ms-ordenes/src/main/java/ms_ordenes/ms_ordenes/security/JwtService.java`
- `3° Back-End/ms-ordenes/src/main/java/ms_ordenes/ms_ordenes/service/OrdenService.java`
- `3° Back-End/ms-ordenes/src/main/resources/application.properties`
- `3° Back-End/ms-ordenes/src/test/java/ms_ordenes/ms_ordenes/OrdenesITTest.java`
- `3° Back-End/ms-ordenes/src/test/java/ms_ordenes/ms_ordenes/TestTokens.java`
- `3° Back-End/ms-postsale/.env.example`
- `3° Back-End/ms-postsale/src/main/java/com/ventasdiscos/mspostsale/controller/PostSaleController.java`
- `3° Back-End/ms-postsale/src/main/java/com/ventasdiscos/mspostsale/security/AuthUser.java`
- `3° Back-End/ms-postsale/src/main/java/com/ventasdiscos/mspostsale/security/JwtService.java`
- `3° Back-End/ms-postsale/src/main/resources/application.properties`
- `3° Back-End/ms-postsale/src/test/java/com/ventasdiscos/mspostsale/TestTokens.java`
- `3° Back-End/ms-soporte/.env.example`
- `3° Back-End/ms-soporte/src/main/java/ms_soporte/ms_soporte/controller/TicketController.java`
- `3° Back-End/ms-soporte/src/main/java/ms_soporte/ms_soporte/security/AuthUser.java`
- `3° Back-End/ms-soporte/src/main/java/ms_soporte/ms_soporte/security/JwtService.java`
- `3° Back-End/ms-soporte/src/main/resources/application.properties`
- `3° Back-End/ms-soporte/src/test/java/ms_soporte/ms_soporte/TestTokens.java`
- `Bases-De-Datos/DIAGNOSTICO_POSTSALE.sql`
- `Bases-De-Datos/MIGRACION_DETALLE_PEDIDO.sql`
- `Bases-De-Datos/PREPARAR_RELACIONES_POSTSALE.sql`
- `Bases-De-Datos/REPARAR_RELACIONES_SOPORTE.sql`
- `Bases-De-Datos/VERIFICAR_SEIS_BD.sql`
- `Bases-De-Datos/bd_ordenes.sql`
- `INSTRUCCIONES_DE_EJECUCION.md`
- `PRUEBAS_PENDIENTES.md`
- `REGISTRO_DE_CAMBIOS.md`

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
