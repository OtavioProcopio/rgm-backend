# Plano de implementação — Catálogo de modelos: filtros, busca e contagens

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.
> `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.
> Base: `develop` (`v1.7.0`). Publicação: backend primeiro, front (spec 019) depois.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Parâmetros novos, opt-in | `tipo`, `q`, `maquinaExata`, `ordem` na listagem existente | Rota nova de busca; mudar `maquina` para igualdade | RF-08 e Princípio 9: a administração e o front antigo continuam iguais |
| Busca única | `q` em código ou descrição com `rgm_sem_acento` (V11), no mesmo padrão de `SolicitacaoJpaRepository` e `UsuarioJpaRepository` | `LIKE` com `LOWER` (hoje) | Sem acento e sem caixa, como as outras buscas do sistema; sem migração |
| Mínimo de 2 caracteres | Validado em `ParametrosDeConsulta` (400), como o `q` das solicitações | Ignorar `q` curto | Falhar alto: o front não deve achar que filtrou |
| Máquina exata | `maquinaExata=true` troca "contém" por igualdade (sem caixa e sem acento) | Parâmetro `maquina` exato por padrão | Não quebra a tela de administração |
| Tipo | Igualdade sem caixa sobre o nome do tipo (`m.tipo`) | Id do tipo | O modelo guarda o nome do tipo, e o catálogo público (`/api/tipos-modelo`) devolve nomes |
| Ordem | `ordem=CODIGO` → `codigo` asc, `versao` desc, `id`; `CRIACAO` (padrão) → `criadoEm` desc, como hoje | Parâmetro livre de campo e sentido | Dois valores fechados são testáveis e não abrem injeção de campo |
| Consulta | Uma consulta nativa parametrizada única para listagem e contagem, com todos os filtros opcionais (`(:x IS NULL OR …)`) | Uma consulta por combinação de filtros | Mantém um só lugar com a regra dos filtros; a contagem reusa a cláusula |
| Contagens | `GET /api/modelos/contagens`: `total`, e `porTipo`/`porMaquina` calculados aplicando **todos os filtros menos o do próprio grupo** | Contar tudo com todos os filtros | RF-06: o botão mostra o que traria; é a contagem facetada padrão |
| Zeros | Só valores com contagem > 0 | Devolver o catálogo de tipos com zeros | O front já conhece os tipos pelo catálogo público e completa com 0 |
| Estrutura | `ListarModelosUseCase.Input` ganha `tipo`, `q`, `maquinaExata`, `ordem`; novo `ContarModelosUseCase`; porta `ModeloRepository` ganha `contarPor…` | Controller montando a consulta | Regra no caso de uso (P7) |

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| web | `MAIN/adapter/in/web/modelo/ModeloController.java` (parâmetros, rota `/contagens`) | alterar | `TEST/adapter/in/web/modelo/ModeloControllerTest.java` |
| web | `MAIN/adapter/in/web/ParametrosDeConsulta.java` (`q` mínimo, `ordem`) | alterar | `TEST/adapter/in/web/ParametrosDeConsultaTest.java` |
| web | `MAIN/adapter/in/web/dto/response/ContagensDeModelosResponse.java` | criar | `TEST/adapter/in/web/dto/response/ContagensDeModelosResponseTest.java` |
| uc | `MAIN/core/application/usecases/modelo/ListarModelosUseCase.java` | alterar | `TEST/core/application/usecases/modelo/ListarModelosUseCaseTest.java` |
| uc | `MAIN/core/application/usecases/modelo/ContarModelosUseCase.java` | criar | `TEST/core/application/usecases/modelo/ContarModelosUseCaseTest.java` |
| ports | `MAIN/core/domain/ports/repositories/ModeloRepository.java`, `FiltrosDeModelos` (registro dos filtros) e `OrdemDeModelos` (enum) | alterar/criar | — |
| per | `MAIN/adapter/out/persistence/repository/ModeloJpaRepository.java` (consulta nativa e contagens) | alterar | `TEST/adapter/out/persistence/repository/ModeloJpaRepositoryPostgresTest.java` |
| per | `MAIN/adapter/out/persistence/ModeloRepositoryAdapter.java` | alterar | `TEST/adapter/out/persistence/ModeloRepositoryAdapterTest.java` |
| config | `MAIN/adapter/config/UseCaseConfig.java` | alterar | `TEST/ApiApplicationTests.java` |
| docs | `docs/casos-de-uso.md`, `openspec/README.md` | alterar | — |

## Contrato entre camadas

- **Entrada:** `tipo`, `q` (≥ 2), `maquina`, `maquinaExata`, `ativo`, `codigo`, `descricao`, `ordem`, `page`, `size`.
- **Aplicação:** `ListarModelosUseCase` monta `FiltrosDeModelos` e `OrdemDeModelos`; `ContarModelosUseCase` pede os três agrupamentos.
- **Saída da contagem:** `{ total, porTipo: [{ tipo, total }], porMaquina: [{ maquina, total }] }`.
- **Compatibilidade:** aditivo; sem parâmetros novos, o caminho antigo (`findAll`/`findByFilters`) responde o mesmo.

## Dependências externas

Nenhuma.

## Impacto no contrato de operação

Nenhum alvo ou variável novos. Sem migração.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Busca com `rgm_sem_acento` sem índice fica lenta | baixa (centenas de linhas) | RNF-01 mede o número de consultas; teste com 1000 modelos no Postgres real |
| A consulta única muda a ordem ou a paginação antigas | média | RF-08 com teste de contrato comparando a resposta antiga e a nova sem parâmetros |
| Contagem facetada confunde: "por que Resina mostra 3 e a lista 0?" | média | A spec define que cada grupo ignora o próprio filtro e o front mostra o que o botão traria (spec 019) |
| `ordem=CODIGO` ordena código como texto ("A-10" antes de "A-2") | média | Declarado: ordem alfabética do código; ordem natural fica fora de escopo |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Testes provam a entrega | Todo arquivo novo tem teste espelhado; SQL em `*PostgresTest`; cenários rastreados em `tasks.md` |
| Simplicidade defensável | Sem migração, índice nem biblioteca; parâmetros opt-in |
| P7 — mapa de camadas | Regra de filtro no caso de uso e na porta; SQL só em `adapter.out` |
| P8 — linguagem ubíqua | `tipo`, `maquina`, `contagens`, `ordem` |
| P9 — compatibilidade | Aditivo, verificado por RF-08 |
| P10 — cobertura | Não baixa o mínimo; ≥ 95% por arquivo tocado |
| P11 — padrão de teste | Testes novos `shouldXWhenY`, AAA |
