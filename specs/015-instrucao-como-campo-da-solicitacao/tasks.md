# Tarefas — Instrução da triagem como campo da solicitação

> Ordem de dependência; teste antes da implementação. A fase 1 não altera asserção nenhuma.
> `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

## Fase 1 — Refatoração sem mudança de comportamento (RNF-01 a RNF-03)

> Registro: um primeiro rascunho desta fase (T001 e T002) foi escrito antes da revisão da spec e
> está em `git stash`, não commitado. Retoma-se dele depois da aprovação, refazendo o que mudar.

- [ ] T001 Teste `SolicitacaoCopiaTest` (roda verde sobre o código atual: protege a refatoração): solicitação com todos os campos preenchidos; para cada transição (`triar`, `enviarParaValidacao`, `devolver`, `encerrar`, `cancelar`, `editar`) só mudam os campos declarados; `usingRecursiveComparison`
- [ ] T002 `Solicitacao.Builder` e `toBuilder()`; construtor privado; reescrever as 9 cópias e as fábricas `abrir` e `abrirCriacao`; `SolicitacaoTest` e T001 verdes
- [ ] T003 `SolicitacaoMapper.toDomain` pelo builder; `MapperRoundtripTest` verde
- [ ] T004 Teste e `TEST/support/SolicitacaoTestBuilder` com padrões válidos para cada status (prioridade em `EM_ANDAMENTO` em diante, `concluidaEm` e `comentarioFinal` em `CONCLUIDA`, `canceladaEm` em `CANCELADA`)
- [ ] T005 Migrar os ~50 usos de `new Solicitacao(` dos testes para o builder, arquivo a arquivo, sem tocar asserção; suíte verde ao fim
- [ ] T006 Conferir RNF-01 e RNF-02 por busca: `new Solicitacao(` em produção ≤ 3 pontos; em teste só no builder e no teste do construtor
- [ ] T007 `make validate` verde (commit só da refatoração)

## Fase 2 — Campo e triagem (RF-01 a RF-09)

- [ ] T008 Teste de migração: coluna `instrucao` existe, é anulável, e linhas existentes ficam nulas (RF-01, RNF-05)
- [ ] T009 `V10__solicitacao_instrucao.sql`
- [ ] T010 Testes de `SolicitacaoTest`: `triar` grava a instrução aparada; recusa em branco, nula e 2001; recusa `EM_ANDAMENTO`, `EM_VALIDACAO`, `CONCLUIDA`, `CANCELADA` com `TransicaoStatusInvalidaException`; `A_FAZER` nasce sem instrução (RF-02, RF-03, RF-04)
- [ ] T011 `Solicitacao.instrucao`, `getInstrucao`, `triar(prioridade, instrucao, agora)` e a guarda de status; `SolicitacaoCopiaTest` passa a cobrir o campo (RF-08)
- [ ] T012 Teste de ida e volta no mapper e no Postgres (`instrucao` com acento, quebra de linha e 2000 caracteres) (RF-01)
- [ ] T013 `SolicitacaoJpaEntity` e `SolicitacaoMapper`
- [ ] T014 Testes do caso de uso: a solicitação salva leva a instrução; duas atividades (atribuição e status) e nenhum comentário; triar fora de `A_FAZER` não grava atribuição nem atividade; regras antigas (RF-03, RF-04, RF-05)
- [ ] T015 `TriarSolicitacaoUseCase.Input.instrucao`; remover a atividade de comentário
- [ ] T016 Testes do controller: 400 sem `instrucao`, em branco e com 2001; 200 com 2000; 400 com o corpo antigo (`descricao`); 409 fora de `A_FAZER`; perfil sem permissão continua 403 (RF-02, RF-04, RF-06)
- [ ] T017 `TriarSolicitacaoRequest` e `SolicitacaoController`
- [ ] T018 Testes de `SolicitacaoResponseTest`: `instrucao` presente e nula; listagem e evento "triada" a trazem (RF-07)
- [ ] T019 `SolicitacaoResponse`
- [ ] T020 Teste de `ModeloPdfServiceTest`: PDF lido de volta mostra "Instrução: …" da solicitação que a tem e nenhuma linha na que não tem (RF-09)
- [ ] T021 `ModeloPdfService`
- [ ] T022 Teste: editar, devolver, enviar para validação e encerrar preservam a instrução pelo caso de uso, não só pelo agregado (RF-08)

## Fase 3 — Edição da instrução (RF-10 a RF-15)

- [ ] T023 Testes de `SolicitacaoTest`: `editarInstrucao` grava a instrução aparada e devolve nova instância; texto igual (depois de aparado) devolve a mesma instância; nula, em branco e 2001 caracteres → `ValidationException`; cada status ≠ `EM_ANDAMENTO` → `ConflitoException`; após `devolver` (volta a `EM_ANDAMENTO`) é permitido (RF-10, RF-11, RF-13)
- [ ] T024 `Solicitacao.editarInstrucao`; `SolicitacaoCopiaTest` cobre: só `instrucao` e `atualizadaEm` mudam (RF-08, RF-15)
- [ ] T025 Testes de `AtividadeSolicitacao.instrucaoAlterada` (tipo, texto anterior em `comentario`, autor, data) e de ida e volta do tipo novo no repositório (RF-13)
- [ ] T026 `TipoAtividadeSolicitacao.INSTRUCAO_ALTERADA` e a fábrica
- [ ] T027 Testes de `AcoesPermitidasSolicitacao`: tabela perfil × status — `EDITAR_INSTRUCAO` só para gestor e administrador ativos com `EM_ANDAMENTO`; nunca para operador (responsável ou não), externo ou inativo (RF-12, RF-14)
- [ ] T028 `AcaoSolicitacao.EDITAR_INSTRUCAO` e a regra
- [ ] T029 Testes do `EditarInstrucaoSolicitacaoUseCaseTest`: gestor e administrador editam e gravam solicitação mais atividade com o texto anterior; texto igual não grava nada; operador → `NaoAutorizadoException`; usuário inativo recusado; solicitação inexistente → 404; fora de `EM_ANDAMENTO` não grava atividade (RF-10 a RF-13)
- [ ] T030 `EditarInstrucaoSolicitacaoUseCase` e `UseCaseConfig`
- [ ] T031 Testes do controller: `PATCH /instrucao` 200 com a solicitação completa; 400 (ausente, em branco, 2001); 200 com 2000; 409 por status; 403 operador; 401 sem token; 404; eventos `solicitacao` e `solicitacao_atividade` publicados com tipo `instrucao_editada` (RF-10 a RF-12, RF-15)
- [ ] T032 `EditarInstrucaoRequest` e a rota no `SolicitacaoController`
- [ ] T033 Teste de Postgres: duas edições partindo da mesma versão — a segunda falha e o texto final é o da primeira (RF-15)

## Fase 4 — Documentação e validação

- [ ] T034 Atualizar `docs/casos-de-uso.md` (UC-03 e a edição da instrução), `docs/modelo_dados.md` e a tabela de `openspec/README.md`; ajustar a linha da spec 014 (o corpo passou a `instrucao`)
- [ ] T035 `make format`, `make lint`, `make validate` e `make test TESTE='*PostgresTest,FlywayMigrationTest'` verdes, com a saída real no `Convergence`

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01 | T008, T009, T012, T013 |
| RF-02 | T010, T011, T016, T017 |
| RF-03 | T010, T011, T014, T015 |
| RF-04 | T010, T011, T014, T016 |
| RF-05 | T014, T015 |
| RF-06 | T016, T017 |
| RF-07 | T018, T019 |
| RF-08 | T001, T011, T022, T024 |
| RF-09 | T020, T021 |
| RF-10 | T023, T024, T029, T030, T031, T032 |
| RF-11 | T023, T024, T029, T031 |
| RF-12 | T027, T029, T031 |
| RF-13 | T023, T025, T026, T029 |
| RF-14 | T027, T028 |
| RF-15 | T024, T031, T033 |
| RNF-01, RNF-02 | T002, T004, T005, T006 |
| RNF-03 | T005, T007 |
| RNF-04, RNF-06 | T035 |
| RNF-05 | T008, T009, T026 |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.
