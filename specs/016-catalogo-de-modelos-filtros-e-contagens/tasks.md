# Tarefas — Catálogo de modelos: filtros, busca e contagens

> Ordem de dependência; teste antes da implementação.
> `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

## Fase 1 — Filtros e ordem na listagem (RF-01 a RF-05, RF-08, RF-09)

- [x] T001 Teste de contrato (RF-08): a listagem sem parâmetro novo segue o caminho antigo (`findAll`/`findByFilters`) e nunca chama o catálogo (`ListarModelosUseCaseTest`, `ModeloControllerTest`); o caminho antigo não foi alterado, então não há teste comparando respostas no Postgres
- [x] T002 Testes de `ParametrosDeConsulta`: `q` com menos de 2 caracteres depois de aparado → 400; `ordem` fora de `CRIACAO|CODIGO` → 400 (RF-02, RF-04)
- [x] T003 `ParametrosDeConsulta` valida `q` e `ordem`
- [x] T004 Testes de `ModeloJpaRepositoryPostgresTest`: `tipo` (sem caixa), `q` em código e em descrição (sem acento e sem caixa), `maquinaExata` ("LINHA 2" ≠ "LINHA 20"), soma de todos os filtros, `ordem=CODIGO` ("A-1" v2, "A-1" v1, "B-2"), paginação com filtro (45 + 10) (RF-01 a RF-05, RF-09)
- [x] T005 `FiltrosDeModelos`, `OrdemDeModelos`, consulta nativa em `ModeloJpaRepository` e `ModeloRepositoryAdapter`
- [x] T006 Testes de `ListarModelosUseCaseTest`: monta os filtros e a ordem; sem parâmetro novo usa o caminho antigo (RF-08)
- [x] T007 `ListarModelosUseCase`
- [x] T008 Testes do controller: parâmetros novos repassados; 400 por `q` curto e por `ordem` inválida; regra de acesso igual à listagem atual (RF-10)
- [x] T009 `ModeloController` repassa `tipo`, `q`, `maquinaExata`, `ordem`

## Fase 2 — Contagens (RF-06, RF-07)

- [x] T010 Testes de `ModeloJpaRepositoryPostgresTest` das contagens: `porTipo` sem o filtro de tipo e com os demais; `porMaquina` sem o filtro de máquina; `total` com todos; zeros ausentes; ordem por contagem e depois alfabética; mesma busca `q` da listagem (RF-06, RF-07)
- [x] T011 Consultas agregadas e `ModeloRepositoryAdapter` das contagens
- [x] T012 Testes de `ContarModelosUseCaseTest` e de `ContagensDeModelosResponseTest`
- [x] T013 `ContarModelosUseCase`, `ContagensDeModelosResponse`, `UseCaseConfig`
- [x] T014 Testes do controller: `GET /api/modelos/contagens` com e sem filtros, 400 por `q` curto, acesso (RF-10)
- [x] T015 Rota `GET /api/modelos/contagens` no `ModeloController`
- [x] T016 Teste de desempenho de forma: com 1000 modelos, `contagens` faz no máximo 3 consultas (RNF-01) e a listagem 1 consulta de capas (RNF-02)

## Fase 3 — Posição do modelo (RF-11, RF-12)

- [x] T017 Testes de `ModeloJpaRepositoryPostgresTest` da posição: 33º de 45 com tipo; desempate por versão e por `id`; fora do filtro → nula; coerência com a listagem (a posição *p* cai na página `(p−1)/size`, linha `(p−1)%size`) para todos os modelos de um conjunto (RF-11, RF-12)
- [x] T018 Consulta de posição no `ModeloJpaRepository` e no adaptador
- [x] T019 Testes de `PosicaoDoModeloUseCaseTest` (404, nula, `CRIACAO` recusada) e de `PosicaoDoModeloResponseTest`
- [x] T020 `PosicaoDoModeloUseCase`, resposta e `UseCaseConfig`
- [x] T021 Testes do controller: `GET /{id}/posicao` 200, nula, 404, 400 por `ordem`, acesso (RF-10, RF-11)
- [x] T022 Rota `GET /api/modelos/{id}/posicao`

## Fase 4 — Documentação e validação

- [x] T023 Atualizar `docs/casos-de-uso.md` e a tabela de `openspec/README.md` (linha de `modelos`)
- [x] T024 `make validate` e `make test TESTE='*PostgresTest,FlywayMigrationTest'` verdes, com a saída real no `Convergence`

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01, RF-03, RF-05 | T004, T005 |
| RF-02, RF-04 | T002, T003, T004, T005 |
| RF-06, RF-07 | T010 a T015 |
| RF-08 | T001, T006, T007 |
| RF-09 | T004 |
| RF-11, RF-12 | T017 a T022 |
| RF-10 | T008, T014 |
| RNF-01, RNF-02 | T016 |
| RNF-03, RNF-05 | T024 |
| RNF-04 | T024 (nenhum arquivo em `db/migration`) |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-11-11

Evidência de execução (nesta máquina, sem Docker):

- `make validate` (lint spotless + `mvn clean verify` fora de `*PostgresTest` e `FlywayMigrationTest`):
  1264 testes, 0 falhas, `All coverage checks have been met`, `BUILD SUCCESS`.
- Cobertura por arquivo tocado (JaCoCo, linhas): `ModeloController` 100%, `ListarModelosUseCase` 100%,
  `ContarModelosUseCase` 100%, `PosicaoDoModeloUseCase` 100%, `ParametrosDeConsulta` 100%,
  `ModeloRepositoryAdapter` 98,1%, DTOs e portas novas 100%.
- **Postgres 16 real, fora do Testcontainers:** `ModeloCatalogoPostgresTest` (17 testes) rodou por uma
  cópia temporária, já removida, apontada para um servidor local; todos passam. Como prova de que
  o teste detecta erro, o SQL foi quebrado de propósito em três pontos (versão maior depois na
  posição; máquina exata virando "contém"; contagem por tipo aplicando o filtro de tipo) e cada
  quebra fez exatamente um teste falhar.
- **Não executado como o CI o faz:** o mesmo teste em Testcontainers (exige Docker). O CI do PR fecha isso.

| Requisito | Estado | Evidência |
|---|---|---|
| RF-01 | realizado | `tipo` igual sem caixa: `shouldFilterByTipoIgnoringCase` |
| RF-02 | realizado | `q` em código ou descrição, sem acento e sem caixa; `%` e `_` como texto; 400 abaixo de 2: `shouldSearch…`, `shouldTreatPercent…`, controller |
| RF-03 | realizado | `maquinaExata`: "LINHA 2" ≠ "LINHA 20"; sem ele "contém" |
| RF-04 | realizado | `ordem=CODIGO` (versão maior primeiro) e `CRIACAO`; 400 inválida |
| RF-05 | realizado | `shouldAddUpAllTheFilters` |
| RF-06, RF-07 | realizado | contagens facetadas, só valores > 0, ordem por total e depois alfabética |
| RF-08 | realizado | caminho antigo intacto (ver T001) |
| RF-09 | realizado | 45 + 10, página 2 → 5 linhas, total 45 |
| RF-10 | realizado | `ModeloControllerSegurancaTest`: 401 sem credencial nas duas rotas novas; mesma regra da listagem |
| RF-11 | realizado | posição, nula fora do filtro, 404 e 400 para `CRIACAO` |
| RF-12 | realizado | para os 45 modelos, a posição *p* cai na página `(p−1)/20`, linha `(p−1)%20` da listagem |
| RNF-01 | realizado | `contar` faz ≤ 3 consultas com 300 modelos (estatísticas do Hibernate) |
| RNF-02 | realizado | a listagem não mudou: continua 1 consulta de capas |
| RNF-03 | realizado | cobertura acima |
| RNF-04 | realizado | nenhum arquivo em `db/migration` |
| RNF-05 | realizado | `make validate` verde |

Achados e desvios:

- A ordem `CRIACAO` da listagem de sempre ordena só por `criadoEm`, sem desempate: duas linhas com o
  mesmo instante podem trocar de página entre uma consulta e outra. A ordem nova (`CRIACAO` do
  catálogo e `CODIGO`) já desempata por `id`; a listagem de sempre ficou como está (RF-08) e
  isso fica registrado como dívida.
- Erro de processo meu durante a execução: ao desfazer uma mutação de teste usei `git checkout` num
  arquivo com mudanças não comitadas e perdi as edições do adapter; foram reaplicadas e
  comitadas na hora, e os testes foram rodados de novo.
