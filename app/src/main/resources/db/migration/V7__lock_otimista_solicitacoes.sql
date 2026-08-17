-- Lock otimista para evitar corrida em transicoes de status simultaneas (issue #80).
ALTER TABLE solicitacoes ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
