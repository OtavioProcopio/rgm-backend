# Plano de implementação — Descrição do que fazer, obrigatória na triagem

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.
> `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.
> Base: `develop` com a spec 013.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Onde gravar | Atividade de comentário da solicitação, criada pelo caso de uso na transação da triagem, logo após a mudança de status (mesmo padrão do motivo em `DevolverSolicitacaoUseCase`) | (a) coluna `instrucao_triagem` na solicitação; (b) novo `TipoAtividadeSolicitacao` | (a) e (b) exigem migração e mexem em PDF, timeline e tipos do front; o histórico já é lido por quem executa. **Pendente de confirmação do usuário**: se a instrução precisar aparecer fora do histórico (card, detalhe), vale (a) em spec própria |
| Validação | Bean Validation no DTO (`@NotBlank`, `@Size` com `LimitesTexto.COMENTARIO`) **e** checagem no caso de uso | Só no DTO | O caso de uso é chamado fora do controller; a regra de negócio não pode depender da borda |
| Erro em caso de ausência | 400 pelo DTO; `BusinessRuleException` no caso de uso | 422 | Mesmo tratamento dos demais campos obrigatórios |
| Espaços | `trim` antes de gravar (a entidade já normaliza com `optionalTrimToNull`) | Gravar cru | RF-03 |

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| web | `MAIN/adapter/in/web/dto/request/TriarSolicitacaoRequest.java` (campo `descricao`) | alterar | `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java` |
| web | `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java` (repassa `descricao`) | alterar | `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java` |
| uc | `MAIN/core/application/usecases/solicitacao/TriarSolicitacaoUseCase.java` (`Input.descricao`, validação, atividade de comentário) | alterar | `TEST/core/application/usecases/solicitacao/TriarSolicitacaoUseCaseTest.java` |
| docs | `docs/casos-de-uso.md` (UC-03), `openspec/README.md` (linha da tabela) | alterar | — |

## Contrato entre camadas

- **Entrada:** `TriarSolicitacaoRequest { prioridade, responsavelIds, descricao }`, `descricao` não vazia, até 2000.
- **Aplicação:** valida antes de qualquer leitura; grava solicitação, atribuições, 3 atividades (atribuição, mudança de status, comentário) na mesma transação.
- **Compatibilidade:** quebra declarada na spec (campo obrigatório novo); nada removido.

## Dependências externas

Nenhuma.

## Impacto no contrato de operação

Nenhum. Sem variável, alvo ou migração.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Front antigo recebe 400 ao triar | alta se publicado sozinho | Ordem de publicação na spec: juntos, backend primeiro |
| Descrição aparece como "comentário" e é confundida com conversa | média | Texto do histórico já distingue por posição (após a atribuição); campo próprio fica como evolução |
| Triagens antigas sem descrição | certa | Fora de escopo; não se preenche retroativamente |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Testes provam a entrega | Caso de uso e controller com teste espelhado; cobertura ≥ 95% nos arquivos tocados |
| Simplicidade defensável | Sem migração, sem tipo de atividade novo |
| P7 — mapa de camadas | Regra no caso de uso, sem Spring novo em `core` |
| P8 — linguagem ubíqua | `descricao`, `triagem` |
| P9 — compatibilidade | Quebra declarada na spec, com ordem de publicação; nenhum campo existente removido |
| P10 — cobertura | Não baixa o mínimo |
| P11 — padrão de teste | Testes novos `shouldXWhenY`, AAA |
