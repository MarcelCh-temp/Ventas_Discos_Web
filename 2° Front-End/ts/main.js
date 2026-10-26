"use strict";
// Función para enviar una compra a ms-ordenes (Puerto 8084)
async function enviarOrden(idUsuario, total) {
    const orden = { idUsuario, total };
    try {
        const response = await fetch('http://localhost:8084/api/ordenes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(orden)
        });
        if (response.ok) {
            const data = await response.json();
            alert('¡Orden creada con éxito! ID: ' + data.id);
        }
        else {
            alert('Error al procesar la orden.');
        }
    }
    catch (error) {
        console.error('Error al conectar con ms-ordenes:', error);
    }
}
// Función para enviar un ticket de ayuda a ms-soporte (Puerto 8087)
async function enviarTicket(idUsuario, asunto, descripcion) {
    const ticket = { idUsuario, asunto, descripcion };
    try {
        const response = await fetch('http://localhost:8087/api/soporte/tickets', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(ticket)
        });
        if (response.ok) {
            const data = await response.json();
            alert('¡Ticket de soporte enviado! ID: ' + data.id);
        }
        else {
            alert('Error al enviar ticket.');
        }
    }
    catch (error) {
        console.error('Error al conectar con ms-soporte:', error);
    }
}
// Exponer las funciones globalmente al navegador para ejecutarlas en la consola
window.enviarOrden = enviarOrden;
window.enviarTicket = enviarTicket;
