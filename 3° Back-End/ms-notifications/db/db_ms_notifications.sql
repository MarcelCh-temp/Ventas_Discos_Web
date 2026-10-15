
-- ==========================================
-- MS-NOTIFICATIONS - VENTAS DISCOS
-- SCRIPT DE BASE DE DATOS
-- ==========================================

CREATE DATABASE IF NOT EXISTS bd_ms_notifications
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE bd_ms_notifications;

-- ==========================================
-- TABLA: NOTIFICACIONES
-- ==========================================

CREATE TABLE IF NOT EXISTS notificaciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    mensaje TEXT NOT NULL,
    tipo VARCHAR(255) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    leida BIT(1) NOT NULL DEFAULT b'0',
    referencia_id BIGINT NULL,

    CONSTRAINT pk_notificaciones
        PRIMARY KEY (id),

    INDEX idx_notificaciones_usuario_fecha
        (usuario_id, fecha_creacion),

    INDEX idx_notificaciones_usuario_leida
        (usuario_id, leida),

    INDEX idx_notificaciones_usuario_tipo
        (usuario_id, tipo)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- TABLA: EVENTOS PROCESADOS
-- ==========================================

CREATE TABLE IF NOT EXISTS eventos_notificacion_procesados (
    id BIGINT NOT NULL AUTO_INCREMENT,
    evento_id VARCHAR(120) NOT NULL,
    origen VARCHAR(40) NOT NULL,
    fecha_procesamiento DATETIME(6) NOT NULL,

    CONSTRAINT pk_eventos_notificacion
        PRIMARY KEY (id),

    CONSTRAINT uk_evento_origen
        UNIQUE (origen, evento_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
