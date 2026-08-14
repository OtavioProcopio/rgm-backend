-- V4: Galeria de fotos do modelo (substitui a foto de capa unica)
--
-- Cada modelo passa a ter 0..N fotos na galeria, cada uma com uma
-- identificacao (rotulo, ex.: "Parte 1", "Contra-macho") e no maximo
-- uma marcada como principal (usada como capa nas listagens).
--
-- Evidencias (anexos de solicitacao/historico) permanecem isoladas: nunca
-- alimentam a galeria automaticamente.

CREATE TABLE fotos_galeria_modelo (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    modelo_id               UUID NOT NULL REFERENCES modelos(id) ON DELETE CASCADE,
    public_url              VARCHAR(1024) NOT NULL,
    identificacao           VARCHAR(255) NOT NULL,
    principal               BOOLEAN NOT NULL DEFAULT FALSE,
    enviada_por_usuario_id  UUID REFERENCES usuarios(id),
    criado_em               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_fotos_galeria_modelo_modelo_id ON fotos_galeria_modelo(modelo_id);

-- No maximo uma foto principal por modelo.
CREATE UNIQUE INDEX idx_fotos_galeria_modelo_principal
    ON fotos_galeria_modelo(modelo_id)
    WHERE principal = true;

-- Migra a foto de capa existente (se houver) para a galeria, como principal.
-- enviada_por_usuario_id fica nulo: nao ha registro de quem enviou a foto original.
INSERT INTO fotos_galeria_modelo (modelo_id, public_url, identificacao, principal, criado_em)
SELECT id, foto_url, 'Principal', true, COALESCE(foto_atualizada_em, criado_em)
FROM modelos
WHERE foto_url IS NOT NULL;

ALTER TABLE modelos DROP COLUMN foto_url;
ALTER TABLE modelos DROP COLUMN foto_atualizada_em;

ALTER TABLE eventos_modelo DROP COLUMN define_foto_capa;
