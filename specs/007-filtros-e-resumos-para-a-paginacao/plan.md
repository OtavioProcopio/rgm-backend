# Plano de implementação — Filtros e resumos para a paginação

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.

Caminhos relativos a `app/src/main/java/com/rgm/api/` (produção) e
`app/src/test/java/com/rgm/api/` (teste), conforme o Princípio 7.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Filtro "em aberto" | Parâmetro opcional `emAberto` na listagem e no relatório, levado até a consulta de filtros que já existe | Aceitar lista de status no parâmetro `status` | Não muda o formato de um parâmetro em uso (RNF-02); a regra "em aberto" fica num só lugar |
| "Em aberto" ligado sem outro filtro | Conta como filtro e usa a consulta de filtros | Tratar no caminho sem filtros | O caminho sem filtros não tem onde aplicar a condição |
| Data de encerramento | A consulta de filtros compara a data de conclusão e, na falta dela, a de cancelamento | Parâmetro novo só para cancelamento | A spec pede um filtro só (RF-04); uma solicitação tem uma das duas datas, nunca as duas |
| Resumo de modelos | Duas consultas agregadas (contagens; quantidade por máquina já ordenada) | Uma consulta com agrupamento e soma na aplicação | Duas consultas simples, dentro do limite de 3 (RNF-01), sem lógica de soma fora do banco |
| Resumo das solicitações de um modelo | Uma consulta agregada devolve contagens, primeira e última data de abertura e média de resolução das concluídas | Reaproveitar a consulta do ranking, que usa `LAG()` | A média dos intervalos entre aberturas consecutivas é igual a (última − primeira) ÷ (total − 1); dispensa função de janela e roda no banco de teste |
| Onde fica a conta do intervalo | No próprio registro do resumo, no domínio | No adapter de persistência | É regra de negócio (RF-11) e fica testável sem banco |
| Ficha em PDF | O controller pede o resumo ao caso de uso e repassa os dois tempos ao gerador de PDF | Manter a conta em memória no controller | RF-12; remove regra duplicada do controller |
| Verificação da consulta de filtros | Teste novo contra PostgreSQL real, no mesmo grupo do teste de migração | Só testes com dependência simulada | A consulta é SQL nativo de PostgreSQL e não roda no banco em memória; hoje não tem verificação nenhuma |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Nenhum novo | — | — | — |

Considerado e recusado: **Specification** para compor os filtros da listagem, porque a
consulta de filtros já existe e ganha só uma condição.

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| interfaces | `core/domain/ports/repositories/ResumoModelos.java` | criar: contagens e quantidade por máquina | — (registro sem lógica) |
| interfaces | `core/domain/ports/repositories/QuantidadePorMaquina.java` | criar | — (registro sem lógica) |
| interfaces | `core/domain/ports/repositories/ResumoSolicitacoesModelo.java` | criar: contagens, tempos e a conta do intervalo | `core/domain/ports/repositories/ResumoSolicitacoesModeloTest.java` (criar) |
| interfaces | `core/domain/ports/repositories/ModeloRepository.java` | alterar: `resumir()` | — |
| interfaces | `core/domain/ports/repositories/SolicitacaoRepository.java` | alterar: `emAberto` em `findByFilters`; `resumirPorModelo` | — |
| core/application | `core/application/usecases/modelo/ObterResumoModelosUseCase.java` | criar | `.../modelo/ObterResumoModelosUseCaseTest.java` (criar) |
| core/application | `core/application/usecases/modelo/ObterResumoSolicitacoesModeloUseCase.java` | criar: modelo inexistente responde "não encontrado" | `.../modelo/ObterResumoSolicitacoesModeloUseCaseTest.java` (criar) |
| core/application | `core/application/usecases/solicitacao/ListarSolicitacoesUseCase.java` | alterar: `emAberto` na entrada | `.../solicitacao/ListarSolicitacoesUseCaseTest.java` (alterar) |
| adapters/repositories | `adapter/out/persistence/repository/ModeloJpaRepository.java` | alterar: duas consultas agregadas | `.../repository/ModeloJpaRepositoryTest.java` (alterar) |
| adapters/repositories | `adapter/out/persistence/repository/SolicitacaoJpaRepository.java` | alterar: `emAberto` e data de encerramento na consulta de filtros; consulta de resumo por modelo | `.../repository/SolicitacaoJpaRepositoryTest.java` (criar), `.../repository/SolicitacaoJpaRepositoryPostgresTest.java` (criar) |
| adapters/repositories | `adapter/out/persistence/ModeloRepositoryAdapter.java` | alterar | `adapter/out/persistence/ModeloRepositoryAdapterTest.java` (alterar) |
| adapters/repositories | `adapter/out/persistence/SolicitacaoRepositoryAdapter.java` | alterar | `adapter/out/persistence/SolicitacaoRepositoryAdapterTest.java` (alterar) |
| adapters/controllers | `adapter/in/web/dto/response/ResumoModelosResponse.java` | criar | coberto pelo teste do controller |
| adapters/controllers | `adapter/in/web/dto/response/ResumoSolicitacoesModeloResponse.java` | criar | coberto pelo teste do controller |
| adapters/controllers | `adapter/in/web/modelo/ModeloController.java` | alterar: dois endpoints; ficha em PDF usa o resumo | `adapter/in/web/modelo/ModeloControllerTest.java` (alterar) |
| adapters/controllers | `adapter/in/web/solicitacao/SolicitacaoController.java` | alterar: `emAberto` na listagem e no relatório | `adapter/in/web/solicitacao/SolicitacaoControllerTest.java` (alterar) |
| infra/init | `adapter/config/UseCaseConfig.java` | alterar: registra os dois casos de uso | — |
| operação | `Makefile` | alterar: os alvos sem Testcontainers passam a excluir também `*PostgresTest` | — |
| documentação | `docs/casos-de-uso.md`, `openspec/README.md` | alterar | — |

Rastreabilidade: RF-01 a RF-04 e RF-09 em `ListarSolicitacoesUseCase`,
`SolicitacaoJpaRepository` e `SolicitacaoController`; RF-05 em `ObterResumoModelosUseCase`
e `ModeloJpaRepository`; RF-06, RF-07, RF-10 e RF-11 em
`ObterResumoSolicitacoesModeloUseCase`, `ResumoSolicitacoesModelo` e
`SolicitacaoJpaRepository`; RF-08 e RF-12 em `ModeloController`.

## Contrato entre camadas

- `GET /api/solicitacoes?emAberto=true` e `GET /api/solicitacoes/relatorio?emAberto=true`:
  parâmetro opcional; ausente ou `false` não filtra.
- `GET /api/modelos/resumo` responde
  `{ total, ativos, inativos, comPendenciaAberta, porMaquina: [{ maquina, quantidade }] }`.
- `GET /api/modelos/{id}/solicitacoes/resumo` responde
  `{ total, emAberto, concluidas, canceladas, tempoMedioResolucaoSegundos, intervaloMedioSegundos }`;
  os dois tempos são nulos quando não há dado; modelo inexistente responde 404.
- Os dois endpoints ficam sob a regra geral de segurança (qualquer usuário autenticado),
  a mesma de `GET /api/modelos` (RF-08).

## Dependências externas

| Dependência | Versão | Justificativa | Simulada nos testes por |
|---|---|---|---|
| Nenhuma nova | — | Testcontainers e H2 já estão no projeto | — |

## Impacto no contrato de operação

`make test`, `make test-fast`, `make coverage` e `make validate` passam a excluir também
`*PostgresTest`, além de `FlywayMigrationTest`; `make test-all` e a CI rodam os dois.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Consulta agregada se comportar diferente no banco de teste e no PostgreSQL | média | O resumo por modelo é exercitado nos dois bancos |
| Filtro por data de encerramento passar a trazer canceladas onde antes só vinham concluídas | certa | É o RF-04; quem quer só concluídas combina com o status, como o quadro faz |
| Números da ficha mudarem em relação à v1.5.0 | certa | Decisão do usuário, registrada na spec |
| Frontend v1.5.0 contra este backend | baixa | Os parâmetros são opcionais e os endpoints são novos; nada que ele usa muda de formato |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Contrato de operação | Validação por `make validate`; nenhum alvo novo, só a lista de exclusão dos alvos sem Docker. Vale o desvio da feature 001: sem alvos `it` e `bdd` |
| Arquitetura limpa | Casos de uso falam com portas; a conta do intervalo fica no domínio; SQL só nos repositórios |
| Testes provam a entrega | Teste antes da implementação; testes novos em AAA, um comportamento por teste |
| Simplicidade defensável | Uma condição a mais na consulta existente; nenhuma abstração nova para filtros |
| Autoria | Commits e PR só com o autor do `git config` |
| Idioma | Artefatos em português |
| Mapa de camadas (7) | Pacotes existentes |
| Linguagem ubíqua (8) | "Em aberto" é `StatusSolicitacao.isNaoTerminal()` |
| Compatibilidade com produção (9) | Nenhum campo removido ou renomeado; parâmetros novos opcionais (RNF-02) |
| Cobertura não regride (10) | Mínimo do JaCoCo intocado e nenhuma exclusão nova |
| Padrão de teste no que for tocado (11) | Testes novos no padrão; os antigos mantêm o formato e só recebem o parâmetro novo |
| Uma fonte de especificação (12) | Spec nesta pasta; linha nova na tabela de `openspec/README.md` |
