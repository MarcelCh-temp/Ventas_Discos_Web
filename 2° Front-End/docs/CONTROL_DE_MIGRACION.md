# Control de migración — Etapa 1

**Fuente:** frontend de `Ventas_Discos_Web-main.zip` entregado anteriormente por el equipo. El último ZIP de documentación no contenía el frontend.

**Decisiones respetadas:** React + TypeScript + SWC + Bootstrap, SPA, conservar apariencia, nombre **Ñackson Store**, no modificar backend ni inventar contratos de API.

**Vistas migradas estructuralmente:**
- `accesorios.html` → `src/pages/AccesoriosPage.tsx`
- `admin.html` → `src/pages/AdminPage.tsx`
- `anime.html` → `src/pages/AnimePage.tsx`
- `bluray.html` → `src/pages/BlurayPage.tsx`
- `blurray.html` → `src/pages/BlurrayPage.tsx`
- `carrito.html` → `src/pages/CarritoPage.tsx`
- `checkout.html` → `src/pages/CheckoutPage.tsx`
- `index.html` → `src/pages/IndexPage.tsx`
- `login.html` → `src/pages/LoginPage.tsx`
- `merchandising.html` → `src/pages/MerchandisingPage.tsx`
- `musica.html` → `src/pages/MusicaPage.tsx`
- `peliculas.html` → `src/pages/PeliculasPage.tsx`
- `perfil.html` → `src/pages/PerfilPage.tsx`
- `registro.html` → `src/pages/RegistroPage.tsx`
- `resultados.html` → `src/pages/ResultadosPage.tsx`
- `series.html` → `src/pages/SeriesPage.tsx`
- `videojuegos.html` → `src/pages/VideojuegosPage.tsx`

Los elementos HTML y clases Bootstrap han sido llevados a JSX. No se asegura paridad visual completa hasta hacer una comparación en navegador.

**Separación de contratos:** `src/models/ApiError.ts` solo refleja el acuerdo documentado por el equipo; no se ha invocado ningún endpoint. `src/services/README.md` describe lo pendiente.

**Prohibiciones para esta fase:** No utilizar `idUsuario=1` ni montos fijos en checkout; no emular un login exitoso; no dar por probado el backend; no enviar solicitudes a los endpoints antiguos de Orders.

**Riesgos/puntos de integración futuros:**
- Scripts JS heredados no ejecutados: widgets de radio/ruleta, búsqueda, productos, sesión, carrito y panel admin.
- Sin `npm install`, no hay test real de bundling; revisar al ejecutar en VS Code.
- Los estilos fueron trasladados literalmente; CSS global legado puede tener selectores con efectos cruzados y deberá validarse visualmente.
- Verificar que menús Bootstrap y medios (videos/fuentes) funcionen en navegador y que no se alteró la estética.
- Debe decidirse si la URL del proyecto React conservará rutas terminadas en `.html` cuando se formalice el Router.

**Entregable:** ZIP independiente; no sobrescribir el repositorio original.
