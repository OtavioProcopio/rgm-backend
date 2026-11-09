-- Instrucao do que fazer: gravada na triagem e corrigida em andamento (spec 015).
-- Anulavel e sem valor padrao: solicitacoes ja triadas ficam sem instrucao, sem inventar texto.
ALTER TABLE solicitacoes ADD COLUMN instrucao TEXT;
