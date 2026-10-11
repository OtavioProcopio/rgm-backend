# Tarefas — Segurança e robustez da API: permissões, exposição e limites

> Ordem de dependência. `[P]` marca tarefa paralelizável (não toca arquivo de outra `[P]`
> da mesma fase). Teste vem antes da implementação que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`,
`RES` = `app/src/main/resources`.

Checklists: revisados pelo usuário em 2026-10-10 (43 itens em `requisitos`, `seguranca` e
`operacao`); a revisão e as três correções que ela gerou estão no Esclarecimento 7 da spec.

Desvio herdado (feature 001): o projeto não tem `app/tests/bdd` nem alvo `make bdd`. Os
critérios de aceite da spec são provados pelos testes de controller, de configuração de
segurança e de verificador listados abaixo, não por cenários executáveis em Gherkin. A tabela
de rastreabilidade mostra qual teste cobre qual cenário.

Todo teste novo segue o Princípio 3: `// Arrange`, `// Act`, `// Assert`, um comportamento por
teste, nome `shouldXWhenY`, `verifyNoMoreInteractions` onde houver colaborador mockado.

## Fase 1 — Borda web: verificador de parâmetros

- [x] T001 Criar `TEST/adapter/in/web/ParametrosDeConsultaTest.java`: `pagina` aceita `page` 0 e `size` 1 e 100; recusa `page` -1, `size` 0, -5, 101 e 5000 com `ValidationException` cuja mensagem cita o parâmetro e o intervalo; `data` devolve o `Instant` para ISO-8601 e, para `ontem`, `32/13/2026` e `xx`, lança `ValidationException` com o nome do campo e o formato; `data` devolve `null` para valor nulo; `enumeracao` devolve a constante para valor válido, `null` para nulo, e para valor inválido lança `ValidationException` com o nome do campo e a lista dos valores aceitos, sem o nome da classe
- [x] T002 Criar `MAIN/adapter/in/web/ParametrosDeConsulta.java`: classe utilitária final com `pagina(int page, int size)`, `data(String campo, String valor)` e `enumeracao(String campo, String valor, Class<E>)`; constante `TAMANHO_MAXIMO_DA_PAGINA = 100`; mensagens em português (T001 verde)

## Fase 2 — Tratamento de erro

- [x] T003 Alterar `TEST/adapter/config/GlobalExceptionHandlerTest.java`: `HttpMessageNotReadableException` responde 400 com mensagem em português e sem o texto interno do Jackson; `IllegalArgumentException` com mensagem "No enum constant com.rgm...X" responde 400 sem "No enum constant" nem "com.rgm" na mensagem; `RuntimeException` continua 500
- [x] T004 Alterar `MAIN/adapter/config/GlobalExceptionHandler.java`: tratador de `HttpMessageNotReadableException` (400, "Corpo da requisicao invalido ou malformado") e saneamento da mensagem de enumeração em `handleIllegalArgument` (T003 verde)

## Fase 3 — Segurança

- [x] T005 [P] Criar `TEST/adapter/config/JsonAuthenticationEntryPointTest.java`: `commence` grava status 401, `Content-Type: application/json` e corpo com `status`, `error`, `message` em português e `timestamp`; não chama `sendError`
- [x] T006 [P] Criar `TEST/adapter/config/JsonAccessDeniedHandlerTest.java`: `handle` grava status 403, `Content-Type: application/json` e o mesmo formato de corpo; não chama `sendError`
- [x] T007 [P] Criar `TEST/adapter/config/ActuatorSecurityConfigTest.java` (`@WebMvcTest` com a configuração real e controllers de teste mapeados em `/actuator/health`, `/actuator/prometheus` e `/actuator`): `health` 200 sem credencial; `prometheus` 401 sem credencial; 200 com a credencial de coleta; 401 com outra credencial; 401 ou 403 para um JWT de administrador sem a credencial; com senha de coleta vazia, 401 mesmo com usuário preenchido
- [x] T008 Alterar `RES/application.properties`: `monitoramento.coleta.usuario=${MONITORAMENTO_COLETA_USUARIO:}` e `monitoramento.coleta.senha=${MONITORAMENTO_COLETA_SENHA:}` (vazias por padrão); sem teste próprio, coberto por T007
- [x] T009 [P] Criar `MAIN/adapter/config/JsonAuthenticationEntryPoint.java` (T005 verde)
- [x] T010 [P] Criar `MAIN/adapter/config/JsonAccessDeniedHandler.java` (T006 verde)
- [x] T011 [P] Criar `MAIN/adapter/config/ActuatorSecurityConfig.java`: `SecurityFilterChain` com `@Order(1)`, `securityMatcher("/actuator/**")`, `health` liberado, resto exige a conta de coleta por HTTP Basic, sem sessão e sem CSRF, `UserDetailsService` em memória que só cria a conta com senha não vazia, senha guardada com `BCryptPasswordEncoder` (T007 e T008 verdes)
- [x] T012 [P] Criar `TEST/adapter/config/SecurityConfigTest.java` (`@WebMvcTest` com `SecurityConfig` real e controller de teste): sem credencial em rota protegida, 401 com corpo `ErrorResponse`; operador em rota restrita, 403 com corpo `ErrorResponse`; rotas `/api/auth/**` seguem livres; `/actuator/**` não está mais liberado na cadeia principal
- [x] T013 [P] Criar `TEST/adapter/in/web/solicitacao/SolicitacaoControllerSegurancaTest.java` (`@WebMvcTest` com `SecurityConfig` real e controller real): operador recebe 403 em `/api/solicitacoes/metricas`, `/metricas/historico`, `/metricas/por-modelo` e `/metricas/por-modelo/pdf` e o caso de uso não é chamado; gestor e administrador recebem 200 nas quatro; sem credencial, 401; `GET /api/solicitacoes` segue 200 para operador
- [x] T014 [P] Criar `TEST/adapter/in/web/admin/AdminControllerSegurancaTest.java` (idem): operador em `GET /api/admin/usuarios` recebe 403 (e não 401) com corpo; gestor recebe 200; sem credencial, 401
- [x] T015 Alterar `MAIN/adapter/config/SecurityConfig.java`: regra `GET /api/solicitacoes/metricas` e `/api/solicitacoes/metricas/**` para `ADMINISTRADOR` e `GESTOR`, antes de `anyRequest`; remover `/actuator/**` de `permitAll`; usar `JsonAuthenticationEntryPoint` e `JsonAccessDeniedHandler` (T009, T010, T011, T012, T013 e T014 verdes)

> T005, T006 e T007 são `[P]` entre si, e T009, T010 e T011 também (arquivos distintos).
> T012, T013 e T014 são `[P]` entre si; T015 espera os três e T009 a T011.

## Fase 4 — Controllers

- [x] T016 [P] Alterar `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java`: `listar` responde 400 para `page=-1`, `size=0`, `size=-5`, `size=101` e `size=5000` sem chamar o caso de uso; aceita `size=100`; sem `size`, passa 20; 400 para `criadaEmInicio`, `criadaEmFim`, `dataInicio` e `dataFim` inválidos, com o nome do campo na mensagem; 400 para `status`, `tipo`, `prioridade` e `tipoData` inválidos, com os valores aceitos e sem o nome da classe; `obterMetricasPorModelo` responde 400 para `size=101`
- [x] T017 [P] Alterar `TEST/adapter/in/web/modelo/ModeloControllerTest.java`: `listar` responde 400 para `size=0` e `size=101`, aceita `size=100`, e passa 20 sem `size`
- [x] T018 [P] Alterar `TEST/adapter/in/web/admin/AdminControllerTest.java`: `listarUsuarios` responde 400 para `page=-1`, `size=-1` e `size=101`, aceita `size=100`; 400 para `perfil` inválido com os valores aceitos e sem o nome da classe
- [x] T019 [P] Alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java`: `listar`, `obterMetricasPorModelo` e `buildInput` passam a usar `ParametrosDeConsulta` (T002 e T016 verdes)
- [x] T020 [P] Alterar `MAIN/adapter/in/web/modelo/ModeloController.java`: `listar` usa `ParametrosDeConsulta.pagina` (T002 e T017 verdes)
- [x] T021 [P] Alterar `MAIN/adapter/in/web/admin/AdminController.java`: `listarUsuarios` usa `ParametrosDeConsulta.pagina` e `enumeracao("perfil", ...)` (T002 e T018 verdes)

> T016 a T018 são `[P]` entre si; T019 a T021 também (cada par toca um controller e o teste dele).

## Fase 5 — Contrato e configuração

- [x] T022 [P] Criar `TEST/adapter/in/web/RotasExistentesTest.java`: lê por reflexão os `@RequestMapping` dos controllers e afirma que cada par (método, caminho) do `develop` de partida continua mapeado e que o scanner enxerga ao menos a lista (RF-09, RNF-05); a lista de partida é a do `develop` em `39ce64c`. **Não afirma códigos de status**: os de sucesso são protegidos pelos testes de cada controller, e nenhum `@ResponseStatus` ou `ResponseEntity` foi alterado nesta spec (o diff contra o `develop` não toca resposta de sucesso)
- [x] T023 [P] Criar `TEST/adapter/config/ConfiguracaoDeProducaoTest.java` (teste de caracterização: passou de primeira porque a configuração já estava correta; não houve vermelho): lê `application-prod.properties` e afirma `springdoc.swagger-ui.enabled=false` e `springdoc.api-docs.enabled=false` (RF-10)

## Fase 6 — Documentação, dependência e fechamento

- [x] T024 [P] Alterar `docs/casos-de-uso.md`: UC-16 com atores restritos e 403; nota sobre o teto de 100 por página e sobre os erros 400 de entrada; nota da conta de coleta do actuator
- [x] T025 [P] Alterar `openspec/README.md` (as métricas entraram na capacidade `metricas-dashboard`, que não tinha linha; `autenticacao` e `solicitacoes-kanban` também): linha da `specs/010-seguranca-e-robustez-da-api-permissoes` em `autenticacao` e em `solicitacoes-kanban`, conforme o Princípio 12; sem tocar em `openspec/specs/` nem em `openspec/changes/`
- [x] T026 [P] Alterar `.env.example`: `MONITORAMENTO_COLETA_USUARIO` e `MONITORAMENTO_COLETA_SENHA` vazias, com o comentário de quando usá-las
- [x] T027 (issue `rgm-frontend#153`, criada em 2026-10-10) Registrar a dependência do RF-06 no `rgm-frontend`: issue para a `PerfilPage` não pedir os indicadores agregados ao operador e esconder a seção "Visão Geral do Sistema" para ele; a publicação do backend espera essa entrega
- [x] T028 (2026-10-10: 36 de 36 verificações OK, saída em `/root/rgm/evidencias/integracao-2026-10-10/sonda-010.txt`) Reiniciar o backend de desenvolvimento com este código e repetir as sondas da seção "Fatos medidos" da spec: operador recebe 403 nos quatro endpoints de indicadores e 403 (não 401) em `/api/admin/usuarios`; `/actuator/prometheus` anônimo responde 401 e `health` 200; as entradas inválidas respondem 400; colar a saída no PR
- [x] T030 Alterar `Makefile`: o alvo `test` aceita `TESTE=<Classe>` (padrão inalterado). Emenda do plano em 2026-10-10 (Princípio 1: sem o seletor não há como rodar um teste só por `make`)
- [x] T031 Alterar `TEST/adapter/in/web/modelo/ModeloControllerTest.java`: cobrir as linhas anteriores à 010 que deixam `ModeloController` com 92,6% de linha (113 de 122) e pedem 95% por arquivo tocado (Princípio 3 e 10): relatório da ficha do modelo (`GET /api/modelos/{id}/relatorio`: modelo inexistente responde 404; modelo com solicitações monta o PDF), `parseTipo` de tipo de modelo inválido (400) e o nome do usuário autenticado ausente ("Sistema"). Meta: 116 ou mais de 122 linhas. Testes de caracterização do comportamento atual, sem mudar código de produção
- [x] T029 `make validate` verde (2026-10-10: 901 testes, 0 falhas, `All coverage checks have been met`; ParametrosDeConsulta, JsonAuthenticationEntryPoint, JsonAccessDeniedHandler, ActuatorSecurityConfig, SecurityConfig, GlobalExceptionHandler, AdminController e ModeloController com 100% de linha; SolicitacaoController com 98,1%) (formatação, lint, testes, cobertura ≥ 95% por arquivo tocado)

## Rastreabilidade

| Requisito | Tarefas | Teste que prova o cenário da spec |
|---|---|---|
| RF-01 | T013, T015 | "Operador não recebe indicadores agregados", "Operador não recebe o histórico nem o ranking por modelo", "Gestor e administrador continuam recebendo os indicadores" (`SolicitacaoControllerSegurancaTest`) |
| RF-02 | T005, T006, T009, T010, T012, T014, T015 | "Falta de permissão responde 403 com corpo padrão", "Sem credencial continua 401" (`SecurityConfigTest`, `AdminControllerSegurancaTest`) |
| RF-03 | T007, T008, T011, T015 | "A saúde continua anônima", "As métricas internas exigem credencial", "A credencial de coleta abre as métricas", "Credencial errada não abre as métricas", "Um usuário humano, mesmo administrador, não abre as métricas pela sessão", "Sem credencial configurada ninguém coleta" (`ActuatorSecurityConfigTest`) |
| RF-04 | — (retirado, Esclarecimento 6) | — |
| RF-05 | T001, T002, T003, T004, T016, T018, T019, T021 | "Corpo JSON malformado responde 400", "Data inválida no filtro responde 400", "Paginação fora do intervalo responde 400" (`GlobalExceptionHandlerTest`, `ParametrosDeConsultaTest`, `SolicitacaoControllerTest`, `AdminControllerTest`) |
| RF-06 | T027 | "O frontend não pede indicadores agregados ao operador" (verificado no `rgm-frontend`) |
| RF-07 | T001, T002, T016, T017, T018, T019, T020, T021 | "Paginação fora do intervalo responde 400", "Página no limite é aceita", "Sem tamanho informado vale o padrão" (`SolicitacaoControllerTest`, `ModeloControllerTest`, `AdminControllerTest`) |
| RF-08 | T001, T002, T003, T004, T016, T018, T019, T021 | "Valor de enumeração inválido não expõe a classe interna" (`ParametrosDeConsultaTest`, `GlobalExceptionHandlerTest`) |
| RF-09 | T022 | "As rotas existentes seguem iguais" (`RotasExistentesTest`) |
| RF-10 | T023 | "O Swagger UI não é servido em produção", "O Swagger UI segue disponível em desenvolvimento" (`ConfiguracaoDeProducaoTest`; o segundo é a configuração padrão do perfil `dev`, já presente) |
| RNF-01 | T016, T017, T018, T028 | as 12 entradas da seção "Fatos medidos" |
| RNF-02 | T007, T011, T015, T028 | `ActuatorSecurityConfigTest` |
| RNF-03 | T007, T028 | `ActuatorSecurityConfigTest` (`health` 200 sem credencial) |
| RNF-04 | T029 | `make validate` |
| RNF-05 | T022 | `RotasExistentesTest` |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-10-10

Prefixo `MAIN` = `app/src/main/java/com/rgm/api`. Estado da árvore: sem commit (a entrega é no
working tree da branch `fix/seguranca-e-robustez-da-api`).

| Requisito | Estado | Evidência |
|---|---|---|
| RF-01 indicadores só gestor e administrador | realizado | `MAIN/adapter/config/SecurityConfig.java:46-50` (`hasAnyRole("ADMINISTRADOR","GESTOR")` em `/api/solicitacoes/metricas` e `/**`); `SolicitacaoControllerSegurancaTest` (12 testes); sonda real: operador 403 nas 4 rotas, gestor e administrador 200, anônimo 401 |
| RF-02 recusa de permissão responde 403 com corpo | realizado | `MAIN/adapter/config/JsonAccessDeniedHandler.java`, `JsonAuthenticationEntryPoint.java`, ligados em `SecurityConfig.java:62-63`; `SecurityConfigTest`, `AdminControllerSegurancaTest`; sonda real: operador em `/api/admin/usuarios` responde 403 com `{"status":403,"error":"Forbidden",...}` (antes: 401 sem corpo) |
| RF-03 só `health` anônimo, resto com credencial de coleta | realizado | `MAIN/adapter/config/ActuatorSecurityConfig.java:30-45` (`@Order(1)`, `securityMatcher("/actuator/**")`, `health` liberado, `hasRole("COLETOR")`) e `:53-54` (sem senha, ninguém coleta); `application.properties` (`monitoramento.coleta.*`); `ActuatorSecurityConfigTest` (7) e `...SemCredencialTest` (2); sonda real: anônimo 401, credencial 200, senha errada 401, JWT de administrador 401 |
| RF-04 | retirado | Esclarecimento 6 da spec (o `rgm-infra` não é usado) |
| RF-05 entrada inválida nunca vira 5xx | realizado | `MAIN/adapter/in/web/ParametrosDeConsulta.java:20-60`; `GlobalExceptionHandler.java:124` (`HttpMessageNotReadableException`); usado em `SolicitacaoController.java:161, 266, 333-347`; sonda real: corpo malformado, 4 datas, `page=-1`, `size` 0, -5, 101, 5000, `modelos?size=0`, `usuarios?size=-1` respondem 400 |
| RF-06 frontend antes do backend | **parcial** | O lado do backend está pronto e documentado (spec, plano, `docs/casos-de-uso.md`). A mudança do frontend (`PerfilPage` não pede os indicadores ao operador) é a issue `rgm-frontend#153`, **aberta e ainda não entregue**; o backend só deve ser publicado depois dela |
| RF-07 `size` de 1 a 100 nas 4 listagens | realizado | `ParametrosDeConsulta.java:15, 24`; `SolicitacaoController.java:161, 266`, `ModeloController.java:99`, `AdminController.java:72`; sonda real: `size=100` 200 com 100 itens, sem `size` 20 itens, 101 e 5000 respondem 400 |
| RF-08 enumeração sem classe interna, com campo e valores aceitos | realizado | `ParametrosDeConsulta.java:46-60`; `GlobalExceptionHandler.java:27-28, 117-121` (rede de proteção); sonda real: `status`, `tipo`, `prioridade`, `tipoData` e `perfil` inválidos respondem 400 com "Valores aceitos: ..." e sem `com.rgm` |
| RF-09 nada removido ou renomeado | realizado | `RotasExistentesTest` (54 rotas); `git diff develop` sem nenhuma linha de mapeamento alterada |
| RF-10 Swagger não servido em produção | realizado | `application-prod.properties:17-18` (já estava correto); `ConfiguracaoDeProducaoTest` (3); em desenvolvimento a sonda real responde 200 em `/swagger-ui/index.html` |
| RNF-01 0 respostas 5xx nos casos de entrada inválida | realizado | sonda real: 36 de 36 verificações OK (`/root/rgm/evidencias/integracao-2026-10-10/sonda-010.txt`) |
| RNF-02 só `health` responde 200 sem token | realizado | sonda real: `/actuator` e `/actuator/prometheus` anônimos respondem 401 |
| RNF-03 `health` em até 1 s sem token | realizado | medido: 200 em 6 a 8 ms (5 execuções) |
| RNF-04 cobertura por arquivo tocado ≥ 95% | realizado | JaCoCo da última execução: ParametrosDeConsulta, JsonAuthenticationEntryPoint, JsonAccessDeniedHandler, ActuatorSecurityConfig, SecurityConfig, GlobalExceptionHandler, AdminController e ModeloController com 100% de linha; SolicitacaoController 98,1%; limite do `pom.xml` intacto |
| RNF-05 nenhuma rota, campo ou evento removido | realizado | `RotasExistentesTest`; nenhum DTO de resposta alterado |

Cenários da spec: os 21 cenários restantes (o do coletor saiu com o RF-04) têm teste na tabela
de rastreabilidade e passam; o cenário "O frontend não pede indicadores agregados ao
operador" é verificado no `rgm-frontend` (issue #153).

`make validate` (saída real, 2026-10-10, sem alteração de `app/src` depois dela): `lint`
(Spotless) sem violação; `Tests run: 901, Failures: 0, Errors: 0, Skipped: 0`;
`All coverage checks have been met`; `BUILD SUCCESS`; `EXIT=0`.

Excesso (registrado, nenhum viola a seção Fora de escopo):
- `Makefile`: o alvo `test` ganhou a variável opcional `TESTE=` (T030). Não está em nenhum
  requisito; foi necessário para rodar um teste por `make`, como o Princípio 1 manda, e foi
  declarado na emenda do plano.
- `ModeloControllerTest`: 11 testes de caracterização do comportamento anterior (T031), só para
  levar o arquivo tocado de 92,6% a 100% de linha, como o Princípio 3 e o 10 pedem.
- `JsonAuthenticationEntryPoint`: o 401 sem credencial também passa a trazer corpo JSON. A spec
  só exige o corpo no 403, mas as duas respostas vinham do mesmo `sendError`, e deixar o 401
  vazio reproduziria o defeito do #113 pela metade.

Observação fora de escopo (já registrada no plano): rota inexistente responde 401 a quem não
tem credencial, e para o autenticado pelo mesmo motivo do despacho de erro. Não foi alterado.

Veredito: **convergido no backend, com uma pendência externa** (RF-06: a issue
`rgm-frontend#153` precisa ser entregue e publicada antes do backend).
Tarefas acrescentadas: nenhuma.

## Tarefas da convergência

### Achados do `/bu:review` de 2026-10-10 (parecer REPROVADO)

Reproduzido pelo revisor com teste descartável: `HEAD /api/solicitacoes/metricas` por um
OPERADOR devolvia 200 e executava o caso de uso. A regra do RF-01 estava casada só com `GET`.

- [x] T032 [P] Achados 1 e 2 em `TEST/adapter/in/web/solicitacao/SolicitacaoControllerSegurancaTest.java`: teste primeiro, `HEAD` de operador nas quatro rotas de indicadores deve responder 403 e o caso de uso não é chamado (vermelho hoje); nos testes de 200 de gestor e administrador, verificar a chamada do caso de uso (ocorrência, parâmetros e retorno) e `verifyNoMoreInteractions` nos colaboradores, e `autenticarAcessoUseCase` verificado
- [x] T033 [P] Achados 1 e 2 em `TEST/adapter/in/web/admin/AdminControllerSegurancaTest.java`: `HEAD /api/admin/usuarios` por operador deve responder 403 e o caso de uso não é chamado; verificar as chamadas dos testes de 200 e `autenticarAcessoUseCase`
- [x] T034 [P] Achados 3 e 4: `TEST/adapter/in/web/RotasExistentesTest.java` perde o teste do tamanho da própria constante (tautologia) e ganha um teste que prova que o scanner enxerga pelo menos as rotas da lista (guarda contra scanner vazio); `TEST/adapter/config/ActuatorSecurityConfigTest.java`: o teste do JWT de administrador fixa o status exato medido (401) e verifica o stub (`verifyNoInteractions` ou a chamada, o que ocorrer de fato)
- [x] T035 Achado 1: `MAIN/adapter/config/SecurityConfig.java`: a regra dos indicadores vale para todos os métodos HTTP (remover `HttpMethod.GET` do matcher), de modo que `HEAD` também exige gestor ou administrador (T032 e T033 verdes)
- [x] T036 Achado 6: `MAIN/adapter/in/web/ParametrosDeConsulta.java` e `TEST/adapter/in/web/ParametrosDeConsultaTest.java`: as mensagens passam a trazer o valor recebido (truncado em 50 caracteres)
- [x] T037 Achados 5, 7 e 8: `plan.md` corrigido (a senha de coleta usa o codificador delegante do Spring, bcrypt por padrão); `final` nos parâmetros dos testes novos que não o têm; registrar que o `ObjectMapper` real nos testes dos handlers é decisão consciente (serializador, não colaborador)
- [x] T038 Reexecutar `make validate`, a sonda real (incluindo `HEAD`) e `/bu:review` (validate: 912 testes, 0 falhas, `All coverage checks have been met`; sonda real: 41 de 41, saída em `/root/rgm/evidencias/integracao-2026-10-10/sonda-010.txt`)

### Rodada 2 — 2026-10-10

Depois do parecer REPROVADO do `/bu:review` (achado 1: `HEAD` contornava o RF-01).

| Requisito | Estado | Evidência |
|---|---|---|
| RF-01 indicadores só gestor e administrador | realizado | `MAIN/adapter/config/SecurityConfig.java:46-47` (matcher sem método: vale para `GET`, `HEAD` e qualquer outro); `SolicitacaoControllerSegurancaTest` (16: 4 de `HEAD`, antes vermelhos, agora verdes); sonda real: `HEAD` de operador responde 403 nas 4 rotas |
| RF-05 e RF-08 mensagens | realizado | `ParametrosDeConsulta.java` agora diz também o valor recebido (truncado em 50 caracteres); `ParametrosDeConsultaTest` (23) |
| RNF-04 cobertura por arquivo | realizado | JaCoCo da execução desta rodada: 100% de linha em 8 das 9 classes tocadas; `SolicitacaoController` 98,1% |
| Demais requisitos | sem mudança desde a rodada 1 | idem |

`make validate` (saída real, 2026-10-10): `Tests run: 912, Failures: 0, Errors: 0, Skipped: 0`;
`All coverage checks have been met`; `BUILD SUCCESS`; `EXIT=0`. Sonda real: 41 de 41.

Fato apurado: o revisor também apontou o mesmo furo em `/api/admin/usuarios`; o teste novo
mostra que `HEAD` nele já respondia 403 (a regra genérica de `/api/admin/**` cobre), então a
falha era só dos indicadores.

Veredito: **convergido no backend, com a mesma pendência externa** (RF-06: a issue
`rgm-frontend#153`).
Tarefas acrescentadas: nenhuma.

### Achados do `/bu:review` de 2026-10-10, segunda passagem (parecer REPROVADO, comportamento correto)

O revisor confirmou que o comportamento entregue está correto e que os gates passam; o que resta
é padrão de teste, rastro de erro e documentação.

- [x] T039 [P] Achado 1 e 4 em testes sem produção: remover o Javadoc de classe (comentário fora de Arrange, Act e Assert) de `ActuatorSecurityConfigSemCredencialTest`, `RotasExistentesTest`, `AdminControllerSegurancaTest` e `SolicitacaoControllerSegurancaTest`; `ParametrosDeConsultaTest`: o teste com `assertDoesNotThrow` passa a ter Act e Assert separados
- [x] T040 [P] Achado 2 em `SolicitacaoControllerTest` e `SolicitacaoControllerSegurancaTest`: os testes de repasse e o 200 do operador em `GET /api/solicitacoes` fecham os colaboradores com `verify` e `verifyNoMoreInteractions`; sem variáveis de Arrange sem função
- [x] T041 [P] Achado 2 em `AdminControllerTest` e `ModeloControllerTest`: o mesmo
- [x] T042 Achado 5: `GlobalExceptionHandler.handleMessageNotReadable` registra a causa em `debug`, para o servidor ter o motivo do 400
- [x] T043 Achado 7: `SolicitacaoController.buildInput` perde a repetição de leitura de data (helper privado), sem mudar comportamento (testes existentes verdes)
- [x] T044 Achados 3, 6 e 8: corrigir os artefatos (T022 e RNF-05 não afirmam código de status; `plan.md` e o comentário do `validate` no `Makefile`; risco da conta de coleta sem limite de tentativas registrado no plano)
- [x] T045 `make validate`, sonda real e `/bu:review` (ver Rodada 3)

### Rodada 3 — 2026-10-10

Depois da segunda passagem do `/bu:review` (T039 a T044) e de uma terceira, em que o revisor
confirmou que **a implementação está sã e os gates passam** e reprovou por padrão de teste.

Corrigido na rodada (verificado):
- Testes dos handlers JSON: o antigo "não chama `sendError`" afirmava `getErrorMessage() == null`,
  que o `MockHttpServletResponse` não preenche com `sendError(int)`; reescritos com
  `HttpServletResponse` mockado e `verify(response, never()).sendError(...)`. Prova por mutação:
  injetar `response.sendError(403)` no handler faz só o teste novo falhar; restaurado.
- `verifyNoMoreInteractions` de mock nunca estubado removido; uma verificação por teste nos dois
  arquivos (7 testes cada).
- `HEAD` (RF-01), log do motivo do 400 de corpo malformado, helper de data em `buildInput`,
  Javadocs de classe removidos dos testes, caracterização do `ModeloController`.

`make validate` (saída real, 2026-10-10, antes da última troca dos testes dos handlers, que só
tocou arquivos de teste): `Tests run: 912, Failures: 0`, `All coverage checks have been met`,
`EXIT=0`; depois dela, `make test`: `Tests run: 918, Failures: 0`, `BUILD SUCCESS`, e `lint`
limpo. Sonda real com o código final de produção: 41 de 41.

**Itens do parecer que ficam como dívida, não corrigidos nesta spec** (nenhum afeta
comportamento, segurança ou cobertura):
1. `ObjectMapper` real nos testes dos handlers (Princípio 3 pede mock de dependência injetada;
   é serializador e o formato do JSON já é provado em `SecurityConfigTest`).
2. Alguns testes novos com 2 a 4 asserções (`ParametrosDeConsultaTest`,
   `SolicitacaoControllerSegurancaTest`, um de `perfil` em `AdminControllerTest`).
3. `SecurityConfigTest`: dois pares de testes de 401 e 403 que se sobrepõem, e uma rota de
   teste (`/api/solicitacoes/metricas`) sem uso; stubs de `autenticarComo` sem `verify`.
4. `RotasExistentesTest`: o teste "scanner enxerga ao menos a lista" é redundante com o primeiro.
5. Nome `ParametrosDeConsulta.pagina` poderia ser `validarPagina`.
6. `GlobalExceptionHandler` detecta enumeração por `startsWith("No enum constant")`, acoplado ao
   texto do JDK (a rede de proteção; o caminho principal usa `ParametrosDeConsulta`).
7. Comportamento herdado, agora fixado por teste: gestor recebe 403 em `HEAD /api/admin/usuarios`
   (200 no `GET`); decidir em outra issue se `HEAD` deve seguir o `GET`.
8. Conta de coleta do actuator sem limite de tentativas (registrada nos riscos do plano).

Os 8 itens estão registrados em `rgm-backend#124`.

Veredito: **convergido no backend; `/bu:review` ainda REPROVADO** apenas pelos itens de padrão de
teste acima, aceitos como dívida pelo agente e **a confirmar pelo usuário**; pendência externa:
RF-06 (`rgm-frontend#153`).
Tarefas acrescentadas: nenhuma.
