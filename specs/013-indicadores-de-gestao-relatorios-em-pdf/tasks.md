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

- [ ] T010 Criar `TEST/adapter/out/persistence/repository/IndicadoresDeGestaoJpaRepositoryPostgresTest.java`: prazo (1 minuto antes e 1 depois, por prioridade, sem prioridade contada à parte); mediana 5,5 h e p90 9,1 h com 10 concluídas; sem concluída devolve vazio; faixas de idade somam o total em aberto; abertas sem responsável; etapas (2 h, 5 h e 1 h; devolvida soma 3 h + 2 h; transição para cancelada não conta como espera até iniciar); devolução 1 em 2 envios; por máquina (abertas, atrasadas, tempo médio, criação fora); reincidente com 3 reparos sim e com 2 não; carga com dois responsáveis e total de distintas
- [ ] T011 Criar `MAIN/adapter/out/persistence/repository/IndicadoresDeGestaoJpaRepository.java` e as portas e registros em `MAIN/core/domain/ports/repositories/` (T010 verde)
- [ ] T012 Criar `TEST/adapter/out/persistence/IndicadoresDeGestaoRepositoryAdapterTest.java`: mapeia as linhas nativas para os registros da porta, inclusive `null` onde não há dado
- [ ] T013 Criar `MAIN/adapter/out/persistence/IndicadoresDeGestaoRepositoryAdapter.java` (T012 verde)
- [ ] T014 Criar `TEST/core/application/usecases/solicitacao/ObterIndicadoresDeGestaoUseCaseTest.java`: janela de 7, 30 e 90 dias no fuso; porcentagens calculadas a partir das contagens; taxa de devolução nula sem envio; nenhuma busca por lista é chamada (RNF-02)
- [ ] T015 Criar `MAIN/core/application/usecases/solicitacao/ObterIndicadoresDeGestaoUseCase.java` (T014 verde)
- [ ] T016 Criar `TEST/adapter/in/web/dto/response/IndicadoresDeGestaoResponseTest.java` e `MAIN/adapter/in/web/dto/response/IndicadoresDeGestaoResponse.java`
- [ ] T017 Alterar `TEST/adapter/in/web/ParametrosDeConsultaTest.java` e `MAIN/adapter/in/web/ParametrosDeConsulta.java`: `periodoDeGestao` aceita 7, 30, 90; 15 lança `ValidationException` citando os valores
- [ ] T018 Criar `TEST/adapter/in/web/solicitacao/IndicadoresDeGestaoControllerTest.java` e `MAIN/adapter/in/web/solicitacao/IndicadoresDeGestaoController.java`: 200 com o formato; 400 para 15 dias; 403 para operador; 401 sem token (usar o modelo de `PainelPorPeriodoControllerTest`)
- [ ] T019 Alterar `MAIN/adapter/config/UseCaseConfig.java` (bean do caso de uso) e `TEST/adapter/in/web/solicitacao/MetricasContratoV15Test.java` (campos antigos intactos)

## Fase 3 — PDFs fiéis à tela (RF-15 a RF-22)

- [ ] T020 Criar `TEST/adapter/in/web/DescricaoDosFiltrosTest.java` e `MAIN/adapter/in/web/DescricaoDosFiltros.java`: rótulo legível por filtro; "sem filtros" quando vazio; ignora filtro nulo
- [ ] T021 Alterar `TEST/adapter/out/report/SolicitacaoPdfServiceTest.java` e `MAIN/adapter/out/report/SolicitacaoPdfService.java`: o PDF lido de volta traz os filtros, o total e, quando cortado, "exibindo N de M"
- [ ] T022 Alterar `TEST/adapter/out/report/ModeloPdfServiceTest.java` e `MAIN/adapter/out/report/ModeloPdfService.java` (lista): filtros, total, aviso e coluna Tipo; ranking: ordenação e filtros no cabeçalho
- [ ] T023 Alterar `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java` e `SolicitacaoController.java`: `relatorio` aceita `q`, `sort`, `dir`, usa o mesmo `buildInput`, pede 5000 linhas e repassa o total; ranking repassa filtros
- [ ] T024 Alterar `TEST/adapter/in/web/modelo/ModeloControllerTest.java` e `ModeloController.java`: `relatorio` da lista sem corte silencioso de 1000 e com filtros e total
- [ ] T025 Alterar `TEST/core/domain/ports`/adaptador de atividades: `findBySolicitacaoIdIn` em `AtividadeSolicitacaoRepository`, `AtividadeSolicitacaoRepositoryAdapter` e `AtividadeSolicitacaoJpaRepository` com `TEST/adapter/out/persistence/AtividadeSolicitacaoRepositoryAdapterTest.java`
- [ ] T026 Alterar `TEST/adapter/out/storage/MinioStorageServiceTest.java` e `MAIN/adapter/out/storage/MinioStorageService.java` e `MAIN/core/domain/ports/services/StorageService.java`: `baixar` devolve os bytes do objeto e falha com `RuntimeException`
- [ ] T027 Criar `TEST/core/application/usecases/modelo/ObterFichaDoModeloUseCaseTest.java` e `MAIN/core/application/usecases/modelo/ObterFichaDoModeloUseCase.java`: monta modelo, eventos, solicitações, atividades em uma chamada em lote, nomes, resumo e fotos (capa primeiro); modelo inexistente lança `RecursoNaoEncontradoException`; o número de chamadas ao repositório é o mesmo com 1 e com 50 solicitações (RNF-04)
- [ ] T028 Alterar `TEST/adapter/out/report/ModeloPdfServiceTest.java` e `ModeloPdfService.java` (ficha): três fotos com a capa em destaque; foto que falha ao baixar vira "indisponível" sem derrubar a ficha; atividades aparecem sob cada solicitação
- [ ] T029 Alterar `ModeloController.fichaDoModelo`/`gerarFichaModelo` para usar o caso de uso e passar as fotos; `UseCaseConfig` com o bean novo; `TEST/adapter/in/web/modelo/ModeloControllerTest.java`

## Fase 4 — Documentação e validação

- [ ] T030 Atualizar `docs/diagramas.md`, `docs/modelo_dados.md`, `docs/casos-de-uso.md` e `openspec/README.md` (indicadores, PDFs, tabela legada, remoções)
- [ ] T031 `make format`, `make lint`, `make validate` e `make test TESTE='*PostgresTest,FlywayMigrationTest'` verdes, com a saída real no `Convergence`

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
