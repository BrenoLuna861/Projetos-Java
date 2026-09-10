-- Tabela de eventos de acesso: o cenario classico de "listagem mais recente primeiro".
CREATE TABLE IF NOT EXISTS evento_acesso (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL,
    acao        VARCHAR(40)  NOT NULL,
    ip          VARCHAR(45)  NOT NULL,
    ocorrido_em TIMESTAMP    NOT NULL
);

-- Indice que sustenta TANTO a ordenacao quanto o filtro do keyset.
-- A ordem das colunas importa: e a mesma do ORDER BY (ocorrido_em DESC, id DESC).
CREATE INDEX IF NOT EXISTS idx_evento_ordenacao
    ON evento_acesso (ocorrido_em DESC, id DESC);

-- Indice de apoio para o filtro por usuario combinado com a ordenacao.
CREATE INDEX IF NOT EXISTS idx_evento_usuario_ordenacao
    ON evento_acesso (usuario_id, ocorrido_em DESC, id DESC);
