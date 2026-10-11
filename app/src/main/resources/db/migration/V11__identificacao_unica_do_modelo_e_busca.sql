-- V11: identificacao unica do modelo e funcao de busca sem acento.
--
-- 1) Saneia duplicatas de (maquina, codigo, versao): o modelo mais antigo de cada grupo mantem a
--    versao; os demais recebem a maior versao do grupo somada a sua posicao. Modelos sem
--    duplicata nao mudam.
-- 2) Indice unico em (maquina, codigo, versao).
-- 3) rgm_sem_acento: minuscula e sem acento, usada na busca textual de solicitacoes e usuarios.
WITH ordenados AS (
  SELECT id,
         ROW_NUMBER() OVER (PARTITION BY maquina, codigo, versao ORDER BY criado_em, id) AS posicao,
         MAX(versao) OVER (PARTITION BY maquina, codigo) AS maior_versao
  FROM modelos
),
repetidos AS (
  SELECT o.id,
         o.maior_versao,
         ROW_NUMBER() OVER (PARTITION BY m.maquina, m.codigo ORDER BY m.criado_em, m.id) AS sequencia
  FROM ordenados o
  JOIN modelos m ON m.id = o.id
  WHERE o.posicao > 1
)
UPDATE modelos
SET versao = r.maior_versao + r.sequencia
FROM repetidos r
WHERE modelos.id = r.id;

CREATE UNIQUE INDEX uq_modelos_maquina_codigo_versao ON modelos (maquina, codigo, versao);

CREATE FUNCTION rgm_sem_acento(texto TEXT) RETURNS TEXT AS $$
  SELECT translate(
    lower(texto),
    'áàâãäåéèêëíìîïóòôõöúùûüçñýÿ',
    'aaaaaaeeeeiiiiooooouuuucnyy'
  )
$$ LANGUAGE SQL IMMUTABLE;
