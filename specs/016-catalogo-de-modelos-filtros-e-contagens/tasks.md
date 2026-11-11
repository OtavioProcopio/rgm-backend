# Tarefas — Catálogo de modelos: filtros, busca e contagens

> Ordem de dependência; teste antes da implementação.
> `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

## Fase 1 — Filtros e ordem na listagem (RF-01 a RF-05, RF-08, RF-09)

- [ ] T001 Teste de contrato (RF-08): a listagem sem parâmetro novo devolve a mesma página, ordem e totais que a consulta antiga, no Postgres real
- [ ] T002 Testes de `ParametrosDeConsulta`: `q` com menos de 2 caracteres depois de aparado → 400; `ordem` fora de `CRIACAO|CODIGO` → 400 (RF-02, RF-04)
- [ ] T003 `ParametrosDeConsulta` valida `q` e `ordem`
- [ ] T004 Testes de `ModeloJpaRepositoryPostgresTest`: `tipo` (sem caixa), `q` em código e em descrição (sem acento e sem caixa), `maquinaExata` ("LINHA 2" ≠ "LINHA 20"), soma de todos os filtros, `ordem=CODIGO` ("A-1" v2, "A-1" v1, "B-2"), paginação com filtro (45 + 10) (RF-01 a RF-05, RF-09)
- [ ] T005 `FiltrosDeModelos`, `OrdemDeModelos`, consulta nativa em `ModeloJpaRepository` e `ModeloRepositoryAdapter`
- [ ] T006 Testes de `ListarModelosUseCaseTest`: monta os filtros e a ordem; sem parâmetro novo usa o caminho antigo (RF-08)
- [ ] T007 `ListarModelosUseCase`
- [ ] T008 Testes do controller: parâmetros novos repassados; 400 por `q` curto e por `ordem` inválida; regra de acesso igual à listagem atual (RF-10)
- [ ] T009 `ModeloController` repassa `tipo`, `q`, `maquinaExata`, `ordem`

## Fase 2 — Contagens (RF-06, RF-07)

- [ ] T010 Testes de `ModeloJpaRepositoryPostgresTest` das contagens: `porTipo` sem o filtro de tipo e com os demais; `porMaquina` sem o filtro de máquina; `total` com todos; zeros ausentes; ordem por contagem e depois alfabética; mesma busca `q` da listagem (RF-06, RF-07)
- [ ] T011 Consultas agregadas e `ModeloRepositoryAdapter` das contagens
- [ ] T012 Testes de `ContarModelosUseCaseTest` e de `ContagensDeModelosResponseTest`
- [ ] T013 `ContarModelosUseCase`, `ContagensDeModelosResponse`, `UseCaseConfig`
- [ ] T014 Testes do controller: `GET /api/modelos/contagens` com e sem filtros, 400 por `q` curto, acesso (RF-10)
- [ ] T015 Rota `GET /api/modelos/contagens` no `ModeloController`
- [ ] T016 Teste de desempenho de forma: com 1000 modelos, `contagens` faz no máximo 3 consultas (RNF-01) e a listagem 1 consulta de capas (RNF-02)

## Fase 3 — Documentação e validação

- [ ] T017 Atualizar `docs/casos-de-uso.md` e a tabela de `openspec/README.md` (linha de `modelos`)
- [ ] T018 `make validate` e `make test TESTE='*PostgresTest,FlywayMigrationTest'` verdes, com a saída real no `Convergence`

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01, RF-03, RF-05 | T004, T005 |
| RF-02, RF-04 | T002, T003, T004, T005 |
| RF-06, RF-07 | T010 a T015 |
| RF-08 | T001, T006, T007 |
| RF-09 | T004 |
| RF-10 | T008, T014 |
| RNF-01, RNF-02 | T016 |
| RNF-03, RNF-05 | T018 |
| RNF-04 | T018 (nenhum arquivo em `db/migration`) |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.
