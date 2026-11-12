# Ñackson Store — Frontend candidato de integración · revisión por bloques

Versión de trabajo: 0.6.0. Se partió de la Etapa 5.1 corregida (57 pruebas aprobadas por el usuario). Se conservaron los recursos visuales originales. **Este paquete aún no ha pasado build ni pruebas integrales en un equipo con dependencias instaladas.**

## Fuente de los contratos

Únicamente `3° Back-End/seguimiento/CONTRATOS_API_LOTE_1..6.md` y DTO Java del backend de cierre MySQL/Vite. Frontend por Gateway (`http://localhost:8080`), nunca conexiones MySQL ni rutas `/api/v1/internal/**` desde React. Esta entrega **no** altera el backend.

## Alcance preparado

- **Accounts**: registro, login, perfil, cambio de contraseña, recuperación académica, eliminación de cuenta y control de rol desde JWT.
- **Catalog**: productos publicados (sin mocks), búsqueda, filtros, paginación, detalle, ofertas y favoritos. Los tres enums oficiales son `MUSICA`, `SERIES_PELICULAS_ANIME`, `VIDEOJUEGOS`.
- **Orders**: carrito del usuario mediante API, suma y cantidades, vaciar con confirmación, checkout (métodos de entrega y pago desde backend, validar antes de confirmar), pago **simulado académico**, historial, detalle y seguimiento.
- **Support**: tickets reales, detalle y respuesta del cliente.
- **Notifications**: listado, filtros por tipo, marcar una o todas como leídas.
- **PostSale**: solicitudes de cancelación, devolución por artículos de pedido entregado y registro de reembolso; historial y detalle.
- **Admin**: productos (incluye retiro), inventario/movimientos/ajuste/umbral/lote, promociones, órdenes, despacho, soporte, postventa, estadísticas de usuarios y consulta de cuentas por IDs.

**Limitaciones documentadas:**
1. El backend solo contiene tres categorías oficiales. Las antiguas páginas visuales de anime, películas y Blu-ray comparten `SERIES_PELICULAS_ANIME`; accesorios y merchandising no son categorías oficiales de Catalog. La interfaz lo indica; no se inventan artículos.
2. La antigua maqueta local de carrito queda únicamente en archivos de referencia y tests históricos, **no se monta en las rutas públicas**. Los precios en la interfaz conectada provienen de Orders/Catalog.
3. Los widgets decorativos heredados de radio/ruleta no forman parte del ERS ni tienen contratos API y siguen bloqueados por el componente de seguridad del HTML antiguo.
4. La cuenta `ADMINISTRADOR` debe existir en la base configurada por el equipo; el registro público crea únicamente `CLIENTE`. No se inventa un admin ni se distribuyen credenciales.
5. Deben sembrarse productos reales de Catalog: si la base está vacía, no habrá tarjetas ni compras; es un resultado real y esperado.
6. El backend sigue declarado **implementado, sin ejecución**. Hasta ejecutar pruebas reales no se puede garantizar comunicación entre servicios ni las operaciones de extremo a extremo.

## Cómo iniciar (Windows)

Desde `nackson-store-frontend`:

```powershell
npm.cmd install
npm.cmd run test
npm.cmd run build
npm.cmd run dev
```

Abrir `http://localhost:5173/`. Para usar operaciones reales, el equipo debe levantar MySQL 8.4, los seis MS definitivos y Gateway con sus variables `.env`, según la guía del backend. No iniciar el antiguo MS-Inventory. Configuración del frontend: `.env.example` indica `VITE_API_BASE_URL=http://localhost:8080`; no poner JWT secretos, usuarios MySQL ni claves privadas en Vite.

## Checklist de pruebas, **una por vez**

**A — Sin backend**
- [ ] `npm.cmd install` termina.
- [ ] `npm.cmd run test` informa cantidad y fallos exactos.
- [ ] `npm.cmd run build` termina sin error TypeScript/Vite.
- [ ] Login vacío: valida sin sesión falsa.

**B — Gateway + Catalog**
- [ ] Catálogo público devuelve productos reales o listado vacío, sin usar demos.
- [ ] Buscar y filtrar por categoría/stock/precio.
- [ ] Detalle del producto y ofertas vigentes; probar error si Gateway apagado.

**C — Accounts**
- [ ] Registro con RUT/correo válidos; 409 con repetidos.
- [ ] Login: JWT y rol; 401 con contraseña incorrecta.
- [ ] Perfil y cambio de contraseña reales; logout descarta token.
- [ ] Favoritos y rutas protegidas del usuario.

**D — Orders**
- [ ] Agregar desde catálogo; cantidad/stock/errores (409); eliminar y vaciar.
- [ ] Recargar F5, recuperar carrito desde Orders.
- [ ] Checkout: métodos GET públicos, entrega, validate (total de backend), confirm simulado.
- [ ] Pedido CONFIRMADO o PENDIENTE y seguimiento; 402 pago rechazado según backend.

**E — Support + Notifications + PostSale**
- [ ] Ticket, respuesta, mensajes y notificaciones reales.
- [ ] Postventa por pedido propio y estado válido; errores 404/409.

**F — Admin**
- [ ] Login ADMINISTRADOR; cliente no entra a panel.
- [ ] Productos, inventario, promociones, pedidos, tickets, postventa y clientes.
- [ ] Validar cambios con respuesta real y confirmar que persisten en MySQL.

**G — Demo en PC del compañero**
- [ ] Clonar/extraer el mismo paquete, npm install/test/build.
- [ ] Levantar MS + Gateway con `.env` de ese PC, sin claves en GitHub.
- [ ] Ejecutar la demo completa de cliente y administrador.

Para cada prueba registrar **APROBADA / FALLIDA / BLOQUEADA**, captura y error. No reportar cobertura como porcentaje del frontend entero si solo se incluyen archivos seleccionados.
