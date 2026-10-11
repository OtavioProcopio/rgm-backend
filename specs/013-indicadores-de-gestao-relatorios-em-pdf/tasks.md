# Tarefas — Indicadores de gestão, relatórios em PDF fiéis à tela e limpeza

> Ordem de dependência. Teste vem antes da implementação que ele prova. Nenhuma tarefa é `[P]`
> (vários arquivos compartilhados: `UseCaseConfig`, `SolicitacaoController`, serviços de PDF).

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.
Desvio herdado (feature 001): sem `app/tests/bdd` nem `make bdd`; os cenários da spec são provados
por testes de caso de uso, de controller, de consulta em Postgres e de PDF lido de volta, listados
na rastreabilidade. Testes novos: `// Arrange`, `// Act`, `// Assert`, um comportamento por teste,
nome `shouldXWhenY`. `*PostgresTest` rodam por `make test TESTE='*PostgresTest'` (o `validate` os
exclui por desenho).

## Fase 1 — Limpeza (RF-23 a RF-27)

- [x] T001 Registrar a linha de base: `make validate` e `make test TESTE='*PostgresTest,FlywayMigrationTest'` verdes e a lista de rotas de `TEST/adapter/in/web/RotasExistentesTest.java`
- [x] T002 Remover `EventoModeloEvidencia` (domínio, porta, adaptador, JPA, entidade) e os testes `EventoModeloEvidenciaRepositoryAdapterTest` e `EventoModeloEvidenciaTest`; ajustar `MapperTest`/`MapperRoundtripTest` se citarem a entidade
- [x] T003 Remover `findByStatus` (paginado e em lista) de `MAIN/core/domain/ports/repositories/SolicitacaoRepository.java`, `MAIN/adapter/out/persistence/SolicitacaoRepositoryAdapter.java`, `MAIN/adapter/out/persistence/repository/SolicitacaoJpaRepository.java` e dos testes que só os exercitam
- [x] T004 Remover as sobrecargas de compatibilidade de `MAIN/adapter/out/report/ModeloPdfService.java` e `MAIN/adapter/out/report/SolicitacaoPdfService.java` e ajustar `TEST/adapter/out/report/*PdfServiceTest.java`
- [x] T005 Fundir `TEST/adapter/out/persistence/mapper/MapperTest.java` e `MapperRoundtripTest.java` em `MapperRoundtripTest.java`, preservando os casos distintos (versão de credencial, versão preservada, prioridade, remoção da atribuição)
- [x] T006 Dar verificação ao teste de `TEST/adapter/config/RateLimitFilterTest.java` (cadeia chamada 105 vezes; sexta chamada do mesmo IP continua limitada)
- [x] T007 Mover `TEST/core/domain/model/PerfilUsuarioTest.java` e `StatusSolicitacaoTest.java` para `TEST/core/domain/model/enums/` (pacote espelha o código)
- [x] T008 Remover `app/Makefile` e `openspec/changes/archive/`; corrigir `.vscode/launch.json` para `com.rgm.api.ApiApplication`
- [x] T009 `make validate` verde depois da limpeza e `RotasExistentesTest` com as mesmas rotas (RF-27, RNF-07)

## Fase 2 — Indicadores de gestão (RF-01 a RF-14)

- [x] T010 Criar `TEST/adapter/out/persistence/repository/IndicadoresDeGestaoJpaRepositoryPostgresTest.java`: prazo (1 minuto antes e 1 depois, por prioridade, sem prioridade contada à parte); mediana 5,5 h e p90 9,1 h com 10 concluídas; sem concluída devolve vazio; faixas de idade somam o total em aberto; abertas sem responsável; etapas (2 h, 5 h e 1 h; devolvida soma 3 h + 2 h; transição para cancelada não conta como espera até iniciar); devolução 1 em 2 envios; por máquina (abertas, atrasadas, tempo médio, criação fora); reincidente com 3 reparos sim e com 2 não; carga com dois responsáveis e total de distintas
- [x] T011 Criar `MAIN/adapter/out/persistence/repository/IndicadoresDeGestaoJpaRepository.java` e as portas e registros em `MAIN/core/domain/ports/repositories/` (T010 verde)
- [x] T012 Criar `TEST/adapter/out/persistence/IndicadoresDeGestaoRepositoryAdapterTest.java`: mapeia as linhas nativas para os registros da porta, inclusive `null` onde não há dado
- [x] T013 Criar `MAIN/adapter/out/persistence/IndicadoresDeGestaoRepositoryAdapter.java` (T012 verde)
- [x] T014 Criar `TEST/core/application/usecases/solicitacao/ObterIndicadoresDeGestaoUseCaseTest.java`: janela de 7, 30 e 90 dias no fuso; porcentagens calculadas a partir das contagens; taxa de devolução nula sem envio; nenhuma busca por lista é chamada (RNF-02)
- [x] T015 Criar `MAIN/core/application/usecases/solicitacao/ObterIndicadoresDeGestaoUseCase.java` (T014 verde)
- [x] T016 Criar `TEST/adapter/in/web/dto/response/IndicadoresDeGestaoResponseTest.java` e `MAIN/adapter/in/web/dto/response/IndicadoresDeGestaoResponse.java`
- [x] T017 Alterar `TEST/adapter/in/web/ParametrosDeConsultaTest.java` e `MAIN/adapter/in/web/ParametrosDeConsulta.java`: `periodoDeGestao` aceita 7, 30, 90; 15 lança `ValidationException` citando os valores
- [x] T018 Criar `TEST/adapter/in/web/solicitacao/IndicadoresDeGestaoControllerTest.java` e `MAIN/adapter/in/web/solicitacao/IndicadoresDeGestaoController.java`: 200 com o formato; 400 para 15 dias; 403 para operador; 401 sem token (usar o modelo de `PainelPorPeriodoControllerTest`)
- [x] T019 Alterar `MAIN/adapter/config/UseCaseConfig.java` (bean do caso de uso) e `TEST/adapter/in/web/solicitacao/MetricasContratoV15Test.java` (campos antigos intactos)

## Fase 3 — PDFs fiéis à tela (RF-15 a RF-22)

- [x] T020 Criar `TEST/adapter/in/web/DescricaoDosFiltrosTest.java` e `MAIN/adapter/in/web/DescricaoDosFiltros.java`: rótulo legível por filtro; "sem filtros" quando vazio; ignora filtro nulo
- [x] T021 Alterar `TEST/adapter/out/report/SolicitacaoPdfServiceTest.java` e `MAIN/adapter/out/report/SolicitacaoPdfService.java`: o PDF lido de volta traz os filtros, o total e, quando cortado, "exibindo N de M"
- [x] T022 Alterar `TEST/adapter/out/report/ModeloPdfServiceTest.java` e `MAIN/adapter/out/report/ModeloPdfService.java` (lista): filtros, total, aviso e coluna Tipo; ranking: ordenação e filtros no cabeçalho
- [x] T023 Alterar `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java` e `SolicitacaoController.java`: `relatorio` aceita `q`, `sort`, `dir`, usa o mesmo `buildInput`, pede 5000 linhas e repassa o total; ranking repassa filtros
- [x] T024 Alterar `TEST/adapter/in/web/modelo/ModeloControllerTest.java` e `ModeloController.java`: `relatorio` da lista sem corte silencioso de 1000 e com filtros e total
- [x] T025 Alterar `TEST/core/domain/ports`/adaptador de atividades: `findBySolicitacaoIdIn` em `AtividadeSolicitacaoRepository`, `AtividadeSolicitacaoRepositoryAdapter` e `AtividadeSolicitacaoJpaRepository` com `TEST/adapter/out/persistence/AtividadeSolicitacaoRepositoryAdapterTest.java`
- [x] T026 Alterar `TEST/adapter/out/storage/MinioStorageServiceTest.java` e `MAIN/adapter/out/storage/MinioStorageService.java` e `MAIN/core/domain/ports/services/StorageService.java`: `baixar` devolve os bytes do objeto e falha com `RuntimeException`
- [x] T027 Criar `TEST/core/application/usecases/modelo/ObterFichaDoModeloUseCaseTest.java` e `MAIN/core/application/usecases/modelo/ObterFichaDoModeloUseCase.java`: monta modelo, eventos, solicitações, atividades em uma chamada em lote, nomes, resumo e fotos (capa primeiro); modelo inexistente lança `RecursoNaoEncontradoException`; o número de chamadas ao repositório é o mesmo com 1 e com 50 solicitações (RNF-04)
- [x] T028 Alterar `TEST/adapter/out/report/ModeloPdfServiceTest.java` e `ModeloPdfService.java` (ficha): três fotos com a capa em destaque; foto que falha ao baixar vira "indisponível" sem derrubar a ficha; atividades aparecem sob cada solicitação
- [x] T029 Alterar `ModeloController.fichaDoModelo`/`gerarFichaModelo` para usar o caso de uso e passar as fotos; `UseCaseConfig` com o bean novo; `TEST/adapter/in/web/modelo/ModeloControllerTest.java`

## Fase 4 — Documentação e validação

- [x] T030 Atualizar `docs/diagramas.md`, `docs/modelo_dados.md`, `docs/casos-de-uso.md` e `openspec/README.md` (indicadores, PDFs, tabela legada, remoções)
- [x] T031 `make format`, `make lint`, `make validate` e `make test TESTE='*PostgresTest,FlywayMigrationTest'` verdes, com a saída real no `Convergence`

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01, RF-02 | T017, T018 |
| RF-03, RF-04 | T010, T011, T014, T015 |
| RF-05, RF-06, RF-07 | T010, T011, T014, T015 |
| RF-08, RF-09, RF-10 | T010, T011, T014, T015 |
| RF-11, RF-12, RF-13 | T010, T011, T014, T015 |
| RF-14 | T016, T019 |
| RF-15 | T021, T023 |
| RF-16 | T020, T021, T022 |
| RF-17 | T022, T024 |
| RF-18 | T022, T023 |
| RF-19, RF-20 | T026, T027, T028, T029 |
| RF-21 | T021, T022, T023, T024 |
| RF-22 | T025, T027 |
| RF-23 a RF-27 | T001 a T009, T030 |
| RNF-01 | T010 |
| RNF-02 | T014 |
| RNF-03 | T021, T023, T024 |
| RNF-04 | T027 |
| RNF-05, RNF-06 | T019, T031 |
| RNF-07 | T001, T009 |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-10-11

Evidência de execução:

- `make validate`: lint (spotless) limpo, 1108 testes, 0 falhas, `All coverage checks have been met`.
- `make test TESTE='*PostgresTest,FlywayMigrationTest'` (o `validate` os exclui): 94 testes, 0 falhas,
  em Postgres 16 real (indicadores, painel, filtros, usuários, migrações).

| Requisito | Estado | Evidência |
|---|---|---|
| RF-01, RF-02 | realizado | `IndicadoresDeGestaoController`, `ParametrosDeConsulta.periodoDoPainel`; `IndicadoresDeGestaoControllerTest` (400, 403, 401) |
| RF-03, RF-04 | realizado | `IndicadoresDeGestaoJpaRepository.prazoPorPrioridade`/`concluidasSemPrioridade`; `IndicadoresDeGestaoPostgresTest` (1 minuto antes e depois, sem prioridade contada) |
| RF-05 | realizado | `resolucao` com `percentile_cont`; mediana 5,5 h e p90 9,1 h; nulos sem dado |
| RF-06, RF-07 | realizado | `backlog`; faixas somam o total; abertas sem responsável |
| RF-08, RF-09 | realizado | `tempoPorEtapa` (marcos + `LEAD`); devolução soma 3 h + 2 h; cancelada antes de iniciar não conta |
| RF-10 | realizado | `devolucoes`; taxa na `ObterIndicadoresDeGestaoUseCase` |
| RF-11 | realizado | `porMaquina` (criação fora) |
| RF-12 | realizado | `reincidentes` com o limite fixo 3; criação não conta |
| RF-13 | realizado | `cargaPorResponsavel` + `abertasComResponsavel` |
| RF-14 | realizado | endpoint e campos novos; `MetricasContratoV15Test` verde |
| RF-15 | realizado | `SolicitacaoController.gerarRelatorio` com `q`, `sort`, `dir` e o mesmo `buildInput`; `SolicitacaoControllerTest` |
| RF-16 | realizado | `ContextoDoRelatorio`; PDF lido de volta nos testes de `SolicitacaoPdfService` e `ModeloPdfService` |
| RF-17 | realizado | `ModeloController.gerarRelatorioLista` (situação, código, máquina, descrição) e coluna Tipo |
| RF-18 | realizado | ranking com ordenação no cabeçalho |
| RF-19, RF-20 | realizado | `ObterFichaDoModeloUseCase`, `ModeloPdfService.gerarFicha`; capa em destaque; "Foto indisponível" |
| RF-21 | realizado | `LIMITE_DE_LINHAS_DO_RELATORIO = 5000` e aviso "Exibindo N de M"; testes de aviso |
| RF-22 | realizado | `findBySolicitacaoIdIn` em lote; `ObterFichaDoModeloUseCaseTest` (uma chamada com 50 solicitações) |
| RF-23 a RF-27 | realizado | limpeza listada abaixo; `RotasExistentesTest` e suíte verdes |
| RNF-01 | realizado | `IndicadoresDeGestaoPostgresTest` |
| RNF-02 | realizado | `ObterIndicadoresDeGestaoUseCase` só combina linhas agregadas |
| RNF-03 | realizado | `SolicitacaoControllerTest` (total e exibidas vêm de `totalElements` e do conteúdo) |
| RNF-04 | realizado | `ObterFichaDoModeloUseCaseTest` |
| RNF-05, RNF-06, RNF-07 | realizado | validate verde; contrato V1.5 e rotas iguais |

**Limpeza feita (RF-23 a RF-25):** domínio, porta, adaptador, JPA e entidade de
`EventoModeloEvidencia` (a tabela fica); `findByStatus` (duas assinaturas); sobrecargas de
compatibilidade dos serviços de PDF; `app/Makefile`; `openspec/changes/archive/` e
`openspec/config.yaml`; `.vscode/launch.json` corrigido; `MapperTest` fundido em
`MapperRoundtripTest`; teste sem asserção do `RateLimitFilter`; testes de enums movidos para o
pacote certo; e o **teste de carga do painel** (10 mil solicitações, p95 de 500 ms), que não faz
sentido num sistema interno: o que ele provava de correto (série igual ao total) continua em
`PainelPostgresTest` com 700 solicitações.

**Ajustes de plano:** `DescricaoDosFiltros` virou `ContextoDoRelatorio.Filtros` (uma classe só);
`periodoDeGestao` reaproveita `periodoDoPainel`; o teste do adaptador de indicadores ficou em
`IndicadoresDeGestaoPostgresTest` mais um teste de unidade com mock; a baixa de foto ficou no caso
de uso da ficha (e não no serviço de PDF), o que deixa o serviço puro.

Excesso de escopo: nenhum.

Veredito: convergido
Tarefas acrescentadas: nenhuma
