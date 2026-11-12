# Etapa 5.1 — Corrección del aviso heredado en Accounts

**Alcance:** solo frontend React + TypeScript. No se modificaron microservicios, Gateway, bases de datos ni contratos.

## Problema
`PageFrame` interceptaba cualquier envío de formulario mediante `onSubmitCapture` y mostraba el aviso de acción sin backend, incluso al enviar formularios React de Accounts (login, registro, edición de perfil, cambio de contraseña).

## Solución
Los cuatro formularios modernos tienen `data-nackson-react-form="true"`. `PageFrame` reconoce esa marca y deja que cada formulario maneje su evento, las validaciones y sus llamadas REST al Gateway. Los formularios HTML antiguos sin la marca siguen bloqueados. La búsqueda heredada conserva su redirección.

## Validación
Se agregaron dos pruebas en `src/test/pageFrameForms.test.tsx` para comprobar que el formulario moderno no muestra el aviso heredado y el antiguo permanece bloqueado. El total previsto pasa de 55 a 57 pruebas, sujeto a ejecución en Windows.

```powershell
npm.cmd run test
npm.cmd run build
npm.cmd run dev
```

Para probar una autenticación real son necesarios Accounts y Gateway ejecutándose. Esta corrección no simula autenticaciones.
