# Tarefas — Usuário desativado perde o acesso imediatamente

> Ordem de dependência. `[P]` marca tarefa paralelizável. Teste vem antes da implementação
> que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

## Fase 1 — Domínio

Sem tarefas.

## Fase 2 — Aplicação

Sem tarefas.

## Fase 3 — Adapters e infra

- [x] T001 Testes em `TEST/adapter/out/security/JwtAuthenticationFilterTest.java`: usuário ativo é autenticado; usuário inativo segue sem autenticação; usuário inexistente segue sem autenticação
- [x] T002 Alterar `MAIN/adapter/out/security/JwtAuthenticationFilter.java`
- [x] T003 Criar `TEST/adapter/in/web/solicitacao/SolicitacaoSseControllerTest.java`: usuário ativo abre a conexão; inativo recebe 401; inexistente recebe 401
- [x] T004 Alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoSseController.java`

## Fase 4 — Integração e BDD

- [x] T005 Atualizar `docs/casos-de-uso.md` (UC-01 e seção de eventos)
- [x] T006 `make validate` verde

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01 | T001, T002 |
| RF-02 | T001, T002 |
| RF-03 | T001, T002, T003, T004 |
| RF-04 | T003, T004 |
| RF-05 | T002, T006 |
| RNF-01 | T001, T002 |
| RNF-02 | T001, T002 |
| RNF-03 | T002, T006 |
| RNF-04 | T001, T003, T006 |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-10-05

- **Resultado:** convergiu em RF-01 a RF-05.
- **`make validate`:** verde, 623 testes no conjunto da branch.
- **Ordem:** spec, plano e tarefas escritos antes dos testes; testes de T001 e T003 vistos
  falhando antes de T002 e T004.
- **Desvios:** sem cenários em `app/tests/bdd`. Checklist sem revisão humana.
  `SolicitacaoSseController` ganhou teste, mas continua na lista de exclusões do JaCoCo.
