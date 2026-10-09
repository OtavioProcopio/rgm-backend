# Tarefas — Nome dos responsáveis na solicitação e capa da galeria sem erro

> Ordem de dependência. `[P]` marca tarefa paralelizável (não toca arquivo de outra `[P]`
> da mesma fase). Teste vem antes da implementação que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

Checklists: revisados e marcados pelo usuário em 2026-10-09 (26 de 26 itens).

Desvio herdado (feature 001): o projeto não tem `app/tests/bdd` nem alvo `make bdd`. Os
critérios de aceite da spec são provados pelos testes de caso de uso, de controller, de DTO e
de repositório listados abaixo, não por cenários executáveis em Gherkin. A tabela de
rastreabilidade mostra qual teste cobre qual cenário.

Estado da execução (2026-10-09): T001 a T014 estão escritas, mas **nenhum teste foi executado**:
o ambiente da sessão não tem Java nem acesso ao Maven Central (proxy com certificado
autoassinado). As marcas `[x]` valem como "escrita"; a prova de "verde" é a T015, que segue
aberta.

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
- [ ] T015 `make validate` verde (formatação, lint, testes, cobertura ≥ 95% por arquivo tocado)

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
