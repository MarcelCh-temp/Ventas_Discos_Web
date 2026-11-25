CREATE TABLE IF NOT EXISTS ruleta_usuario (
    user_id BIGINT NOT NULL PRIMARY KEY,
    ultimo_giro TIMESTAMP NULL,
    premio VARCHAR(25) NOT NULL DEFAULT 'NINGUNO',
    estado VARCHAR(25) NOT NULL DEFAULT 'SIN_GIRO',
    pedido_id BIGINT NULL
);

CREATE INDEX IF NOT EXISTS idx_ruleta_pedido
ON ruleta_usuario (pedido_id);
