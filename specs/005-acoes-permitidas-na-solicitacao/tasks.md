# Tarefas — Ações permitidas na solicitação

> Ordem de dependência. `[P]` marca tarefa paralelizável. Teste vem antes da implementação
> que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

## Fase 1 — Domínio

- [x] T001 Teste em `TEST/core/domain/validation/AcoesPermitidasSolicitacaoTest.java`, um por cenário de cálculo da spec
- [x] T002 Criar `MAIN/core/domain/model/enums/AcaoSolicitacao.java`
- [x] T003 Criar `MAIN/core/domain/validation/AcoesPermitidasSolicitacao.java`
- [x] T004 Alterar `MAIN/core/domain/validation/AcessoEvidenciaSolicitacao.java`: consulta `podeListar`

## Fase 2 — Aplicação

- [x] T005 Testes em `TEST/core/application/usecases/solicitacao/ObterSolicitacaoUseCaseTest.java`: devolve as ações do usuário; falha quando o usuário não existe
- [x] T006 Alterar `MAIN/core/application/usecases/solicitacao/ObterSolicitacaoUseCase.java`

## Fase 3 — Adapters e infra

- [x] T007 Testes em `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java`: consulta pelo identificador devolve `acoesPermitidas`; listagem devolve nulo
- [x] T008 Alterar `MAIN/adapter/in/web/dto/response/SolicitacaoResponse.java`, `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java` e `MAIN/adapter/config/UseCaseConfig.java`

## Fase 4 — Integração e BDD

- [x] T009 Tabela de ações em `docs/casos-de-uso.md`
- [x] T010 `make validate` verde

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01 | T005, T006, T007, T008 |
| RF-02 a RF-08 | T001, T002, T003 |
| RF-09 | T001, T003, T004 |
| RF-10 | T001, T003 |
| RF-11 | T007, T008 |
| RNF-01 | T008, T010 |
| RNF-02 | T005, T006 |
| RNF-03 | T007, T008 |
| RNF-04 | T001, T005, T007, T010 |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-10-05

- **Resultado:** convergiu em RF-01 a RF-11.
- **`make validate`:** verde, 612 testes; cobertura de linha do conjunto 95,95% e de ramo
  81,18%.
- **Cobertura (linha):** `AcoesPermitidasSolicitacao` 100%, `AcessoEvidenciaSolicitacao`
  100%, `ObterSolicitacaoUseCase` 100%, `SolicitacaoResponse` 100%,
  `SolicitacaoController` 95,1%.
- **Desvios:** spec, plano e tarefas foram escritos depois do código. Só T001 foi visto
  falhando antes da implementação; T005 e T007 foram escritos junto com o código. Sem
  cenários em `app/tests/bdd`. Checklist sem revisão humana. Cinco ambiguidades aguardam
  confirmação na spec, todas resolvidas por suposição na implementação.
