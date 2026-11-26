-- Hacer respaldo y detener MS-Ordenes antes de ejecutar. No elimina datos.
USE bd_ordenes;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ordenes' AND COLUMN_NAME='descuento_ruleta')=0,
'ALTER TABLE ordenes ADD COLUMN descuento_ruleta DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ordenes' AND COLUMN_NAME='costo_envio')=0,
'ALTER TABLE ordenes ADD COLUMN costo_envio DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ordenes' AND COLUMN_NAME='premio_ruleta')=0,
'ALTER TABLE ordenes ADD COLUMN premio_ruleta VARCHAR(25) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ordenes' AND COLUMN_NAME='metodo_pago')=0,
'ALTER TABLE ordenes ADD COLUMN metodo_pago VARCHAR(25) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
CREATE TABLE IF NOT EXISTS ruleta_usuario (
 user_id BIGINT NOT NULL PRIMARY KEY, ultimo_giro DATETIME NULL,
 premio VARCHAR(25) NOT NULL DEFAULT 'NINGUNO', estado VARCHAR(25) NOT NULL DEFAULT 'SIN_GIRO',
 pedido_id BIGINT NULL, KEY idx_ruleta_pedido (pedido_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ordenes' AND COLUMN_NAME='metodo_pago';
