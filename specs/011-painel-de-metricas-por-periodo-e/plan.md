# Plano de implementação — Painel de métricas por período e ordenação da listagem

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.
> Caminhos relativos a `app/`; produção em `src/main/java/com/rgm/api/`, testes espelhados em
> `src/test/java/com/rgm/api/`.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Ordenação da listagem (RF-01 a RF-04) | Objeto de domínio `OrdenacaoSolicitacao` (critério + sentido) passado a `findByFilters`; a consulta nativa ganha `ORDER BY` com expressão própria por critério, sempre com `criada_em` e `id` como desempate | Ordenar em memória; três consultas nativas duplicadas; `Sort` do Spring com nome de coluna vindo do cliente | Em memória é o erro que a #118 quer sair; duplicar a consulta de 15 filtros três vezes é custo de manutenção; nome de coluna do cliente é injeção |
| Prazo limite no `ORDER BY` | Reaproveitar a expressão SQL que já existe (`PRAZO_LIMITE_EXPR`), com `NULLS LAST` nos dois sentidos | Coluna calculada nova | A expressão é a mesma do filtro `atrasada`; sem migração |
| Sentido da prioridade | Pelas horas do prazo da prioridade: crescente = mais urgente primeiro (URGENTE, ALTA, MEDIA, BAIXA); sem prioridade sempre no fim | Ordem alfabética do nome | Alfabética não tem significado; o critério coincide com o do prazo limite |
| Nomes dos parâmetros | `sort` aceita `criadaEm`, `prazoLimite`, `prioridade`; `dir` aceita `asc`, `desc`; validação em `ParametrosDeConsulta` (padrão da 010), 400 em português | Reaproveitar o enum interno | O `/metricas/por-modelo` já tem `sort` com outros valores; são rotas diferentes, sem conflito |
| Painel (RF-05 a RF-11) | Caso de uso novo `ObterPainelPorPeriodoUseCase` e rota nova `GET /api/solicitacoes/metricas/painel?dias=7\|30\|90&modeloId=` | Estender `/metricas/historico` | Decisão do usuário; o contrato da v1.5.0 não muda |
| Agregação (RNF-02) | Porta nova no repositório com **consultas agregadas** (contagens e médias por dia, geradas com série de datas no banco, no fuso da fábrica); o caso de uso nunca recebe `Solicitacao` | Carregar e somar em memória (como hoje) | RNF-02 pede zero entidade por requisição |
| Dia e semana | O banco agrupa por `date_trunc` aplicado ao instante convertido para o fuso `America/Sao_Paulo`; semana começa na segunda-feira, igual ao histórico atual | Converter em Java depois de buscar | Mantém a agregação no banco (RF-11) |
| Fuso configurável | Propriedade `rgm.fabrica.fuso` com padrão `America/Sao_Paulo`; um `ZoneId` único lido na configuração e injetado | `ZoneId.of(...)` espalhado (já ocorre nos PDFs) | RF-08 pede configurável; os PDFs ficam como estão (fora de escopo) |
| Período anterior (RF-05) | Mesma consulta aplicada à janela deslocada: `[ini - n, ini)`; a resposta traz `atual` e `anterior` com a mesma forma | Duas rotas | Uma requisição, mesmo código |
| Abertas e atrasadas por dia (RF-10) | "Aberta ao fim do dia D" = `criada_em <= fim(D)` e (`encerrada_em` nula ou `> fim(D)`); "atrasada ao fim de D" = aberta nesse instante e `prazo_limite < fim(D)`, com a prioridade de hoje | Reconstruir pelo histórico de atividades | Decisão do usuário (aproximação aceita) |
| Tempo de resolução (RF-07) | Média em segundos, `double` com uma casa na resposta, pelas concluídas **no período** (data de conclusão); campo novo `tempoMedioResolucaoSegundos`; o antigo `slaMediaHoras` fica | Trocar a unidade do campo antigo | Princípio 9 |
| Histórico existente (RF-15) | Passa a usar o fuso, o teto de 365 dias e a consulta agregada; o formato da resposta não muda | Deixar como está | #108; reutiliza a mesma porta do painel para a série |
| Criação fora dos tempos (RF-14) | `AND tipo <> 'CRIACAO'` nas expressões de tempo médio e de intervalo do ranking e do resumo; `total` continua contando todos; `ResumoSolicitacoesModelo.of` ganha a contagem de manutenções para o intervalo | Excluir a criação do total também | Decisão do usuário |
| Índice | Migração aditiva com índice em `solicitacoes (concluida_em)` e `(criada_em)` só se o `EXPLAIN` com 10 000 linhas mostrar varredura que estoure o RNF-01; decidida na tarefa de medição | Índice por antecipação | Princípio de simplicidade: índice entra por problema medido |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Objeto de valor (`OrdenacaoSolicitacao`) | Domínio, porta do repositório | Evita mais dois parâmetros soltos numa assinatura de 16 | Um tipo novo |
| Porta/adaptador (já existente) | Consultas agregadas do painel | Mantém SQL fora do caso de uso | Uma porta com mais métodos |

**Considerados e recusados:** Specification/Criteria builder para a ordenação (a lista fechada de
três critérios não justifica); cache do painel (a carga é de uma consulta agregada e o RNF-01
fica folgado; entra se a medição mostrar o contrário); Strategy por critério de ordenação (um
`switch` sobre enum resolve).

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| domínio | `core/domain/model/enums/CriterioOrdenacaoSolicitacao.java` | criar | `core/domain/model/enums/CriterioOrdenacaoSolicitacaoTest.java` |
| domínio | `core/domain/ports/repositories/OrdenacaoSolicitacao.java` | criar | `core/domain/ports/repositories/OrdenacaoSolicitacaoTest.java` |
| domínio | `core/domain/ports/repositories/SolicitacaoRepository.java` | alterar: `findByFilters` recebe a ordenação; métodos agregados do painel | (interface) |
| domínio | `core/domain/ports/repositories/ResumoSolicitacoesModelo.java` | alterar: contagem de manutenções no intervalo | `core/domain/ports/repositories/ResumoSolicitacoesModeloTest.java` |
| domínio | `core/domain/ports/repositories/PainelAgregado.java` (records de linha) | criar | coberto pelo teste do caso de uso |
| aplicação | `core/application/usecases/solicitacao/ObterPainelPorPeriodoUseCase.java` | criar | `.../ObterPainelPorPeriodoUseCaseTest.java` |
| aplicação | `core/application/usecases/solicitacao/ObterHistoricoMetricasUseCase.java` | alterar: fuso, teto, agregação | `.../ObterHistoricoMetricasUseCaseTest.java` |
| aplicação | `core/application/usecases/solicitacao/ListarSolicitacoesUseCase.java` | alterar: repassa a ordenação | `.../ListarSolicitacoesUseCaseTest.java` |
| adapter | `adapter/out/persistence/repository/SolicitacaoJpaRepository.java` | alterar: ordenação; consultas agregadas; `tipo <> 'CRIACAO'` | `adapter/out/persistence/repository/SolicitacaoJpaRepositoryPostgresTest.java` |
| adapter | `adapter/out/persistence/SolicitacaoRepositoryAdapter.java` | alterar: mapeia linhas agregadas e a ordenação | `adapter/out/persistence/SolicitacaoRepositoryAdapterTest.java` |
| adapter | `adapter/in/web/ParametrosDeConsulta.java` | alterar: `ordenacao(sort, dir)` e `periodoDoPainel(dias)` | `adapter/in/web/ParametrosDeConsultaTest.java` |
| adapter | `adapter/in/web/solicitacao/SolicitacaoController.java` | alterar: `sort`/`dir` na listagem; rota do painel; teto no histórico | `adapter/in/web/solicitacao/SolicitacaoControllerTest.java` e `SolicitacaoControllerSegurancaTest.java` |
| adapter | `adapter/in/web/dto/response/PainelPorPeriodoResponse.java` | criar | coberto pelo teste do controller |
| adapter | `adapter/config/FabricaConfig.java` (`ZoneId` do fuso) | criar | `adapter/config/FabricaConfigTest.java` |
| config | `src/main/resources/application.properties` | alterar: `rgm.fabrica.fuso` | `adapter/config/ConfiguracaoDeProducaoTest.java` |
| adapter | `adapter/config/SecurityConfig.java` | sem mudança: `/api/solicitacoes/metricas/**` já cobre a rota nova (provado por teste) | `adapter/config/SecurityConfigTest.java` (caso novo) |
| doc | `docs/casos-de-uso.md` e `openspec/README.md` | alterar: UC-16 e linha da spec 011 | — |
| migração | `src/main/resources/db/migration/V??__indices_painel.sql` | **condicional** à medição (RNF-01) | `FlywayMigrationTest.java` |

## Contrato entre camadas

- Controller valida `sort`, `dir` e `dias` (400 em português) e converte para tipos de domínio.
- O caso de uso do painel monta as janelas `atual` e `anterior`, chama a porta uma vez por
  janela e **só combina números**; não recebe entidade e não conhece SQL.
- A porta devolve linhas agregadas (por dia ou semana, por indicador); o adaptador as lê da
  consulta nativa e mapeia.
- Erro de validação é `ValidationException` (400) tratado pelo `GlobalExceptionHandler`, como na
  spec 010; falta de perfil é 403 pela cadeia de segurança existente.

## Dependências externas

Nenhuma nova.

## Impacto no contrato de operação

Nenhum alvo novo no `Makefile`. Uma propriedade opcional, `rgm.fabrica.fuso`, com padrão; sem
variável nova obrigatória. **Correção da primeira versão deste plano:** o `make validate` e o
`make test` padrão **excluem** os `*PostgresTest` e o `FlywayMigrationTest` (Makefile:
`-Dtest='!FlywayMigrationTest,!*PostgresTest'`). As consultas novas são provadas por
`make test TESTE='*PostgresTest,FlywayMigrationTest'` e pela esteira do GitHub, que roda
`./mvnw verify` completo (Testcontainers). A tarefa de fechamento registra os dois comandos.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| `ORDER BY` com expressão em consulta nativa paginada com `Pageable` do Spring mexe na `countQuery` | média | A ordenação entra na consulta, não no `Pageable`; teste Postgres com páginas consecutivas (RNF-06) |
| Fuso e horário de verão / fronteira de dia nas séries | média | Testes com instantes às 22h, 23h59 e 00h00 no fuso; `generate_series` sobre datas locais |
| Consulta agregada lenta com 10 000 linhas | baixa | Medição com dados sintéticos no `validate` ou na sonda; índice condicional |
| Prioridade de hoje distorce atraso de dias passados | aceita | Decisão registrada na spec; documentada no UC-16 |
| `slaMediaHoras` truncado continua enganando quem lê o campo antigo | aceita | Mantido por Princípio 9; o novo campo está documentado ao lado |
| Intervalo do resumo muda para modelos com criação | baixa | Teste de caracterização antes da mudança; cenário "criação fora dos tempos" |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| 1 Contrato de operação | Nenhum alvo novo; tudo por `make`; propriedade nova opcional |
| 2 Arquitetura limpa | SQL só no adaptador; caso de uso não importa Spring nem JPA; dependência aponta para dentro |
| 3 Testes provam a entrega | Teste espelhado para cada arquivo de produção; consultas provadas em Postgres; AAA e um comportamento por teste |
| 4 Simplicidade defensável | Um `switch` em vez de Strategy; sem cache; índice só se medido; padrões recusados registrados |
| 5 Autoria | Commits e PR sem crédito a ferramenta de IA |
| 6 Idioma | Spec, plano, mensagens de erro e commits em português |
| 7 Mapa de camadas do legado | Pacotes existentes; `core` sem `org.springframework` |
| 8 Linguagem ubíqua em português | `OrdenacaoSolicitacao`, `PainelPorPeriodo`, `tempoMedioResolucaoSegundos`; termos técnicos em inglês |
| 9 Compatibilidade com a produção | Campos e rotas antigos intactos; teste de contrato do formato v1.5.0; migração só aditiva e condicional |
| 10 Cobertura não regride | ≥ 95% de linha por arquivo tocado; JaCoCo e exclusões intactos |
| 11 Padrão de teste | Só teste novo ou alterado segue o padrão |
| 12 Uma fonte de especificação | Linha da 011 em `openspec/README.md` |
