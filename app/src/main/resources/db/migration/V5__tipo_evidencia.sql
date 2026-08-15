-- V5: Tipagem de evidencias
--
-- Cada evidencia passa a carregar um tipo (categoria conforme o ponto do
-- fluxo Kanban em que foi anexada) e uma descricao opcional (obrigatoria
-- apenas para o tipo SERVICO_REALIZADO, validado no dominio).
--
-- Evidencias existentes sao migradas como GERAL (comportamento previo,
-- anexo avulso sem categoria especifica).

ALTER TABLE evidencias ADD COLUMN tipo VARCHAR(30) NOT NULL DEFAULT 'GERAL';
ALTER TABLE evidencias ADD COLUMN descricao TEXT;

ALTER TABLE evidencias ALTER COLUMN tipo DROP DEFAULT;
