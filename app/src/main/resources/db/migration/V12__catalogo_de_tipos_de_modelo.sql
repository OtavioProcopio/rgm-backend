-- V12: Catalogo administravel de tipos de modelo (referenciado por nome em modelos.tipo, como
-- as maquinas). Os cinco tipos que ja existiam entram com o proprio identificador como nome,
-- para os modelos e o frontend atuais continuarem valendo; o administrador pode renomear.
CREATE TABLE tipos_modelo (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO tipos_modelo (id, nome) VALUES
    (gen_random_uuid(), 'PLACA_ALUMINIO'),
    (gen_random_uuid(), 'MADEIRA_E_3D'),
    (gen_random_uuid(), 'ALUMINIO_E_3D'),
    (gen_random_uuid(), 'RESINA'),
    (gen_random_uuid(), 'COQUILHA_ACO');

-- Preserva qualquer valor ja gravado que nao esteja no seed.
INSERT INTO tipos_modelo (id, nome)
SELECT gen_random_uuid(), t.tipo
FROM (SELECT DISTINCT tipo FROM modelos WHERE tipo IS NOT NULL) t
WHERE t.tipo NOT IN (SELECT nome FROM tipos_modelo);

ALTER TABLE modelos ALTER COLUMN tipo TYPE VARCHAR(255);
