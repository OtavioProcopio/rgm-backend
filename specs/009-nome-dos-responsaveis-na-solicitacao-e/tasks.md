# Tarefas — Nome dos responsáveis na solicitação e capa da galeria sem erro

> Ordem de dependência. `[P]` marca tarefa paralelizável (não toca arquivo de outra `[P]`
> da mesma fase). Teste vem antes da implementação que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

Checklists: revisados e marcados pelo usuário em 2026-10-09 (26 de 26 itens).

Desvio herdado (feature 001): o projeto não tem `app/tests/bdd` nem alvo `make bdd`. Os
critérios de aceite da spec são provados pelos testes de caso de uso, de controller, de DTO e
de repositório listados abaixo, não por cenários executáveis em Gherkin. A tabela de
rastreabilidade mostra qual teste cobre qual cenário.

Estado da execução (2026-10-09): T001 a T014 escritas na sessão, sem Java nem acesso ao Maven
(o firewall da rede bloqueia os JARs); `make validate` (T015) rodado pelo usuário na VPS e verde.

## Fase 1 — Domínio (porta)

- [x] T001 Alterar `MAIN/core/domain/ports/repositories/FotoGaleriaModeloRepository.java`: remover `limparPrincipal(UUID)` e acrescentar `FotoGaleriaModelo salvarComoPrincipal(FotoGaleriaModelo)`. Interface sem teste próprio; coberta por T002 a T008. O código deixa de compilar até T004 e T007, o que é esperado.

## Fase 2 — Aplicação

- [x] T002 [P] Testes em `TEST/core/application/usecases/modelo/EditarFotoGaleriaUseCaseTest.java`: `principal` verdadeiro chama `salvarComoPrincipal` com a foto já com a legenda nova e não chama `save`; `principal` falso e só legenda chamam `save`; foto de outro modelo, foto inexistente, perfil sem permissão e legenda em branco seguem recusados; `verifyNoMoreInteractions`
- [x] T003 [P] Testes em `TEST/core/application/usecases/solicitacao/ObterSolicitacaoUseCaseTest.java`: `execute` devolve `responsaveis` na ordem de `responsavelIds` e `abertaPorNome` com **uma** chamada a `findAllByIdIn`; responsável inativo mantém o nome; responsável sem usuário vira `{id, nome nulo}` preservando ordem e tamanho; quem abriu não encontrado dá `abertaPorNome` nulo; sem responsável devolve lista vazia; `resolverNomes` devolve mapa id→nome, lista vazia não consulta, id desconhecido fica fora do mapa; `responsavelIds` segue igual
- [x] T004 Alterar `MAIN/core/application/usecases/modelo/EditarFotoGaleriaUseCase.java` para usar `salvarComoPrincipal` (T002 verde)
- [x] T005 Alterar `MAIN/core/application/usecases/solicitacao/ObterSolicitacaoUseCase.java`: registro `ResponsavelNome(id, nome)`, `Output` com `responsaveis` e `abertaPorNome`, método `resolverNomes(Collection<UUID>)`; `listarResponsaveisBatch` intacto (T003 verde)

> T002 e T003 são `[P]` entre si; T004 e T005 também. Cada par toca arquivos distintos.

## Fase 3 — Adapters e infra

Persistência da galeria:

- [x] T006 [P] Testes em `TEST/adapter/out/persistence/FotoGaleriaModeloRepositoryAdapterTest.java`: `salvarComoPrincipal` limpa a capa do modelo e grava a foto, nessa ordem, e devolve o domínio mapeado; o método carrega `@Transactional` (verificado por reflexão); os testes de `limparPrincipal` saem
- [x] T007 [P] Criar `TEST/adapter/out/persistence/FotoGaleriaModeloRepositoryAdapterPostgresTest.java` (Testcontainers + `@DataJpaTest` com `Propagation.NOT_SUPPORTED`, adaptador importado): duas fotos, definir a segunda como capa → a primeira deixa de ser capa e a segunda é; foto de outro modelo não perde a capa; falha provocada ao gravar (valor que viola restrição) → a capa antiga continua e há exatamente 1 capa; **o teste falha se `@Transactional` for removido** (`TransactionRequiredException`)
- [x] T008 Alterar `MAIN/adapter/out/persistence/FotoGaleriaModeloRepositoryAdapter.java`: `@Transactional salvarComoPrincipal` (limpa via JPA e grava); remover `limparPrincipal` (T006 e T007 verdes)

Resposta HTTP:

- [x] T009 [P] Criar `TEST/adapter/in/web/dto/response/SolicitacaoResponseTest.java`: fábrica com nomes preenche `responsaveis` e `abertaPorNome`; `from(s)` e `from(s, ids)` deixam os dois campos nulos; `responsavelIds` e `abertaPorUsuarioId` seguem com o valor de hoje; `ResponsavelResponse` leva id e nome (nulo aceito)
- [x] T010 [P] Criar `MAIN/adapter/in/web/dto/response/ResponsavelResponse.java` (`id`, `nome`)
- [x] T011 Alterar `MAIN/adapter/in/web/dto/response/SolicitacaoResponse.java`: `responsaveis` e `abertaPorNome` no fim do registro, nova fábrica com nomes; as fábricas atuais passam `null` (T009 verde). Corrigir os testes existentes que constroem o registro com construtor posicional, se houver
- [x] T012 Testes em `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java`: `buscarPorId` devolve `responsaveis` e `abertaPorNome` do `Output`; `listar` chama `listarResponsaveisBatch` uma vez e `resolverNomes` **uma** vez para a página inteira e monta os nomes por item; item sem responsável sai com lista vazia; ações (`triar`, `devolver`, `gerenciarResponsaveis` e semelhantes) e o evento publicado seguem sem os campos novos; `verifyNoMoreInteractions`
- [x] T013 Alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java`: `buscarPorId` e `listar` passam os nomes à resposta (T012 verde)

## Fase 4 — Integração e documentação

- [x] T014 Alterar `openspec/README.md`: acrescentar `specs/009-nome-dos-responsaveis-na-solicitacao-e` às linhas de `solicitacoes-kanban` (nomes na resposta) e `galeria-modelo` (capa atômica), conforme o Princípio 12; sem tocar `openspec/specs/`
- [x] T015 `make validate` verde (rodado pelo usuário na VPS em 2026-10-09: todos os testes e o gate de cobertura passaram) (formatação, lint, testes, cobertura ≥ 95% por arquivo tocado)

## Rastreabilidade

| Requisito | Tarefas | Teste que prova o cenário da spec |
|---|---|---|
| RF-01 | T003, T005, T009, T010, T011, T012, T013 | "Operador vê os nomes no detalhe" |
| RF-02 | T003, T005, T009, T011, T012, T013 | "Operador vê os nomes no detalhe" |
| RF-03 | T003, T005, T009, T011, T012, T013 | "Listagem traz os nomes sem consulta por linha" |
| RF-04 | T009, T011, T012 | "Campos antigos continuam na resposta" |
| RF-05 | T003, T005, T012 | "Solicitação sem responsável" |
| RF-06 | T003, T005 | "Responsável inativo continua com nome" |
| RF-07 | T012, T013 (o acesso segue decidido antes, em `execute`, coberto por T003) | "Operador vê os nomes no detalhe" |
| RF-08 | T009, T011, T012, T013 | "Respostas de ação e eventos ficam como estão" |
| RF-09 | T001, T002, T004, T006, T007, T008 | "Definir a segunda foto como capa" |
| RF-10 | T007, T008 | "Falha ao gravar a nova capa desfaz a limpeza" |
| RF-11 | T002, T004 | "Editar a legenda não mexe na capa" |
| RF-12 | T003, T005 | "Solicitação sem responsável" e testes de nome nulo |
| RNF-01 | T003, T005, T012, T013 | "Listagem traz os nomes sem consulta por linha" (uma chamada de nomes por página) |
| RNF-02 | T009, T011, T015 | "Campos antigos continuam na resposta" |
| RNF-03 | T007, T008 | "Falha ao gravar a nova capa desfaz a limpeza" |
| RNF-04 | T015 | gate de cobertura do `make validate` |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-10-09
| Requisito | Estado | Evidência |
|---|---|---|
| RF-01 | realizado | `ObterSolicitacaoUseCase.java:44,79-80` monta `responsaveis` na ordem de `responsavelIds`; `SolicitacaoController.java:386-391` entrega no detalhe; teste `shouldReturnNamesInResponsavelIdsOrderWhenSolicitacaoHasAssigneesAndOpener` e `shouldReturnNamesAndKeepIdsWhenSolicitacaoIsFetchedById` |
| RF-02 | realizado | `ObterSolicitacaoUseCase.java:45,83`; nulo quando não achado (`shouldReturnNullNameAndKeepOrderWhenAssigneeAndOpenerAreNotFound`) |
| RF-03 | realizado | `SolicitacaoController.java:285-307` preenche os dois campos em cada item; `shouldReturnNamesInOneLookupWhenSolicitacoesAreListed` |
| RF-04 | realizado | `SolicitacaoResponse.java` mantém `responsavelIds` e `abertaPorUsuarioId`; campos novos só no fim do registro; `SolicitacaoResponseTest` |
| RF-05 | realizado | `shouldReturnEmptyResponsaveisWhenSolicitacaoHasNoAssignee` e `$.content[1].responsaveis.length()` = 0 na listagem |
| RF-06 | realizado | `findAllByIdIn` não filtra situação; `shouldKeepNameWhenAssigneeIsInactive` |
| RF-07 | realizado | nomes só são resolvidos depois de `validarLeitura` (`ObterSolicitacaoUseCase.java:76-78`); os testes de acesso negado seguem sem consulta de nomes |
| RF-08 | realizado | fábricas `from(s)` e `from(s, ids)` passam `null` (`SolicitacaoResponse.java:53-59`); `shouldNotReturnNamesNorPublishThemWhenSolicitacaoIsOpened` confere resposta e evento |
| RF-09 | realizado | `FotoGaleriaModeloRepositoryAdapter.java:49-52` (`@Transactional salvarComoPrincipal`); `EditarFotoGaleriaUseCase.java:52`; `shouldDeixarSoANovaComoCapaWhenSalvarComoPrincipal` |
| RF-10 | realizado | `FotoGaleriaModeloRepositoryAdapterPostgresTest.shouldManterACapaAntigaWhenGravarANovaFalha` (falha real: identificação de 300 caracteres em coluna de 255) |
| RF-11 | realizado | `EditarFotoGaleriaUseCaseTest.shouldSalvarSemLimparCapaWhenPrincipalFalso` e `deveRenomearIdentificacao` |
| RF-12 | realizado | `ObterSolicitacaoUseCaseTest.shouldReturnNullNameAndKeepOrderWhenAssigneeAndOpenerAreNotFound` |
| RNF-01 | realizado | uma chamada a `resolverNomes` por página (`SolicitacaoController.java:291`), conferida por `verify(..., times(1))` no teste da listagem |
| RNF-02 | realizado | nenhum campo existente alterado (diff de `SolicitacaoResponse.java`); sem migração |
| RNF-03 | realizado | índice único parcial já existente + teste Postgres: 1 capa depois da troca e depois da falha |
| RNF-04 | realizado | gate de cobertura do `make validate` |

`make validate`: rodado **pelo usuário na VPS**, em 2026-10-09, depois do ajuste de formatação do commit `2d09383`; o usuário informou que todos os testes passaram. A saída não foi colada nesta sessão, então o veredito é relato do usuário, não saída vista pelo agente. Nesta sessão nada foi compilado nem executado (sem Java e sem acesso ao Maven).

Cenários BDD: o projeto não tem `app/tests/bdd` (desvio herdado, ver topo de `tasks.md`); os 9 cenários da spec estão provados pelos testes citados acima.

Não verificado: que o teste Postgres da galeria **falha sem o `@Transactional`**, exigência do critério de aceite da issue #115. Ninguém removeu a anotação para ver o teste quebrar.

Excesso de escopo: nenhum. A remoção de `limparPrincipal` da porta e do adaptador estava no plano; o método JPA continua. Fora de escopo respeitado: eventos e respostas de ação sem campos novos, nenhuma mudança de acesso, nenhum ajuste de frontend.

Veredito: convergido
Tarefas acrescentadas: nenhuma
