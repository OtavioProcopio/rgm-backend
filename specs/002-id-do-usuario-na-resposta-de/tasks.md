# Tarefas — Id do usuário na resposta de login

> Ordem de dependência. `[P]` marca tarefa paralelizável. Teste vem antes da implementação
> que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

## Fase 1 — Domínio

Sem tarefas: o domínio não muda.

## Fase 2 — Aplicação

- [x] T001 Teste em `TEST/core/application/usecases/auth/LoginUseCaseTest.java`: a saída do login traz o `id` do usuário autenticado
- [x] T002 Alterar `MAIN/core/application/usecases/auth/LoginUseCase.java`: `Output` ganha `id`

## Fase 3 — Adapters e infra

- [x] T003 Testes em `TEST/adapter/in/web/auth/AuthControllerTest.java`: 200 traz `id`; 200 mantém os quatro campos; 401 não traz `id`
- [x] T004 Alterar `MAIN/adapter/in/web/dto/response/LoginResponse.java` e `MAIN/adapter/in/web/auth/AuthController.java` para repassar `id`

## Fase 4 — Integração e BDD

- [x] T005 Atualizar UC-01 em `docs/casos-de-uso.md`
- [x] T006 `make validate` verde

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01 | T001, T002, T003, T004 |
| RF-02 | T003, T004 |
| RF-03 | T003 |
| RNF-01 | T003, T004 |
| RNF-02 | T001, T003, T006 |
| RNF-03 | T001, T002 |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-10-05

- **Resultado:** convergiu em RF-01 a RF-03.
- **`make validate`:** verde, 586 testes na rodada desta feature (612 no conjunto da branch).
- **Cobertura (linha):** `LoginUseCase`, `LoginResponse` e `AuthController` com 100%.
- **Desvios:** sem cenários em `app/tests/bdd`. Checklist sem revisão humana.
