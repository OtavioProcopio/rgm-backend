# Tarefas — Heartbeat nas conexões SSE

> Ordem de dependência. `[P]` marca tarefa paralelizável. Teste vem antes da implementação
> que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

## Fase 1 — Domínio

Sem tarefas.

## Fase 2 — Aplicação

Sem tarefas.

## Fase 3 — Adapters e infra

- [x] T001 Testes em `TEST/adapter/in/web/solicitacao/SolicitacaoEventPublisherTest.java`: sinal chega a toda conexão; conexão com falha de envio é descartada; conexão encerrada é descartada; sem conexões não há erro
- [x] T002 Alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoEventPublisher.java`: sinal periódico de 25 s e envio único
- [x] T003 Criar `MAIN/adapter/config/SchedulingConfig.java`
- [x] T004 Alterar o tempo limite em `MAIN/adapter/in/web/solicitacao/SolicitacaoSseController.java` para 30 min

## Fase 4 — Integração e BDD

- [x] T005 Documentar sinal e tempo limite em `docs/casos-de-uso.md`
- [x] T006 `make validate` verde

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01 | T001, T002, T003 |
| RF-02 | T001, T002 |
| RF-03 | T001, T002 |
| RF-04 | T004 |
| RNF-01 | T002 |
| RNF-02 | T004 |
| RNF-03 | T002, T006 |
| RNF-04 | T001, T006 |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-10-05

- **Resultado:** convergiu nos requisitos RF-01 a RF-03; RF-04 implementado sem teste.
- **`make validate`:** verde, 612 testes no conjunto da branch.
- **Cobertura (linha):** `SolicitacaoEventPublisher` 100%, `SchedulingConfig` 100%.
- **Desvios:** spec, plano e tarefas foram escritos depois do código. Os testes de T001
  foram vistos falhando antes de T002. Sem cenários em `app/tests/bdd`. Checklist sem
  revisão humana. Duas ambiguidades aguardam confirmação na spec.
