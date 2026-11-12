# Etapa 3 — comprobaciones manuales

**Alcance:** React + TypeScript, carrito de interfaz con 8 productos DEMO ya existentes en la Etapa 2. No se crean pedidos, se consulta inventario ni se realizan pagos.

1. Abrir `/anime.html`; pulsar **Comprar** una vez. Debe aparecer «Añadido al carrito de demostración» y enlace «Ver carrito».
2. Pulsar de nuevo Comprar. Abrir `/carrito.html`: el ítem debe tener cantidad **2**, y su subtotal debe ser 2 × $32.990 = **$65.980**.
3. Pulsar `+`, `−` y editar el input a un entero mayor que cero; comprobar subtotal y total provisional. No deben admitirse cero, negativos ni decimales.
4. Abrir `/musica.html` y añadir un vinilo $27.990; verificar dos líneas en el carrito y que el subtotal sea la suma de ambas.
5. Pulsar **Eliminar** de una línea; comprobar que la otra permanece. Luego pulsar **Vaciar carrito** y verificar estado vacío y el checkout deshabilitado.
6. Agregar algo, entrar a `/checkout.html`: mostrar los mismos ítems, cantidades y subtotal; **Pago pendiente de integración** debe estar deshabilitado.
7. Refrescar el navegador en la misma pestaña: los artículos deben conservarse mediante `sessionStorage` de DEMO; al cerrar esa pestaña, la sesión no es una compra guardada.
8. Comprobar que `/`, `/anime.html`, `/musica.html`, `/videojuegos.html`, `/admin.html` y `/login.html` mantienen diseño y navegación de Etapa 2.
9. Probar `npm.cmd run build`. Debe terminar `tsc -b && vite build` sin errores.
10. Confirmar que no existe ninguna petición HTTP de compra ni se marca ningún pedido como pagado.

**Nota:** en el checkout se conserva la maqueta del formulario Soporte, pero su envío sigue bloqueado por PageFrame hasta la etapa de integración correspondiente.
