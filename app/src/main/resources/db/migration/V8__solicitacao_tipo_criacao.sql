-- Solicitacao do tipo CRIACAO: nasce sem modelo vinculado, carregando os dados do
-- modelo a ser criado; ganha modelo_id somente ao ser concluida.
ALTER TABLE solicitacoes ALTER COLUMN modelo_id DROP NOT NULL;

ALTER TABLE solicitacoes ADD COLUMN modelo_codigo VARCHAR(50);
ALTER TABLE solicitacoes ADD COLUMN modelo_maquina VARCHAR(100);
ALTER TABLE solicitacoes ADD COLUMN modelo_observacoes TEXT;

ALTER TABLE solicitacoes ADD CONSTRAINT chk_solicitacao_criacao_dados
  CHECK (
    (tipo <> 'CRIACAO' AND modelo_id IS NOT NULL)
    OR
    (tipo = 'CRIACAO' AND (status = 'CONCLUIDA' OR modelo_id IS NULL))
  );
