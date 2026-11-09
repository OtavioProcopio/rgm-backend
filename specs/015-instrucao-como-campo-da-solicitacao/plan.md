# Plano de implementação — Instrução da triagem como campo da solicitação

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.
> `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.
> Base: `develop` com a spec 014. Publicação: backend completo primeiro, front (spec 018) depois.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Cópia do agregado | `Solicitacao.Builder` aninhado, com `toBuilder()` privado: cada transição diz só o que muda (`toBuilder().status(X).atualizadaEm(agora).build()`); o construtor vira privado e é chamado em **um** ponto (`build()`) | (a) Lombok `@Builder`/`@With`; (b) `record` com `with`; (c) agregado mutável com setters; (d) manter o construtor e só adicionar o argumento | (a) `core` não importa framework nem biblioteca externa (P7); (b) o construtor canônico de 17 componentes tem o mesmo problema e o agregado tem comportamento e invariantes; (c) quebra a imutabilidade e a leitura segura entre threads; (d) é o problema atual |
| Reidratação | O mapper usa `Solicitacao.builder()` público (mesmo `build()`, mesmas invariantes) | Fábrica estática com 17 parâmetros | Nomeia cada campo; evita trocar dois `Instant` |
| Dados de teste | Os 51 usos existentes migram para `Solicitacao.builder()`, omitindo os campos nulos (o `Builder` já é nulo por padrão); testes novos usam `SolicitacaoTestBuilder` em `TEST/support`, com padrões válidos por status (`umaSolicitacao().emAndamento().comInstrucao("…").build()`), criado junto do primeiro teste que o usa | Migrar tudo para o `SolicitacaoTestBuilder` de uma vez; construtor direto em cada teste | A migração mecânica não muda semântica e fecha o construtor; o builder de dados entra quando houver teste novo que precise dele, sem código sem uso |
| Guarda contra campo esquecido | Teste `SolicitacaoCopiaTest`: monta uma solicitação com **todos** os campos preenchidos, aplica cada transição e compara, com `usingRecursiveComparison().ignoringFields(...)`, que só os campos declarados da transição mudaram | Confiar na revisão | Um campo novo que a cópia esquecer faz o teste falhar; é o teste que justifica o `toBuilder()` |
| Onde mora a regra "só de A_FAZER" | No agregado: `triar` recusa qualquer status que não seja `A_FAZER` com `TransicaoStatusInvalidaException` (409 pelo `GlobalExceptionHandler`) | Checar no caso de uso | A regra é do domínio; o caso de uso e qualquer outro chamador a herdam |
| Onde mora a regra "instrução obrigatória" | No agregado (`triar` exige texto não vazio e ≤ 2000) **e** no DTO (`@NotBlank`, `@Size`) | Só no DTO | O caso de uso é chamado fora do controller; o DTO dá o 400 cedo |
| Invariante do estado | `instrucao` é nula em `A_FAZER`; fora disso é livre (triadas antigas são nulas) | Exigir instrução em todo `EM_ANDAMENTO` | RF-01: o dado legado não tem instrução e não será inventado |
| Esquema | `V10__solicitacao_instrucao.sql`: `ALTER TABLE solicitacoes ADD COLUMN instrucao TEXT` anulável, sem valor padrão nem reescrita | `NOT NULL` com valor "—"; preencher a partir dos comentários | RF-01, RNF-05; Princípio 9 (migração nova, número sequencial) |
| Listagem | `SolicitacaoResponse` ganha `instrucao` e o `from` a preenche sempre, como `descricao` | Só no detalhe | Eventos e listagem usam o mesmo tipo; o peso é o mesmo da `descricao` (RF-07) |
| PDF | `ModeloPdfService` escreve "Instrução: …" logo após "Descrição: …" quando houver | Seção própria | RF-09; segue o formato atual |
| Corpo antigo | `descricao` no corpo vira propriedade desconhecida e a falta de `instrucao` dá 400 | Aceitar os dois nomes | RF-06; aceitar os dois esconderia cliente desatualizado |
| Edição | Método `Solicitacao.editarInstrucao(nova, agora)`: valida como a triagem, devolve a própria instância se o texto aparado é igual, recusa status ≠ `EM_ANDAMENTO` com `ConflitoException` (409 pelo `GlobalExceptionHandler`) | Reusar `editar` (que muda título e descrição e vale em `A_FAZER` pelo autor); reusar `TransicaoStatusInvalidaException` | `editar` tem outras regras e outro público; não é transição de status, então o nome do erro de transição enganaria |
| Quem edita | Caso de uso `EditarInstrucaoSolicitacaoUseCase` confere `perfil.podeTriar()` (gestor e administrador) e usuário ativo, e lança `NaoAutorizadoException` (403) | Regra só na rota | Mesma regra de `AlterarResponsaveis`; a regra mora no caso de uso, não no controller |
| Histórico da correção | Novo valor `INSTRUCAO_ALTERADA` em `TipoAtividadeSolicitacao`, criado por `AtividadeSolicitacao.instrucaoAlterada(solicitacaoId, textoAnterior, autorId, agora)`, com o texto anterior em `comentario` | Atividade `COMENTARIO` com prefixo; coluna de auditoria; tabela de versões da instrução | A coluna `tipo` é `VARCHAR(50)` sem restrição, então não há migração; o texto anterior cabe em `comentario`; uma tabela de versões é mais do que o requisito pede |
| Ação oferecida | `AcaoSolicitacao.EDITAR_INSTRUCAO`, em `AcoesPermitidasSolicitacao` quando `perfil.podeTriar()` e status `EM_ANDAMENTO` | Deixar o front deduzir | O front segue `acoesPermitidas` da API (spec 002 do front); regra de permissão não se duplica no cliente |
| Concorrência | O agregado carrega `version`; a segunda de duas edições simultâneas falha no `save` e o handler existente responde 409 | Lock próprio | Já é o mecanismo do projeto (issue #80) |
| Evento | `publicar("instrucao_editada", SolicitacaoResponse.from(...))` e, como o comentário, `solicitacao_atividade` com `SolicitacaoAtividadeEvent("instrucao_editada", id)` | Só o primeiro | O evento de solicitação atualiza o detalhe; só o de atividade faz o histórico do outro usuário recarregar (spec 018) |

## Fases

1. **Refatoração sem mudança de comportamento** (RNF-01 a RNF-03): builder, `toBuilder()`, mapper, `SolicitacaoTestBuilder`, migração dos ~50 usos nos testes, `SolicitacaoCopiaTest`. `make validate` verde **antes** da fase 2. Um PR/commit só dela.
2. **Campo e triagem** (RF-01 a RF-09, RNF-05): migração, agregado, entidade, caso de uso, DTOs, PDF.
3. **Edição da instrução** (RF-10 a RF-15): método do agregado, atividade, ação permitida, caso de uso, rota, evento.
4. **Documentação e validação**.

## Arquivos a criar ou alterar

### Fase 1 — Refatoração

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| domain | `MAIN/core/domain/model/aggregates/Solicitacao.java` (`Builder`, `toBuilder`, construtor privado, 9 cópias reescritas) | alterar | `TEST/core/domain/model/SolicitacaoTest.java` |
| per | `MAIN/adapter/out/persistence/mapper/SolicitacaoMapper.java` (`toDomain` pelo builder) | alterar | `TEST/adapter/out/persistence/mapper/MapperRoundtripTest.java` |
| test | `TEST/support/SolicitacaoTestBuilder.java` (criado na Fase 2, junto do primeiro teste que o usa) | criar | `TEST/support/SolicitacaoTestBuilderTest.java` (padrões válidos por status) |
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

### Fase 3 — Edição da instrução

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| domain | `Solicitacao.java` (`editarInstrucao`) | alterar | `SolicitacaoTest.java`, `SolicitacaoCopiaTest.java` |
| domain | `TipoAtividadeSolicitacao.java` (`INSTRUCAO_ALTERADA`), `AtividadeSolicitacao.java` (`instrucaoAlterada`) | alterar | `TEST/core/domain/model/AtividadeSolicitacaoTest.java`, `AtividadeSolicitacaoRepositoryAdapterTest.java` |
| domain | `AcaoSolicitacao.java` (`EDITAR_INSTRUCAO`), `AcoesPermitidasSolicitacao.java` | alterar | `TEST/core/domain/validation/AcoesPermitidasSolicitacaoTest.java` |
| uc | `MAIN/core/application/usecases/solicitacao/EditarInstrucaoSolicitacaoUseCase.java` | criar | `TEST/core/application/usecases/solicitacao/EditarInstrucaoSolicitacaoUseCaseTest.java` |
| config | `MAIN/adapter/config/UseCaseConfig.java` | alterar | `TEST/ApiApplicationTests.java` |
| web | `MAIN/adapter/in/web/dto/request/EditarInstrucaoRequest.java`, `SolicitacaoController.java` (`PATCH /{id}/instrucao`) | criar/alterar | `SolicitacaoControllerTest.java`, `SolicitacaoControllerSegurancaTest.java` |
| per | `SolicitacaoRepositoryAdapterPostgresTest` (duas edições com a mesma versão) | alterar | — |

### Documentação

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| docs | `docs/casos-de-uso.md` (UC-03 e a edição), `docs/modelo_dados.md`, `openspec/README.md` | alterar | — |

## Contrato entre camadas

- **Entrada:** `TriarSolicitacaoRequest { prioridade, responsavelIds, instrucao }`.
- **Domínio:** `Solicitacao.triar(prioridade, instrucao, agora)` recusa status ≠ `A_FAZER` (409) e instrução em branco ou longa (`ValidationException` → 400).
- **Edição:** `PATCH /api/solicitacoes/{id}/instrucao` com `EditarInstrucaoRequest { instrucao }`; 200 com `SolicitacaoResponse`; 400, 401, 403, 404, 409.
- **Histórico:** atividade `INSTRUCAO_ALTERADA` com o texto anterior em `comentario`.
- **Saída:** `SolicitacaoResponse.instrucao` (String, nula quando não houver).
- **Compatibilidade:** campo de resposta, rota, evento, ação e tipo de atividade aditivos; corpo de `/triar` quebra, declarado na spec.

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
| Triadas antigas aparecem sem instrução | certa | Declarado; o front trata o nulo (spec 018) |
| Editar a instrução enquanto o responsável executa muda o que ele faz | média | O histórico guarda o texto anterior e o evento atualiza a tela do responsável; sem notificação ativa (fora de escopo) |
| Edição concorrente com envio para validação | baixa | Mesma versão: uma das duas recebe 409; o status é rechecado no agregado |

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
