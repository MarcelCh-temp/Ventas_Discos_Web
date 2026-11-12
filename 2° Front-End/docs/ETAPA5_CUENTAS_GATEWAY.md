# Ñackson Store — Etapa 5 (fase 1): Accounts + Gateway

**Base:** Etapa 4 estable. Se conserva la carpeta `public/`, estilos, rutas del catálogo, carrito local y checkout de demostración. El backend no se incluye ni se modifica.

## Fuente de verdad

Cotejado con `ms-accounts` y `api-gateway` de `Ventas_Discos_Web-main_Lotes1-6_Cierre_MySQL_Vite_Configurado.zip`, bajo `3° Back-End`. Rutas efectivas `/api/v1` por `http://localhost:8080`, no se llaman puertos internos del MS desde React.

| Operación | Método y ruta | Cuerpo / respuesta |
|---|---|---|
| Registro | POST `/api/v1/accounts/register` | `RegistroRequest` (RUT, nombres, apellidos, correo y confirmaciones) → `PerfilResponse` 201 |
| Login | POST `/api/v1/auth/login` | correo, password → `LoginResponse` (token, tipo, expiración, userId, nombres, rol) |
| Perfil | GET `/api/v1/accounts/me` | JWT → `PerfilResponse` |
| Editar perfil | PATCH `/api/v1/accounts/me` | Campos cambiados nombres/apellidos/correo → `PerfilResponse` |
| Cambiar contraseña | PUT `/api/v1/accounts/me/password` | Contraseña actual, nueva y confirmación → `MensajeResponse` |
| Cerrar sesión | POST `/api/v1/auth/logout` | JWT; cliente descarta su token (logout **no revoca** JWT en el backend) |

**Seguridad:** el JWT se guarda solo en `sessionStorage` de la pestaña con una caducidad local calculada de `expiraEnSegundos`; jamás se envía como query o se incrusta en el código. Es un compromiso académico, no sustituye una estrategia de autenticación de producción. El frontend bloquea la ruta visual `/admin.html` para clientes; las autorizaciones reales las debe validar Gateway y cada MS. La pantalla administrativa sigue siendo maqueta y se muestra expresamente como tal.

**Fuera de esta fase:** catálogo y productos reales, sincronización de carrito con Orders, checkout/pago, recuperación de contraseña/correo, eliminación de cuenta, avatar (el DTO de perfil no tiene campo avatar) y funcionalidad del panel administrador. No se inventan operaciones exitosas. Los errores de red, 400 y 409 se muestran sin borrar los datos escritos en los formularios.

## Pruebas en Windows (PowerShell)

```powershell
npm.cmd install
npm.cmd run test
npm.cmd run test:coverage
npm.cmd run build
npm.cmd run dev
```

Para probar operaciones REALES, además de abrir React en `http://localhost:5173`, deben estar encendidos XAMPP MySQL (puerto predeterminado **3306**), MS-Accounts (8081) y API Gateway (8080), con los JWT_SECRET iguales. Los demás MS no son necesarios para probar SOLO registro, login y perfil. Si no están arriba, se informa error de conexión y **no hay sesión/registro ficticios**.

Recomendación de prueba manual, una acción por vez: registrar RUT válido, verificar respuesta; iniciar sesión; recargar y visitar perfil; actualizar un dato; comprobar contraseña; cerrar sesión; comprobar acceso denegado a `/admin.html` con rol CLIENTE; comprobar errores 401/400/409. **El equipo todavía no ha demostrado que el backend compile o funcione; estas pruebas reales permanecen pendientes.**

## Variables

`.env.example`: `VITE_API_BASE_URL=http://localhost:8080`. Copia a `.env` solo si requieres otro Gateway. No colocar secretos JWT ni credenciales MySQL en Vite; `VITE_*` se envía al navegador.
