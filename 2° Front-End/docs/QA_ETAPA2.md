# QA de Etapa 2 (verificar en el PC del equipo)

## Instalación y build

- [ ] `npm.cmd install` funciona
- [ ] `npm.cmd run build` termina con `built in ...` y cero errores TypeScript
- [ ] `npm.cmd run dev` abre `http://localhost:5173/`

## Revisión visual y navegación

- [ ] `/` conserva banner, cintillo, navbar, imágenes, cards y footer anterior
- [ ] `/index.html` abre la misma página de inicio
- [ ] Abrir el menú desplegable CATEGORÍAS en escritorio y móvil
- [ ] `/musica.html`, `/series.html`, `/peliculas.html`, `/anime.html` conservan estilos y muestran categoría activa correcta
- [ ] `/bluray.html`, `/blurray.html`, `/videojuegos.html`, `/merchandising.html`, `/accesorios.html` conservan estilos y navegación
- [ ] `/resultados.html?q=test` abre la página de resultados (todavía sin consultar Catalog)
- [ ] `/carrito.html` y `/checkout.html` se muestran, sin procesar compras ficticias
- [ ] `/login.html`, `/registro.html`, `/perfil.html` cargan (sin login simulado)
- [ ] `/admin.html` carga sin ejecutar administración ficticia
- [ ] `/ruta-inexistente` muestra 404
- [ ] Se muestran los avisos de acciones pendientes en botones de compra heredados
- [ ] Al navegar repetidamente no quedan barras duplicadas, estilos anteriores ni avisos superpuestos

## Contratos y restricciones

- Las tarjetas y precios son ejemplos del frontend anterior; NO se atribuyen a MySQL.
- Los endpoints de Orders antiguos no se utilizan.
- No afirmar cumplimiento total de E2: faltan carrito React/TS tipado y las pruebas de Vitest/RTL.
