# Tarefas — Eventos SSE de abertura, edição e responsáveis

> Ordem de dependência. `[P]` marca tarefa paralelizável. Teste vem antes da implementação
> que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

## Fase 1 — Domínio

Sem tarefas.

## Fase 2 — Aplicação

Sem tarefas.

## Fase 3 — Adapters e infra

- [x] T001 Testes em `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java`: abrir publica `aberta`; editar publica `editada`; alterar responsáveis publica `responsaveis_alterados`; cada um exatamente 1 vez
- [x] T002 Alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java` para publicar nos três pontos de entrada

- [x] T005 Testes em `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java`: aviso e resposta de responsáveis trazem a lista; comentar publica `comentada` em `solicitacao_atividade`; comentário recusado não publica
- [x] T006 Criar `MAIN/adapter/in/web/solicitacao/SolicitacaoAtividadeEvent.java` e alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java` (lista de responsáveis e aviso de comentário)
- [x] T007 Testes em `TEST/adapter/in/web/evidencia/EvidenciaControllerTest.java`: anexar publica `evidencia_adicionada`; anexo recusado não publica
- [x] T008 Alterar `MAIN/adapter/in/web/evidencia/EvidenciaController.java` para publicar ao anexar

## Fase 4 — Integração e BDD

- [x] T003 Tabela de tipos de aviso em `docs/casos-de-uso.md`
- [x] T004 `make validate` verde
- [x] T009 Atualizar a seção de eventos em `docs/casos-de-uso.md` com `solicitacao_atividade` e a lista de responsáveis
- [x] T010 `make validate` verde
- [x] T011 Testes em `TEST/adapter/in/web/evidencia/EvidenciaControllerTest.java` para os caminhos sem cobertura do arquivo alterado (tipo informado, tipo inválido, conteúdo para a galeria, falha de leitura do arquivo), exigidos pelo Princípio 10
- [x] T012 `make validate` verde, com `EvidenciaController` em 95% ou mais de linha

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01 | T001, T002 |
| RF-02 | T001, T002 |
| RF-03 | T001, T002 |
| RF-04 | T001, T002 |
| RF-05 | T002, T004 |
| RF-06 | T003, T009 |
| RNF-01 | T002, T004 |
| RNF-02 | T002 |
| RNF-03 | T001 |
| RF-07 | T005, T006 |
| RF-08 | T005, T006 |
| RF-09 | T007, T008 |
| RF-10 | T005, T006, T007, T008 |
| RF-11 | T005, T007 |
| RNF-04 | T001, T004, T005, T007, T010, T011, T012 |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-10-05

- **Resultado:** convergiu em RF-01 a RF-06.
- **`make validate`:** verde, 612 testes no conjunto da branch.
- **Cobertura (linha):** `SolicitacaoController` 95,1%.
- **Desvios:** spec, plano e tarefas foram escritos depois do código. Os testes de T001
  foram vistos falhando antes de T002. Sem cenários em `app/tests/bdd`. Checklist sem
  revisão humana. Três ambiguidades aguardam confirmação na spec.

### Rodada 2 — 2026-10-05

- **Motivo:** o usuário respondeu as ambiguidades; entraram RF-07 a RF-11 (lista de
  responsáveis no aviso, avisos de comentário e de evidência).
- **Resultado:** convergiu em RF-01 a RF-11.
- **`make validate`:** verde, 623 testes no conjunto da branch.
- **Ordem:** spec, plano e tarefas atualizados antes dos testes; testes de T005 e T007
  vistos falhando antes de T006 e T008.
- **Desvios:** sem cenários em `app/tests/bdd`. Checklist sem revisão humana. A primeira
  execução de `make validate` desta rodada falhou na compilação porque o script de edição
  parou antes de gravar o código de produção; reaplicado e validado em seguida.

### Rodada 3 — 2026-10-05

- **Motivo:** `EvidenciaController`, alterado nesta feature, estava com 78,4% de linha,
  abaixo dos 95% do Princípio 10 (a lacuna vinha de antes: 77% na medição inicial).
- **Resultado:** T011 e T012 concluídas; `EvidenciaController` com 100% de linha.
- **`make validate`:** verde, 628 testes; cobertura de linha do conjunto 96,18%.
- **Desvios:** os cinco testes de T011 cobrem comportamento que já existia, então não houve
  etapa de teste falhando. A primeira tentativa de gravar esses testes falhou em silêncio
  (script de edição com erro em segundo plano) e foi refeita.
