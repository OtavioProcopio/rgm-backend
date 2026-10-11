# Tarefas — Painel de métricas por período e ordenação da listagem

> Ordem de dependência. `[P]` marca tarefa paralelizável (não toca arquivo de outra `[P]`
> da mesma fase). Teste vem antes da implementação que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`,
`RES` = `app/src/main/resources`.

Desvio herdado (feature 001): o projeto não tem `app/tests/bdd` nem alvo `make bdd`. Os
critérios de aceite da spec são provados por testes de controller, de caso de uso e de
consulta em Postgres, listados abaixo; a rastreabilidade mostra qual teste cobre qual cenário.

Todo teste novo segue o Princípio 3: `// Arrange`, `// Act`, `// Assert`, um comportamento por
teste, nome `shouldXWhenY`. As consultas SQL são provadas em `*PostgresTest` (rodam no
`make validate`; o `make test` padrão os exclui).

## Fase 1 — Ordenação da listagem (RF-01 a RF-04)

- [x] T001 Alterar `TEST/adapter/in/web/ParametrosDeConsultaTest.java`: `ordenacao` devolve critério e sentido para `criadaEm|prazoLimite|prioridade` e `asc|desc`; sem `sort` devolve a ordem padrão (criação crescente); `sort=titulo` e `dir=up` lançam `ValidationException` com o parâmetro e os valores aceitos em português
- [x] T002 Alterar `MAIN/adapter/in/web/ParametrosDeConsulta.java`: método `ordenacao(sort, dir)` (T001 verde)
- [x] T003 [P] Criar `TEST/core/domain/ports/repositories/OrdenacaoSolicitacaoTest.java`: padrão é criação crescente; igualdade e valores
- [x] T004 [P] Criar `MAIN/core/domain/model/enums/CriterioOrdenacaoSolicitacao.java` e `MAIN/core/domain/ports/repositories/OrdenacaoSolicitacao.java` (T003 verde)
- [x] T005 Alterar `TEST/core/application/usecases/solicitacao/ListarSolicitacoesUseCaseTest.java`: a ordenação do `Input` chega ao repositório; sem ordenação usa o padrão
- [x] T006 Alterar `MAIN/core/domain/ports/repositories/SolicitacaoRepository.java` e `MAIN/core/application/usecases/solicitacao/ListarSolicitacoesUseCase.java`: `findByFilters` recebe `OrdenacaoSolicitacao` (T005 verde)
- [x] T007 Alterar `TEST/adapter/out/persistence/repository/SolicitacaoJpaRepositoryPostgresTest.java`: prazo crescente traz a mais vencida primeiro e sem prioridade por último, também em `desc`; criação decrescente traz a mais nova primeiro; sem ordenação, a mais antiga primeiro; prioridade crescente traz URGENTE antes de BAIXA; duas páginas consecutivas não repetem nem perdem item (RNF-06)
- [x] T008 Alterar `MAIN/adapter/out/persistence/repository/SolicitacaoJpaRepository.java` e `MAIN/adapter/out/persistence/SolicitacaoRepositoryAdapter.java` (e `TEST/adapter/out/persistence/SolicitacaoRepositoryAdapterTest.java`): `ORDER BY` por critério com desempate por `criada_em` e `id`, `NULLS LAST` (T007 verde)
- [x] T009 Alterar `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java`: `sort` e `dir` chegam ao caso de uso; sem `sort`, o padrão; `sort` inválido e `dir` inválido respondem 400 com a mensagem; operador pode ordenar a própria listagem
- [x] T010 Alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java`: `sort` e `dir` na listagem (T009 verde)

## Fase 2 — Criação fora dos tempos do modelo (RF-14)

- [x] T011 Alterar `TEST/core/domain/ports/repositories/ResumoSolicitacoesModeloTest.java`: o intervalo usa a contagem de manutenções, não o total; modelo só com criação tem tempo e intervalo nulos; o total continua contando a criação
- [x] T012 Alterar `TEST/adapter/out/persistence/repository/SolicitacaoJpaRepositoryPostgresTest.java`: modelo com uma criação concluída e nenhuma manutenção não aparece no ranking; com uma manutenção concluída aparece sem o tempo da criação; o resumo exclui a criação do tempo médio e do intervalo e a inclui no total
- [x] T013 Alterar `MAIN/core/domain/ports/repositories/ResumoSolicitacoesModelo.java`, `MAIN/adapter/out/persistence/repository/SolicitacaoJpaRepository.java` e `MAIN/adapter/out/persistence/SolicitacaoRepositoryAdapter.java`: `tipo <> 'CRIACAO'` nas expressões de tempo e de intervalo (T011 e T012 verdes)

## Fase 3 — Fuso, limite e agregação do histórico (RF-08, RF-09, RF-11, RF-15)

- [x] T014 Criar `TEST/adapter/config/FabricaConfigTest.java`: o `ZoneId` padrão é `America/Sao_Paulo`; propriedade `rgm.fabrica.fuso` o troca; valor inválido falha na subida
- [x] T015 Criar `MAIN/adapter/config/FabricaConfig.java` e alterar `RES/application.properties` e `TEST/adapter/config/ConfiguracaoDeProducaoTest.java` (T014 verde)
- [x] T016 Alterar `TEST/adapter/out/persistence/repository/SolicitacaoJpaRepositoryPostgresTest.java`: série por dia no fuso (solicitação das 22h cai no dia local; 00h00 e 23h59 nas bordas); concluídas pela data de conclusão; filtro de modelo na consulta; semana de segunda a domingo
- [x] T017 Alterar `TEST/core/application/usecases/solicitacao/ObterHistoricoMetricasUseCaseTest.java`: monta a série a partir das linhas agregadas sem listar solicitações (nenhuma busca por lista é chamada); média em segundos; `slaMediaHoras` preservado; teto de 365 dias
- [x] T018 Alterar `MAIN/core/domain/ports/repositories/SolicitacaoRepository.java`, `MAIN/adapter/out/persistence/repository/SolicitacaoJpaRepository.java` e `MAIN/adapter/out/persistence/SolicitacaoRepositoryAdapter.java`: consultas agregadas por dia e por semana (T016 verde)
- [x] T019 Alterar `MAIN/core/application/usecases/solicitacao/ObterHistoricoMetricasUseCase.java`: usa a porta agregada e o fuso; formato da resposta inalterado (T017 verde)
- [x] T020 Alterar `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java` e `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java`: `dias` do histórico fora de 1 a 365 responde 400 em português
- [x] T021 Criar `TEST/adapter/in/web/solicitacao/MetricasContratoV15Test.java`: os nomes dos campos de `/metricas` e `/metricas/historico` (inclusive `slaMediaHoras` e `slaGlobalMediaHoras`) permanecem os de antes (RF-13, RNF-05)

## Fase 4 — Painel por período (RF-05 a RF-07, RF-10 a RF-12)

- [x] T022 Alterar `TEST/adapter/in/web/ParametrosDeConsultaTest.java`: `periodoDoPainel` aceita 7, 30 e 90; recusa 0, 15, 91 e negativo com 400 em português listando 7, 30 e 90
- [x] T023 Alterar `MAIN/adapter/in/web/ParametrosDeConsulta.java`: `periodoDoPainel(dias)` (T022 verde)
- [x] T024 Alterar `TEST/adapter/out/persistence/repository/SolicitacaoJpaRepositoryPostgresTest.java`: totais da janela (criadas, concluídas, canceladas, tempo médio em segundos pela data de conclusão); abertas e atrasadas ao fim de cada dia pela regra do RF-10; abertas e atrasadas agora; filtro de modelo; janela anterior com os mesmos totais; 30 minutos de resolução dão 1800 s
- [x] T025 Criar `MAIN/core/domain/ports/repositories/PainelAgregado.java` e alterar `MAIN/core/domain/ports/repositories/SolicitacaoRepository.java`, `MAIN/adapter/out/persistence/repository/SolicitacaoJpaRepository.java` e `MAIN/adapter/out/persistence/SolicitacaoRepositoryAdapter.java`: consultas agregadas do painel (T024 verde)
- [x] T026 Criar `TEST/core/application/usecases/solicitacao/ObterPainelPorPeriodoUseCaseTest.java`: monta janela atual e anterior de mesmo tamanho, adjacentes; chama a porta uma vez por janela; não lista solicitações (RNF-02); semana quando o período passa de 30 dias; repassa o modelo
- [x] T027 Criar `MAIN/core/application/usecases/solicitacao/ObterPainelPorPeriodoUseCase.java` (T026 verde)
- [x] T028 Criar `TEST/adapter/in/web/solicitacao/PainelPorPeriodoControllerTest.java`: 200 com atual e anterior; `dias` ausente usa 30; 15 responde 400; operador 403 com corpo de erro; sem credencial 401; gestor e administrador 200; `modeloId` repassado
- [x] T029 Criar `MAIN/adapter/in/web/dto/response/PainelPorPeriodoResponse.java` e alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java`: rota `GET /api/solicitacoes/metricas/painel` (T028 verde)
- [x] T030 Alterar `TEST/adapter/config/SecurityConfigTest.java`: a rota nova está sob a regra de gestor e administrador, inclusive `HEAD`

## Fase 5 — Medição, documentação e fechamento

- [x] T031 Medir o painel com 10 000 solicitações sintéticas em Postgres (teste `*PostgresTest` ou sonda registrada em `/root/rgm/evidencias/`): p95 ≤ 500 ms; se estourar, criar a migração de índice e repetir (RNF-01)
- [x] T032 Alterar `docs/casos-de-uso.md` (UC-16: painel por período, ordenação, fuso e a aproximação do atraso por dia) e `openspec/README.md` (linha da spec 011)
- [x] T033 Sonda em servidor real: ordenação, painel (atual e anterior), 400, 403 para operador, contrato antigo; saída guardada em `/root/rgm/evidencias/`
- [x] T034 `make format`, `make lint` e `make validate` verdes, com a saída real

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01 | T001, T002, T003, T004, T009, T010 |
| RF-02 | T001, T002, T009, T010 |
| RF-03 | T007, T008 |
| RF-04 | T005, T006, T007, T008, T009 |
| RF-05 | T026, T027, T028, T029 |
| RF-06 | T016, T018, T024, T025 |
| RF-07 | T017, T024, T025 |
| RF-08 | T014, T015, T016, T018, T019 |
| RF-09 | T020, T022, T023, T028 |
| RF-10 | T024, T025 |
| RF-11 | T016, T018, T024, T025 |
| RF-12 | T028, T030 |
| RF-13 | T021 |
| RF-14 | T011, T012, T013 |
| RF-15 | T016, T017, T018, T019, T020 |
| RNF-01 | T031 |
| RNF-02 | T017, T026 |
| RNF-03 | T016, T024 |
| RNF-04 | T034 |
| RNF-05 | T021 |
| RNF-06 | T007 |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-10-10

Confronto do código entregue com `spec.md` (RF-01 a RF-15, RNF-01 a RNF-06) e o `plan.md`.

| Requisito | Estado | Evidência |
|---|---|---|
| RF-01 ordenar por `sort`/`dir` | realizado | `ParametrosDeConsulta.ordenacao`, `OrdenacaoSolicitacao`, `SolicitacaoJpaRepository` (`ORDER BY` por critério); sonda 011: criação, prazo e prioridade nos dois sentidos |
| RF-02 valor inválido 400 | realizado | testes de `ParametrosDeConsultaTest` e do controller; sonda: `sort=titulo` e `dir=up` respondem 400 com os valores aceitos |
| RF-03 prazo: mais vencida primeiro, sem prioridade no fim | realizado | `SolicitacaoJpaRepositoryPostgresTest` (asc e desc, mutação do `NULLS LAST` provada); sonda `[b,c,a]` e `[c,b,a]` |
| RF-04 ordem padrão e paginação estável | realizado | desempate `criada_em, id`; teste Postgres de páginas consecutivas; sonda sem `sort` = `[a,b,c]` |
| RF-05 painel com período e anterior | realizado | `ObterPainelPorPeriodoUseCase`, rota `/metricas/painel`; sonda 29/29 |
| RF-06 concluída pela conclusão, criada pela criação | realizado | testes Postgres do painel (mutação "por criada_em" falha); sonda |
| RF-07 tempo em segundos | realizado | 30 min = 1800,0 em teste Postgres e no use case |
| RF-08 dias no fuso da fábrica | realizado | `FabricaConfig` (`rgm.fabrica.fuso`), testes 22h, 23h59 e 00h00; sonda: último ponto = `10/10` local |
| RF-09 período do painel (7/30/90) e do histórico (1 a 365) | realizado | controller e `ParametrosDeConsultaTest`; sonda: 15 → 400, `dias=100000` e `0` → 400 |
| RF-10 abertas e atrasadas por dia e agora | realizado | `painelSerie`/`painelAgora`; 6 mutações provadas; prioridade de hoje (Esclarecimento) |
| RF-11 filtro de modelo na consulta | realizado | testes Postgres (mutação do filtro falha); sonda filtra por modelo |
| RF-12 403 ao operador, 401 ao anônimo | realizado | `SecurityConfigTest` (inclui `HEAD`), `PainelPorPeriodoControllerTest`; sonda |
| RF-13 formato antigo preservado | realizado | `MetricasContratoV15Test`; sonda confere `/metricas` e `/metricas/historico` |
| RF-14 criação fora dos tempos | realizado | `ResumoSolicitacoesModeloTest`; testes Postgres com 4 mutações; total continua contando a criação |
| RF-15 histórico no fuso, com teto e agregado | realizado | `ObterHistoricoMetricasUseCaseTest` (sem busca por lista); `dias` 1 a 365 |
| RNF-01 p95 ≤ 500 ms com 10 000 linhas | realizado | `PainelDesempenhoPostgresTest`: p95 de 66 a 100 ms (antes da reescrita da série, 1687 ms em `dias=30`) |
| RNF-02 nenhuma entidade carregada | realizado | `verifyNoMoreInteractions` no caso de uso do painel e do histórico |
| RNF-03 exatidão | realizado | casos de conta conhecida nos testes Postgres; equivalência da série reescrita provada pontualmente contra a versão anterior |
| RNF-04 cobertura e `validate` | realizado | `make validate` EXIT=0, 995 testes, "All coverage checks have been met" |
| RNF-05 compatibilidade | realizado | nenhum campo removido; `MetricasContratoV15Test` |
| RNF-06 paginação estável | realizado | teste Postgres de duas páginas consecutivas |

**Medições:** `make validate` EXIT=0 (995 testes; **exclui** `*PostgresTest` e `FlywayMigrationTest`);
`make test TESTE='*PostgresTest,FlywayMigrationTest'`: 76 testes, 0 falhas (inclui a medição de
desempenho, 7 testes); sonda em servidor real 29 de 29
(`/root/rgm/evidencias/integracao-2026-10-10/sonda-011.txt`).

**Achados e desvios (incorporados ou registrados):**
- O plano afirmava que os `*PostgresTest` rodam no `validate`; é falso (o Makefile os exclui).
  Corrigido no plano; a esteira do GitHub (`./mvnw verify`) os roda.
- `findByCriadaEmBetween` e `findByStatusAndCriadaEmBetween` ficaram sem uso e foram removidos
  da porta, do adaptador e do JPA, com seus testes.
- O histórico segue agrupando por data de **criação** (RF-15 pede o formato intacto); a conclusão
  pela data de conclusão está no painel. A linha do T016 que dizia o contrário foi lida assim.
- `slaGlobalMediaHoras` do histórico passou a média das médias por ponto ponderada pelas
  concluídas, truncada em horas (antes: soma de segundos sobre contagem); a diferença é menor que
  1 segundo por ponto antes do truncamento.
- `dias` do histórico igual a 0 ou negativo, que antes virava 30, agora responde 400 (RF-09).
  O frontend v1.5.0 só envia 7, 30 e 90.
- Janela do histórico agora é de dias locais inteiros (do início do dia de `hoje - dias` ao fim de
  hoje); antes eram baldes UTC a partir de `now - dias`.
- A série do painel foi reescrita em passe único depois da medição; a equivalência com a versão
  anterior foi provada uma vez, por teste temporário removido (a prova permanente são os testes
  Postgres do painel).
- Risco: o teste de desempenho roda na esteira com p95 ≤ 500 ms; medidas de 66 a 100 ms dão
  margem de cerca de 5 vezes, mas uma máquina muito lenta pode oscilar.
- Excesso de escopo: nenhum.
- Desvios de processo: usei `cat >>` (heredoc) para acrescentar linhas a `casos-de-uso.md` e
  `openspec/README.md`.

Veredito: **convergido**; falta o `/bu:review`.
Tarefas acrescentadas: nenhuma.

### Rodada 2 — 2026-10-10

Depois da primeira passagem do `/bu:review` (REPROVADO por padrão de teste e higiene; gates e
comportamento sãos), corrigido e verificado:

- Comentários e `System.out.printf` fora de testes (a explicação virou nome de constante e de
  método); blocos `// Arrange` onde faltavam; `verifyNoMoreInteractions` nos testes novos.
- Testes divididos em um comportamento cada; verificações com `any()` trocadas pelos parâmetros
  reais (janelas, modelo, fuso, agora); `MetricasContratoV15Test` agora passa pelo controller real
  e prova os **tipos** dos campos antigos.
- Espelho `CriterioOrdenacaoSolicitacaoTest` criado; `RotuloDeSerie` extraída (DRY) com teste.
- Ramo `dias <= 0 → 30` removido do caso de uso (inalcançável; o controller responde 400).
- `findAll` ganhou desempate por `id`.

Medição: `make validate` EXIT=0, **1014 testes**, 0 falhas, "All coverage checks have been met";
`make test TESTE='*PostgresTest,FlywayMigrationTest'`: **76 testes**, 0 falhas (inclui o
desempenho do painel).

**Dívida aceita (a confirmar):** o SQL de `painelSerie` (cerca de 70 linhas em uma anotação) fica
como está: legível por CTEs, provado por 5 mutações e medido; reescrevê-lo agora arriscaria a
equivalência provada. Testes legados do `SolicitacaoRepositoryAdapterTest` com nome em
`snake_case` e várias asserções não foram tocados (Princípio 11: só teste novo ou alterado).
Desvios de processo: um `sed` de 3 nomes de método em arquivo recém-criado pelo agente.

Veredito: **convergido**; segunda passagem do `/bu:review` pendente.
Tarefas acrescentadas: nenhuma.

### Rodada 3 — 2026-10-10

Segunda passagem do `/bu:review`: **sem achado impeditivo** ("Não há achado que impeça a
entrega"); seis desvios menores. Tratados:

- Testes do painel com stub e sem `verify`: `verify` acrescentado nos três (semana, abertas
  agora, atrasadas agora).
- Teto de `dias` duplicado e assimétrico: o caso de uso agora valida a faixa inteira (1 a 365),
  com teste para 0 e para 366 que compara a mensagem inteira (antes, `assertTrue(a && b)`).

Mantidos como dívida, aceitos (a confirmar pelo usuário):
- `SolicitacaoRepositoryAdapter.agregarPainel` com cerca de 24 linhas e as conversões
  `Object[]` repetidas (extrair `paraTotais` e helpers `longDe`/`doubleOuNulo`).
- Testes de série em Postgres com 5 a 6 asserções (invariantes únicas, peso baixo).
- `shouldMandarPaginaSemSortAoJpa...` usa `any()` nos filtros; o repasse de critério e sentido
  está provado no teste anterior do mesmo arquivo.
- Teste de desempenho com relógio de parede (limite 500 ms, medido 66 a 100 ms).
- SQL de `painelSerie` longo (já registrado na Rodada 2).

Medição final: `make validate` EXIT=0, **1015 testes**, 0 falhas, "All coverage checks have been
met"; `make test TESTE='*PostgresTest,FlywayMigrationTest'` 76 de 76 (rodada 2); sonda em
servidor real 29 de 29.

Veredito: **convergido; `/bu:review` sem achado impeditivo**.
Tarefas acrescentadas: nenhuma.
