# Tarefas — Autor da solicitação anexa e vê evidências

> Ordem de dependência. `[P]` marca tarefa paralelizável (não toca arquivo de outra `[P]`
> da mesma fase). Teste vem antes da implementação que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

## Fase 1 — Domínio

- [x] T001 Teste unitário de `AcessoEvidenciaSolicitacao` em `TEST/core/domain/validation/AcessoEvidenciaSolicitacaoTest.java`: inativo recusado; GESTOR e ADMINISTRADOR liberados sem consultar atribuição; responsável liberado em qualquer tipo; autor liberado na listagem; autor liberado no anexo de ABERTURA e GERAL; autor recusado nos outros quatro tipos; operador sem relação recusado
- [x] T002 Implementar `MAIN/core/domain/validation/AcessoEvidenciaSolicitacao.java`

## Fase 2 — Aplicação

- [x] T003 Testes em `TEST/core/application/usecases/evidencia/AnexarEvidenciaUseCaseTest.java` para os cenários de anexo da spec (autor anexa ABERTURA; autor anexa depois da triagem; autor recusado em tipo não permitido; autor que é responsável anexa SERVICO_REALIZADO; operador sem relação recusado; autor recusado em solicitação encerrada; inativo recusado)
- [x] T004 Alterar `MAIN/core/application/usecases/evidencia/AnexarEvidenciaUseCase.java` para delegar a `AcessoEvidenciaSolicitacao`
- [x] T005 Testes em `TEST/core/application/usecases/evidencia/VisualizarEvidenciaUseCaseTest.java` para os cenários de listagem da spec (autor lista; autor lista solicitação encerrada; operador sem relação recusado; gestor lista sem ser responsável; inativo recusado)
- [x] T006 Alterar `MAIN/core/application/usecases/evidencia/VisualizarEvidenciaUseCase.java` para delegar a `AcessoEvidenciaSolicitacao`

## Fase 3 — Adapters e infra

Sem tarefas: controller, DTO, repositório e configuração não mudam.

## Fase 4 — Integração e BDD

- [x] T007 Atualizar a regra de acesso a evidências em `docs/casos-de-uso.md`
- [x] T008 `make validate` verde

Desvio declarado no plano: o projeto não tem `app/tests/bdd` nem Cucumber. Cada cenário de
aceite é coberto por um teste unitário nas tarefas T001, T003 e T005.

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01 | T001, T002, T003, T004 |
| RF-02 | T001, T002, T005, T006 |
| RF-03 | T001, T002, T003, T005 |
| RF-04 | T003, T004 |
| RF-05 | T001, T002, T003, T005 |
| RF-06 | T001, T002, T004, T006 |
| RF-07 | T001, T002, T003, T004 |
| RF-08 | T001, T002, T003, T005 |
| RNF-01 | T004, T006, T008 |
| RNF-02 | T001, T003, T005, T008 |
| RNF-03 | T001, T002 |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-10-05

- **Resultado:** convergiu. Os 8 requisitos funcionais têm teste que os exercita; nenhum
  arquivo fora do plano foi alterado.
- **`make validate`:** verde. 583 testes, 0 falhas, 0 ignorados; cobertura de linha do
  conjunto 95,82% (antes 95,60%) e de ramo 80,00% (antes 79,23%).
- **Cobertura por arquivo alterado (linha):** `AcessoEvidenciaSolicitacao` 100%,
  `VisualizarEvidenciaUseCase` 100% (antes 79%), `AnexarEvidenciaUseCase` 96,8%.
- **Desvios:** sem cenários em `app/tests/bdd` (declarado no plano). Os testes de T003 e
  T005 foram escritos depois das alterações de T004 e T006, não antes; só T001 foi visto
  falhar antes da implementação. Os itens de `checklists/` seguem sem marcação de revisor.
- **Fora do escopo, observado:** os testes antigos de `AnexarEvidenciaUseCaseTest` e
  `VisualizarEvidenciaUseCaseTest` não tinham nenhum caso de 403.
