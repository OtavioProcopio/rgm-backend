-- V6: Indices de suporte ao ranking de metricas de tempo por modelo
--
-- O calculo agrega solicitacoes por modelo_id filtrando por status
-- (tempo medio de resolucao) e ordenando por modelo_id + criada_em
-- (intervalo medio entre solicitacoes via window function). Sem esses
-- indices compostos, a query cairia em seq scan com o crescimento da
-- tabela de solicitacoes.

CREATE INDEX idx_solicitacoes_modelo_status ON solicitacoes(modelo_id, status);
CREATE INDEX idx_solicitacoes_modelo_criada_em ON solicitacoes(modelo_id, criada_em);
