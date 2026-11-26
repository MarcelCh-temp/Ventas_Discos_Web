-- MIGRACION ESTRUCTURAL DE POSTVENTA
-- Compatible con MariaDB 10.4.
-- UML 04, CC-06, RN-41 y DOC-09 / RNF-09.
-- Respaldar la base y detener MS-PostSale antes de ejecutar.
-- No elimina tablas, columnas ni registros.
-- No migra automaticamente solicitudes historicas.

USE bd_ms_postsale;

ALTER TABLE solicitudes_postventa
    ADD COLUMN IF NOT EXISTS tipo
        ENUM('CANCELACION','DEVOLUCION','REEMBOLSO') NULL,
    ADD COLUMN IF NOT EXISTS estado
        ENUM('PENDIENTE','APROBADA','RECHAZADA','REGISTRADA') NULL,
    ADD COLUMN IF NOT EXISTS motivo_solicitud
        VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS motivo_resolucion
        VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS fecha_resolucion
        DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS solicitud_origen_id
        BIGINT NULL,
    ADD COLUMN IF NOT EXISTS usuario_resolucion_id
        BIGINT NULL;

ALTER TABLE recepciones_devolucion
    ADD COLUMN IF NOT EXISTS usuario_recepcion_id
        BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_solicitud_pedido
    ON solicitudes_postventa (pedido_id);

-- Verificar duplicados antes de incorporar el indice unico.

SELECT solicitud_origen_id, COUNT(*) AS cantidad
FROM solicitudes_postventa
WHERE solicitud_origen_id IS NOT NULL
GROUP BY solicitud_origen_id
HAVING COUNT(*) > 1;

SET @duplicados = (
    SELECT COUNT(*)
    FROM (
        SELECT solicitud_origen_id
        FROM solicitudes_postventa
        WHERE solicitud_origen_id IS NOT NULL
        GROUP BY solicitud_origen_id
        HAVING COUNT(*) > 1
    ) AS origenes_duplicados
);

SET @ddl = IF(
    @duplicados = 0,
    'CREATE UNIQUE INDEX IF NOT EXISTS uk_solicitud_origen ON solicitudes_postventa (solicitud_origen_id)',
    'SELECT ''DETENER: existen origenes duplicados. Revisar con el equipo.'' AS advertencia'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verificacion de columnas.

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME IN (
    'solicitudes_postventa',
    'recepciones_devolucion'
)
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- Verificacion de indices.

SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'solicitudes_postventa'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- Identificar solicitudes historicas pendientes de migracion.

SELECT COUNT(*) AS solicitudes_pendientes_de_migracion
FROM solicitudes_postventa
WHERE tipo IS NULL OR estado IS NULL
   OR tipo = '' OR estado = '';

-- Si existen solicitudes pendientes o duplicados,
-- detenerse y revisar los datos con el equipo.
-- No continuar automaticamente con cambios de relaciones.
