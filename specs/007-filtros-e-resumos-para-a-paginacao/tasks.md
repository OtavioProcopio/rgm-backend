# Tarefas — Filtros e resumos para a paginação

> Ordem de dependência. `[P]` marca tarefa paralelizável. Teste vem antes da implementação
> que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

## Fase 1 — Domínio

- [ ] T001 Criar `TEST/core/domain/ports/repositories/ResumoSolicitacoesModeloTest.java`: intervalo com 2 ou mais solicitações; vazio com menos de 2
- [ ] T002 Criar `MAIN/core/domain/ports/repositories/ResumoSolicitacoesModelo.java`, `ResumoModelos.java` e `QuantidadePorMaquina.java`; acrescentar `resumir()` a `ModeloRepository` e `resumirPorModelo` e `emAberto` a `SolicitacaoRepository`

## Fase 2 — Aplicação

- [ ] T003 Testes em `TEST/core/application/usecases/solicitacao/ListarSolicitacoesUseCaseTest.java`: "em aberto" ligado é repassado; ligado sozinho usa a consulta de filtros; desligado sozinho lista tudo
- [ ] T004 Alterar `MAIN/core/application/usecases/solicitacao/ListarSolicitacoesUseCase.java`
- [ ] T005 [P] Criar `TEST/.../modelo/ObterResumoModelosUseCaseTest.java` e `MAIN/.../modelo/ObterResumoModelosUseCase.java`
- [ ] T006 [P] Criar `TEST/.../modelo/ObterResumoSolicitacoesModeloUseCaseTest.java` (resumo de modelo existente; "não encontrado" para inexistente) e `MAIN/.../modelo/ObterResumoSolicitacoesModeloUseCase.java`

## Fase 3 — Adapters e infra

- [ ] T007 Testes em `TEST/adapter/out/persistence/repository/ModeloJpaRepositoryTest.java`: contagens; por máquina ordenado; sem modelos
- [ ] T008 Alterar `MAIN/adapter/out/persistence/repository/ModeloJpaRepository.java`
- [ ] T009 Criar `TEST/adapter/out/persistence/repository/SolicitacaoJpaRepositoryTest.java`: resumo por modelo (contagens, datas, média só das concluídas; modelo sem solicitações)
- [ ] T010 Criar `TEST/adapter/out/persistence/repository/SolicitacaoJpaRepositoryPostgresTest.java`: "em aberto" ligado, desligado e combinado; canceladas e concluídas por data de encerramento; resumo por modelo
- [ ] T011 Alterar `MAIN/adapter/out/persistence/repository/SolicitacaoJpaRepository.java`
- [ ] T012 Testes em `ModeloRepositoryAdapterTest.java` e `SolicitacaoRepositoryAdapterTest.java`; alterar os dois adapters
- [ ] T013 Testes em `TEST/adapter/in/web/modelo/ModeloControllerTest.java`: resumo de modelos; resumo de um modelo; 404; ficha em PDF recebe os tempos do resumo
- [ ] T014 Criar os dois DTOs de resposta; alterar `MAIN/adapter/in/web/modelo/ModeloController.java`; registrar os casos de uso em `MAIN/adapter/config/UseCaseConfig.java`
- [ ] T015 Testes em `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java`: `emAberto` chega ao caso de uso na listagem e no relatório
- [ ] T016 Alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java`

## Fase 4 — Integração e BDD

- [ ] T017 Alterar `Makefile`: alvos sem Testcontainers excluem `*PostgresTest`
- [ ] T018 Atualizar `docs/casos-de-uso.md` e a tabela de `openspec/README.md`
- [ ] T019 `make validate` verde e `make test-all` verde

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01 | T003, T004, T010, T011, T015, T016 |
| RF-02 | T010, T011 |
| RF-03 | T003, T004, T010 |
| RF-04 | T010, T011 |
| RF-05 | T005, T007, T008, T012, T013, T014 |
| RF-06 | T006, T009, T010, T011, T012, T013, T014 |
| RF-07 | T006, T013 |
| RF-08 | T014 |
| RF-09 | T015, T016 |
| RF-10 | T009, T010, T011 |
| RF-11 | T001, T002, T012 |
| RF-12 | T013, T014 |
| RNF-01 | T006, T008, T011 |
| RNF-02 | T014, T016 |
| RNF-03 | T019 |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.
