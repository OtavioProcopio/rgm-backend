# Tarefas — Descrição do que fazer, obrigatória na triagem

> Ordem de dependência; teste antes da implementação.
> `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

## Fase 1 — Triagem com descrição (RF-01 a RF-04)

- [x] T001 Teste em `TriarSolicitacaoUseCaseTest`: descrição em branco recusa e não grava (RF-01)
- [x] T002 Teste em `TriarSolicitacaoUseCaseTest`: triagem feliz grava 3 atividades (atribuição, status, descrição) (RF-03)
- [x] T003 Teste: descrição com espaços nas pontas é gravada sem eles (RF-03)
- [x] T004 Teste de DTO/controller: ausente, em branco e 2001 caracteres respondem 400; 2000 passa (RF-01, RF-02)
- [x] T005 Implementar `TriarSolicitacaoRequest`, `SolicitacaoController` e `TriarSolicitacaoUseCase`
- [x] T006 Teste: regras antigas (operador, responsável inativo, perfil não atribuível) continuam recusando (RF-04)

## Fase 2 — Documentação e validação

- [x] T007 Atualizar `docs/casos-de-uso.md` (UC-03) e a tabela de `openspec/README.md`
- [ ] T008 `make format`, `make lint` e `make validate` verdes, com a saída real no `Convergence`

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01 | T001, T004, T005 |
| RF-02 | T004, T005 |
| RF-03 | T002, T003, T005 |
| RF-04 | T006 |
| RNF-01, RNF-03 | T008 |
| RNF-02 | T008 (nenhum arquivo em `db/migration`) |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-11-01

Evidência de execução (parcial — ver limites):

- `./mvnw -o test -Dtest='!*PostgresTest,!FlywayMigrationTest'`: 1114 testes, 0 falhas.
- **Não executado:** `make validate` (JaCoCo e spotless indisponíveis offline neste ambiente) e os
  `*PostgresTest` (exigem Docker). Formatação conferida à mão (linhas ≤ 100, um argumento por
  linha); o CI é a verificação real de lint e cobertura.

| Requisito | Estado | Evidência |
|---|---|---|
| RF-01 | realizado | `TriarSolicitacaoRequest` (`@NotBlank`); `TriarSolicitacaoUseCase`; testes `shouldFailAndWriteNothingWhenDescricaoIsBlank`, `shouldFailWhenDescricaoIsNull`, `shouldReturn400WhenTriarHasNoDescricao` |
| RF-02 | realizado | `@Size(max = COMENTARIO)`; `shouldReturn400WhenTriarDescricaoIsBlankOrTooLong`, `shouldAcceptTriarWhenDescricaoHasExactly2000Characters` |
| RF-03 | realizado | `shouldRecordTrimmedDescricaoAsComentarioOfGestorWhenTriaging`; a transação é a do caso de uso (`@Transactional`); a falha parcial não tem teste de integração com Postgres |
| RF-04 | realizado | `TriarSolicitacaoUseCaseTest` das regras antigas, ajustado só para passar a descrição |
| RNF-01 | não medido | JaCoCo não rodou aqui |
| RNF-02 | realizado | nenhum arquivo em `db/migration` |
| RNF-03 | não medido | ver acima |

Desvio de processo: código antes da spec (declarado no topo da spec).
