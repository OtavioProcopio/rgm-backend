## Context

Ha mais de 2000 modelos cadastrados, mas a grande maioria nunca teve uma
solicitacao concluida. `ObterMetricasSolicitacoesUseCase` ja calcula um tempo
medio de resolucao GLOBAL (`solicitacaoRepository.getTempoMedioResolucaoSegundos()`)
e uma contagem de solicitacoes por modelo (`countGroupByModeloId()`), mas
nenhuma das duas quebra por modelo COM tempo. Este design cobre como calcular
e servir a quebra por modelo sem degradar performance com o volume de dados.

## Goals / Non-Goals

**Goals:**
- Calcular as duas metricas (tempo medio de resolucao e intervalo medio entre
  solicitacoes) por modelo, agregando no banco — nunca carregando todas as
  solicitacoes de todos os modelos em memoria na JVM.
- Paginar e permitir ordenacao pelas duas metricas, para suportar tanto "ver
  os piores modelos" quanto navegacao completa.

**Non-Goals:**
- Nao persiste as metricas calculadas (sempre on-demand; volume atual nao
  justifica cache/materializacao).
- Nao inclui filtro por maquina/periodo nesta primeira versao — pode ser
  adicionado depois seguindo o mesmo padrao de `findByFilters`.
- Nao altera o endpoint de metricas GLOBAIS existente (`GET /metricas`); e um
  endpoint novo e independente.

## Decisions

### Agregacao via query SQL nativa com window function
`SolicitacaoRepository.findMetricasPorModelo(sort, page, size)` sera
implementado no adapter JPA com uma query nativa (nao JPQL puro, que nao
suporta window functions de forma portavel) que:
1. Filtra solicitacoes com `status = 'CONCLUIDA'`.
2. Usa `LAG(criada_em) OVER (PARTITION BY modelo_id ORDER BY criada_em)` para
   obter a data de abertura da solicitacao anterior do mesmo modelo, e
   calcula o intervalo em segundos.
3. Agrupa por `modelo_id`, calculando `AVG(concluida_em - criada_em)` (tempo
   de resolucao) e `AVG(intervalo)` (intervalo entre solicitacoes,
   ignorando a primeira solicitacao de cada modelo, que naturalmente nao tem
   um intervalo anterior).
4. Ordena e pagina no banco (`ORDER BY <coluna> <direcao> LIMIT/OFFSET`).

Alternativa considerada: calcular em memoria no Java, carregando
`List<Solicitacao>` por modelo. Rejeitada por nao escalar com 2000+ modelos
— mesmo que hoje poucos tenham solicitacoes concluidas, o calculo em banco
e O(1) em uso de memoria da aplicacao independente do volume.

### Intervalo baseado em `criadaEm`, nao em `concluidaEm`
O intervalo mede "de quanto em quanto tempo um novo PROBLEMA e reportado
para aquele modelo", nao o tempo entre entregas. Usar `criadaEm` captura
recorrencia do problema mesmo que o tempo de resolucao varie muito entre
chamados. Documentado explicitamente na spec para evitar ambiguidade futura.

### Whitelist de ordenacao no controller
O parametro de ordenacao (`sort=tempoResolucao|intervalo`, `dir=asc|desc`)
sera validado contra um enum fixo antes de chegar na query nativa, nunca
interpolando o nome da coluna a partir de input livre do cliente (evita SQL
injection via `ORDER BY`).

### PDF do ranking como servico separado
`ModeloPdfService.gerarLista` (export em massa da listagem geral) NAO ganha
essas colunas — rodar o calculo agregado para potencialmente milhares de
modelos ao exportar a lista completa seria caro e foge do proposito daquele
relatorio (listagem cadastral simples). O ranking de tempo e um relatorio
proprio, com sua propria paginacao/ordenacao, reaproveitando o mesmo estilo
visual de `ModeloPdfService` (pode viver como metodo novo no mesmo service,
decisao de implementacao livre na fase de tasks).

## Risks / Trade-offs

- [Risco] Query com window function pode ficar lenta sem index adequado em
  `(modelo_id, criada_em)` e `(modelo_id, status)` → Mitigacao: confirmar/
  criar esses indices via migration se o `EXPLAIN ANALYZE` mostrar seq scan
  em volume de teste.
- [Risco] Modelos com 1 unica solicitacao concluida tem tempo de resolucao
  mas nao intervalo — UI precisa tratar o campo ausente sem quebrar
  ordenacao por intervalo → Mitigacao: especificado na spec (campo nulo,
  ordenacao por intervalo naturalmente empurra nulos para o fim).
