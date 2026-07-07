-- V3: Catalogo administravel de Maquinas (referenciado por nome em modelos.maquina)

CREATE TABLE maquinas (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed do catalogo inicial
INSERT INTO maquinas (id, nome, ativo, criado_em, atualizado_em) VALUES
    (gen_random_uuid(), 'VICK', TRUE, now(), now()),
    (gen_random_uuid(), 'FBO', TRUE, now(), now()),
    (gen_random_uuid(), 'FBOX', TRUE, now(), now()),
    (gen_random_uuid(), 'FAST LOOP', TRUE, now(), now()),
    (gen_random_uuid(), 'INJETORA DE ALUMINIO', TRUE, now(), now()),
    (gen_random_uuid(), 'CENTRIFUGA VERTICAL', TRUE, now(), now()),
    (gen_random_uuid(), 'CENTRIFUGA HORIZONTAL', TRUE, now(), now());

-- Preservar valores ja usados em modelos que nao estejam no seed
INSERT INTO maquinas (id, nome, ativo, criado_em, atualizado_em)
SELECT gen_random_uuid(), m.maquina, TRUE, now(), now()
FROM (SELECT DISTINCT maquina FROM modelos) m
WHERE m.maquina NOT IN (SELECT nome FROM maquinas);
