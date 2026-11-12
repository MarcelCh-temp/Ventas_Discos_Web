# Ñackson Store — Etapa 4: pruebas automáticas de frontend

## Objetivo

Validar el **carrito local de demostración** del Sprint 7 y los comportamientos visibles en React, utilizando **Vitest + React Testing Library + user-event + jsdom**, como indica la planificación docente (Sprint 9–10). **No se prueba ni se simula ningún microservicio.**

## Preparación (Windows PowerShell, en la carpeta con `package.json`)

```powershell
npm.cmd install
npm.cmd run build
npm.cmd run test
npm.cmd run test:coverage
```

Si `npm` está bloqueado por la política de PowerShell, usar `npm.cmd` como en los comandos. La carpeta `coverage/` incluye `index.html` para inspeccionar el reporte HTML tras ejecutar `test:coverage`. No se incluye un reporte falso en el ZIP.

## Casos previstos (24 grupos de pruebas)

| Grupo | Casos |
|---|---|
| `cartLogic.test.ts` | 01–12: agregar, incrementar, no duplicar, modificar, rechazar cantidades inválidas, eliminar, vacío, cálculo multiproducto, errores numéricos, inmutabilidad y formato CLP |
| `demoCatalog.test.ts` | 13–15: leer precios CLP, rechazar formato inválido, mantener ID locales de demostración |
| `cartPages.test.tsx` | 16–24: carrito vacío, recuperación de sessionStorage, botones de cantidad, eliminar, vaciar, navegación al checkout, totales, lectura segura y regreso al carrito |

`it.each` realiza varias comprobaciones por casos de entradas inválidas, por lo que Vitest puede reportar **más pruebas individuales** que los 24 grupos enumerados.

## Límites honestos de los resultados

1. **Este ZIP no incluye `node_modules`**; npm descargará las dependencias al ejecutar `npm.cmd install`.
2. El entorno de creación no tenía acceso al registro de npm, por lo que **no se ejecutó Vitest aquí y la cobertura real queda pendiente**. Usa los comandos anteriores en tu PC para obtener evidencia. No afirmamos una tasa de cobertura aún.
3. `npm.cmd run build` verifica TypeScript y la construcción de Vite, pero **no ejecuta las pruebas**. Ambos deben aprobarse por separado.
4. El proyecto todavía no implementa una compra ni un pago. Los precios, artículos e ID son ejemplos locales. Los microservicios permanecen fuera de alcance y no se inventaron endpoints ni DTO.
5. Si Vitest encuentra un fallo, envía el mensaje completo y se corregirá en el código; no se deben cambiar los asserts solo para forzar el éxito.

## Cambio visual autorizado

Solo cambian dos etiquetas de CSS en `src/pages/CheckoutPage.tsx` para mejorar contraste sobre fondo oscuro: subtítulo bajo el encabezado y la explicación del precio/envío definitivo. No se cambian colores globales, estructura visual ni componentes.
