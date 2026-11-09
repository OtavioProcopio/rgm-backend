# Plano de implementação — Instrução da triagem como campo da solicitação

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.
> `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.
> Base: `develop` com a spec 014. Publicação: junto do front (spec 018), backend primeiro.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Cópia do agregado | `Solicitacao.Builder` aninhado, com `toBuilder()` privado: cada transição diz só o que muda (`toBuilder().status(X).atualizadaEm(agora).build()`); o construtor vira privado e é chamado em **um** ponto (`build()`) | (a) Lombok `@Builder`/`@With`; (b) `record` com `with`; (c) agregado mutável com setters; (d) manter o construtor e só adicionar o argumento | (a) `core` não importa framework nem biblioteca externa (P7); (b) o construtor canônico de 17 componentes tem o mesmo problema e o agregado tem comportamento e invariantes; (c) quebra a imutabilidade e a leitura segura entre threads; (d) é o problema atual |
| Reidratação | O mapper usa `Solicitacao.builder()` público (mesmo `build()`, mesmas invariantes) | Fábrica estática com 17 parâmetros | Nomeia cada campo; evita trocar dois `Instant` |
| Dados de teste | `SolicitacaoTestBuilder` (Test Data Builder) em `TEST/support`, com padrões válidos por status (`umaSolicitacao().emAndamento().comInstrucao("…").build()`) | Mother com um método por cenário; construtor direto em cada teste | Um ponto de mudança; nomeia o que importa para o teste e esconde o resto |
| Guarda contra campo esquecido | Teste `SolicitacaoCopiaTest`: monta uma solicitação com **todos** os campos preenchidos, aplica cada transição e compara, com `usingRecursiveComparison().ignoringFields(...)`, que só os campos declarados da transição mudaram | Confiar na revisão | Um campo novo que a cópia esquecer faz o teste falhar; é o teste que justifica o `toBuilder()` |
| Onde mora a regra "só de A_FAZER" | No agregado: `triar` recusa qualquer status que não seja `A_FAZER` com `TransicaoStatusInvalidaException` (409 pelo `GlobalExceptionHandler`) | Checar no caso de uso | A regra é do domínio; o caso de uso e qualquer outro chamador a herdam |
| Onde mora a regra "instrução obrigatória" | No agregado (`triar` exige texto não vazio e ≤ 2000) **e** no DTO (`@NotBlank`, `@Size`) | Só no DTO | O caso de uso é chamado fora do controller; o DTO dá o 400 cedo |
| Invariante do estado | `instrucao` é nula em `A_FAZER`; fora disso é livre (triadas antigas são nulas) | Exigir instrução em todo `EM_ANDAMENTO` | RF-01: o dado legado não tem instrução e não será inventado |
| Esquema | `V10__solicitacao_instrucao.sql`: `ALTER TABLE solicitacoes ADD COLUMN instrucao TEXT` anulável, sem valor padrão nem reescrita | `NOT NULL` com valor "—"; preencher a partir dos comentários | RF-01, RNF-05; Princípio 9 (migração nova, número sequencial) |
| Listagem | `SolicitacaoResponse` ganha `instrucao` e o `from` a preenche sempre, como `descricao` | Só no detalhe | Eventos e listagem usam o mesmo tipo; o peso é o mesmo da `descricao` (RF-07) |
| PDF | `ModeloPdfService` escreve "Instrução: …" logo após "Descrição: …" quando houver | Seção própria | RF-09; segue o formato atual |
| Corpo antigo | `descricao` no corpo vira propriedade desconhecida e a falta de `instrucao` dá 400 | Aceitar os dois nomes | RF-06; aceitar os dois esconderia cliente desatualizado |

## Fases

1. **Refatoração sem mudança de comportamento** (RNF-01 a RNF-03): builder, `toBuilder()`, mapper, `SolicitacaoTestBuilder`, migração dos ~50 usos nos testes, `SolicitacaoCopiaTest`. `make validate` verde **antes** da fase 2. Um PR/commit só dela.
2. **Campo e triagem** (RF-01 a RF-09, RNF-05): migração, agregado, entidade, caso de uso, DTOs, PDF.
3. **Documentação e validação**.

## Arquivos a criar ou alterar

### Fase 1 — Refatoração

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| domain | `MAIN/core/domain/model/aggregates/Solicitacao.java` (`Builder`, `toBuilder`, construtor privado, 9 cópias reescritas) | alterar | `TEST/core/domain/model/SolicitacaoTest.java` |
| per | `MAIN/adapter/out/persistence/mapper/SolicitacaoMapper.java` (`toDomain` pelo builder) | alterar | `TEST/adapter/out/persistence/mapper/MapperRoundtripTest.java` |
| test | `TEST/support/SolicitacaoTestBuilder.java` | criar | `TEST/support/SolicitacaoTestBuilderTest.java` (padrões válidos por status) |
| test | `TEST/core/domain/model/SolicitacaoCopiaTest.java` | criar | — |
| test | os ~24 arquivos que hoje usam `new Solicitacao(` | alterar (mecânico) | — |

### Fase 2 — Campo e triagem

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| db | `app/src/main/resources/db/migration/V10__solicitacao_instrucao.sql` | criar | `TEST/adapter/out/persistence/FlywayMigrationTest.java` |
| domain | `Solicitacao.java` (`instrucao`, `getInstrucao`, `triar(prioridade, instrucao, agora)`, guarda de status) | alterar | `SolicitacaoTest.java`, `SolicitacaoCopiaTest.java` |
| per | `SolicitacaoJpaEntity.java`, `SolicitacaoMapper.java` | alterar | `MapperRoundtripTest.java`, `SolicitacaoRepositoryAdapterPostgresTest` (ida e volta no Postgres) |
| uc | `TriarSolicitacaoUseCase.java` (`Input.instrucao`, sem comentário, 2 atividades) | alterar | `TriarSolicitacaoUseCaseTest.java` |
| web | `TriarSolicitacaoRequest.java` (`instrucao`), `SolicitacaoController.java` | alterar | `SolicitacaoControllerTest.java`, `SolicitacaoControllerSegurancaTest.java` |
| web | `SolicitacaoResponse.java` (`instrucao`) | alterar | `SolicitacaoResponseTest.java` |
| pdf | `ModeloPdfService.java` ("Instrução: …") | alterar | `ModeloPdfServiceTest.java` (PDF lido de volta, com e sem instrução) |
| docs | `docs/casos-de-uso.md` (UC-03), `docs/modelo_dados.md`, `openspec/README.md` | alterar | — |

## Contrato entre camadas

- **Entrada:** `TriarSolicitacaoRequest { prioridade, responsavelIds, instrucao }`.
- **Domínio:** `Solicitacao.triar(prioridade, instrucao, agora)` recusa status ≠ `A_FAZER` (409) e instrução em branco ou longa (`ValidationException` → 400).
- **Saída:** `SolicitacaoResponse.instrucao` (String, nula quando não houver).
- **Compatibilidade:** campo de resposta aditivo; corpo de `/triar` quebra, declarado na spec.

## Dependências externas

Nenhuma. AssertJ já vem com o starter de teste.

## Impacto no contrato de operação

Uma migração Flyway (`V10`). Nenhum alvo ou variável novo.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| A refatoração esquece de copiar um campo numa transição | média | `SolicitacaoCopiaTest` com comparação recursiva de todos os campos; roda antes da fase 2 |
| Migração dos ~50 usos de teste mascara um teste que dependia de um padrão | baixa | Fase 1 não altera nenhuma asserção (RNF-03); revisão do diff só de construção |
| `triar` recusar `EM_VALIDACAO` quebra um fluxo que alguém usa | baixa | O caso nunca foi documentado (UC-03 diz `A_FAZER → EM_ANDAMENTO`); teste de 409 por status; `devolver` continua sendo o caminho de `EM_VALIDACAO → EM_ANDAMENTO` |
| Front antigo recebe 400 ao triar | alta se publicado sozinho | Ordem de publicação na spec |
| Triadas antigas aparecem sem instrução | certa | Declarado; o front trata o nulo (spec 018) |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Testes provam a entrega | Todo arquivo novo tem teste espelhado; PDF lido de volta; Postgres real para a coluna; cenários rastreados em `tasks.md`; sem teste escrito só para cobrir linha |
| Simplicidade defensável | Um padrão (Builder com `toBuilder`) para um problema medido (9 cópias); quatro alternativas descartadas por escrito |
| P7 — mapa de camadas | Builder dentro de `core.domain`, sem Spring nem biblioteca; mapper em `adapter.out` |
| P8 — linguagem ubíqua | `instrucao`, `triagem`; testes `shouldXWhenY` |
| P9 — compatibilidade | Resposta aditiva; quebra do corpo declarada com ordem de publicação; migração `V10` nova, sem alterar `V1` a `V9` |
| P10 — cobertura | Não baixa o mínimo; ≥ 95% por arquivo tocado |
| P11 — padrão de teste | Testes novos e alterados em AAA, um comportamento por teste |
| P12 — uma fonte | Spec em `specs/015`; linha em `openspec/README.md` |
