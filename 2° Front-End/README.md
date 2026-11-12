# Ñackson Store — Frontend React + TypeScript

## Versión candidata 0.6.0 — integración completa por Gateway

Se trabaja sobre la Etapa 5.1 corregida y los contratos definitivos de los Lotes 1 a 6. **Esta entrega requiere validación de compilación, Vitest y pruebas reales en el PC del equipo.** No se afirma verificada.

```powershell
npm.cmd install
npm.cmd run test
npm.cmd run build
npm.cmd run dev
```

Vite: `http://localhost:5173/` · API Gateway: `http://localhost:8080`.

Funciones implementadas en React con respuestas reales: Accounts, Catalog, ofertas, favoritos, carrito de Orders, checkout de pago académico simulado, pedidos, seguimiento, soporte, notificaciones, postventa, panel administrador. Las API siguen siendo responsables de autorizar, calcular precios y guardar datos. Sin mocks de transacciones.

**No se incluyen microservicios ni bases de datos ni contraseñas.** El backend actualizado se conserva por separado. Para alcance y checklist detallado ver [docs/ETAPA5_COMPLETA_GUIA_PRUEBAS.md](docs/ETAPA5_COMPLETA_GUIA_PRUEBAS.md).

Cuentas ADMINISTRADOR no se crean mediante registro público. El catálogo oficial solo tiene las categorías MUSICA, SERIES_PELICULAS_ANIME y VIDEOJUEGOS. Las rutas HTML heredadas siguen disponibles como navegación visual; no se ejecuta su JavaScript antiguo.
