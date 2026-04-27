-- V1: Schema inicial do RGM

-- Usuários
CREATE TABLE usuarios (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) UNIQUE,
    senha_hash      VARCHAR(255),
    perfil          VARCHAR(50) NOT NULL,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Máquinas
CREATE TABLE maquinas (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome            VARCHAR(255) NOT NULL,
    codigo          VARCHAR(100) NOT NULL,
    descricao       TEXT,
    ativa           BOOLEAN NOT NULL DEFAULT TRUE,
    criada_em       TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizada_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Modelos
CREATE TABLE modelos (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo                      VARCHAR(100) NOT NULL,
    versao                      INTEGER NOT NULL DEFAULT 1,
    descricao                   VARCHAR(255) NOT NULL,
    observacoes                 TEXT,
    foto_url                    VARCHAR(1024),
    foto_atualizada_em          TIMESTAMPTZ,
    estado_atual_descricao      TEXT,
    estado_atual_atualizado_em  TIMESTAMPTZ,
    ativo                       BOOLEAN NOT NULL DEFAULT TRUE,
    maquina_id                  UUID NOT NULL REFERENCES maquinas(id),
    tem_pendencia_aberta        BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em               TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Solicitações
CREATE TABLE solicitacoes (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo                  VARCHAR(255) NOT NULL,
    descricao               TEXT,
    tipo                    VARCHAR(50) NOT NULL,
    status                  VARCHAR(50) NOT NULL,
    prioridade              VARCHAR(50),
    modelo_id               UUID NOT NULL REFERENCES modelos(id),
    aberta_por_usuario_id   UUID NOT NULL REFERENCES usuarios(id),
    comentario_final        TEXT,
    criada_em               TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizada_em           TIMESTAMPTZ NOT NULL DEFAULT now(),
    concluida_em            TIMESTAMPTZ,
    cancelada_em            TIMESTAMPTZ
);

-- Evidências
CREATE TABLE evidencias (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    public_url              VARCHAR(1024) NOT NULL,
    mime_type               VARCHAR(100) NOT NULL,
    nome_arquivo            VARCHAR(255) NOT NULL,
    tamanho_bytes           INTEGER,
    enviada_por_usuario_id  UUID NOT NULL REFERENCES usuarios(id),
    criada_em               TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Atribuições de solicitação
CREATE TABLE solicitacao_atribuicoes (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitacao_id          UUID NOT NULL REFERENCES solicitacoes(id) ON DELETE CASCADE,
    usuario_id              UUID NOT NULL REFERENCES usuarios(id),
    atribuido_por_usuario_id UUID NOT NULL REFERENCES usuarios(id),
    atribuido_em            TIMESTAMPTZ NOT NULL DEFAULT now(),
    removido_em             TIMESTAMPTZ
);

-- Atividades de solicitação (auditoria)
CREATE TABLE atividades_solicitacao (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitacao_id      UUID NOT NULL REFERENCES solicitacoes(id) ON DELETE CASCADE,
    tipo                VARCHAR(50) NOT NULL,
    de_status           VARCHAR(50),
    para_status         VARCHAR(50),
    comentario          TEXT,
    autor_usuario_id    UUID NOT NULL REFERENCES usuarios(id),
    criada_em           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Join: solicitação ↔ evidência
CREATE TABLE solicitacao_evidencias (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitacao_id  UUID NOT NULL REFERENCES solicitacoes(id) ON DELETE CASCADE,
    evidencia_id    UUID NOT NULL REFERENCES evidencias(id) ON DELETE CASCADE
);

-- Eventos de modelo (histórico)
CREATE TABLE eventos_modelo (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    modelo_id                   UUID NOT NULL REFERENCES modelos(id) ON DELETE CASCADE,
    tipo                        VARCHAR(50) NOT NULL,
    titulo                      VARCHAR(255) NOT NULL,
    descricao                   TEXT NOT NULL,
    estado_modelo_descricao     TEXT,
    define_foto_capa            BOOLEAN NOT NULL DEFAULT FALSE,
    executado_por_usuario_id    UUID NOT NULL REFERENCES usuarios(id),
    solicitacao_relacionada_id  UUID REFERENCES solicitacoes(id),
    criado_em                   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Join: evento_modelo ↔ evidência
CREATE TABLE evento_modelo_evidencias (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evento_modelo_id    UUID NOT NULL REFERENCES eventos_modelo(id) ON DELETE CASCADE,
    evidencia_id        UUID NOT NULL REFERENCES evidencias(id) ON DELETE CASCADE
);

-- Índices
CREATE INDEX idx_solicitacoes_modelo_id ON solicitacoes(modelo_id);
CREATE INDEX idx_solicitacoes_status ON solicitacoes(status);
CREATE INDEX idx_solicitacoes_aberta_por ON solicitacoes(aberta_por_usuario_id);
CREATE INDEX idx_modelos_maquina_id ON modelos(maquina_id);
CREATE INDEX idx_atribuicoes_solicitacao_id ON solicitacao_atribuicoes(solicitacao_id);
CREATE INDEX idx_atribuicoes_usuario_id ON solicitacao_atribuicoes(usuario_id);
CREATE INDEX idx_atividades_solicitacao_id ON atividades_solicitacao(solicitacao_id);
CREATE INDEX idx_evidencias_enviada_por ON evidencias(enviada_por_usuario_id);
CREATE INDEX idx_solicitacao_evidencias_solicitacao ON solicitacao_evidencias(solicitacao_id);
CREATE INDEX idx_eventos_modelo_modelo_id ON eventos_modelo(modelo_id);
CREATE INDEX idx_evento_modelo_evidencias_evento ON evento_modelo_evidencias(evento_modelo_id);
