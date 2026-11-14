
package com.ventasdiscos.msnotifications.model;

// Modelo de dominio: representa los datos y las reglas básicas de integridad de una entidad. Clase TipoNotificacion.
public enum TipoNotificacion {

    // Opción PEDIDO_CREADO: representa el estado o tipo «pedido creado» permitido en este dominio.
    PEDIDO_CREADO,
    // Opción PEDIDO_ACTUALIZADO: representa el estado o tipo «pedido actualizado» permitido en este dominio.
    PEDIDO_ACTUALIZADO,

    // Opción SOPORTE_CREADO: representa el estado o tipo «soporte creado» permitido en este dominio.
    SOPORTE_CREADO,
    // Opción SOPORTE_RESPONDIDO: representa el estado o tipo «soporte respondido» permitido en este dominio.
    SOPORTE_RESPONDIDO,
    // Opción SOPORTE_CERRADO: representa el estado o tipo «soporte cerrado» permitido en este dominio.
    SOPORTE_CERRADO,

    // Opción CANCELACION_CREADA: representa el estado o tipo «cancelacion creada» permitido en este dominio.
    CANCELACION_CREADA,
    // Opción CANCELACION_RESUELTA: representa el estado o tipo «cancelacion resuelta» permitido en este dominio.
    CANCELACION_RESUELTA,

    // Opción DEVOLUCION_CREADA: representa el estado o tipo «devolucion creada» permitido en este dominio.
    DEVOLUCION_CREADA,
    // Opción DEVOLUCION_RESUELTA: representa el estado o tipo «devolucion resuelta» permitido en este dominio.
    DEVOLUCION_RESUELTA,

    // Opción REEMBOLSO_REGISTRADO: representa el estado o tipo «reembolso registrado» permitido en este dominio.
    REEMBOLSO_REGISTRADO
}
