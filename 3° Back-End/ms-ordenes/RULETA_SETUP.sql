-- Ejecutar en bd_ordenes, una sola vez. No borra ni reemplaza otras tablas.
CREATE TABLE IF NOT EXISTS ruleta_usuario (
 user_id BIGINT NOT NULL PRIMARY KEY,
 ultimo_giro DATETIME NULL,
 premio VARCHAR(25) NOT NULL DEFAULT 'NINGUNO',
 estado VARCHAR(25) NOT NULL DEFAULT 'SIN_GIRO',
 pedido_id BIGINT NULL,
 KEY idx_ruleta_pedido (pedido_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Las columnas de la orden se agregan mediante spring.jpa.hibernate.ddl-auto=update.
