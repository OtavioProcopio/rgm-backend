# Plano de implementação — Nome dos responsáveis na solicitação e capa da galeria sem erro

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.

Todos os caminhos são relativos a `app/`. O projeto mantém o layout Maven e os nomes de pacote
do legado (Princípio 7): produção em `src/main/java/com/rgm/api/...`, teste espelhado em
`src/test/java/com/rgm/api/...`. Abreviação usada abaixo: `P=src/main/java/com/rgm/api`,
`T=src/test/java/com/rgm/api`.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Onde fica a transação da troca de capa (RF-09, RF-10) | Em um método novo do **adaptador** de persistência, que limpa a capa antiga e grava a nova; o caso de uso chama um só método da porta. | `@Transactional` no caso de uso; no controller. | O Princípio 7 proíbe código novo em `core` importar `org.springframework`; os 10 casos de uso com `@Transactional` são dívida, não precedente. O controller não deve ter transação (issue #115). O adaptador já usa `@Transactional` em outros métodos. |
| Contrato da porta da galeria | Substituir `limparPrincipal(modeloId)` por `salvarComoPrincipal(foto)`, que devolve a foto gravada. `limparPrincipal` sai da porta e do adaptador; o repositório JPA mantém a consulta. | Manter `limparPrincipal` na porta e transacionar fora dela. | Expor "limpar" sozinho é o que permite o estado inconsistente; a única chamada hoje é a do caso de uso (YAGNI). |
| Edição conjunta de legenda e capa | Quando `principal` é verdadeiro, a foto já com a legenda nova vai em `salvarComoPrincipal`, numa só transação. Nos demais casos continua `save`. | Duas gravações. | RF-11: legenda e `principal: false` não mudam; legenda + capa passam a ser indivisíveis sem custo. |
| Nomes no detalhe (RF-01, RF-02, RF-12) | `ObterSolicitacaoUseCase` resolve os nomes com **uma** chamada a `UsuarioRepository.findAllByIdIn` (responsáveis + quem abriu) e devolve no `Output`. | Chamar o repositório do controller; consulta por responsável. | Mantém a regra de resolução no caso de uso, testável sem Spring. `findAllByIdIn` não filtra por situação: inativo continua com nome (RF-06). |
| Nomes na listagem (RF-03, RNF-01) | Novo método `resolverNomes(ids)` em `ObterSolicitacaoUseCase`, chamado **uma vez** pelo controller com os ids da página (responsáveis do lote já calculado + quem abriu). | Estender `listarResponsaveisBatch` para devolver nomes; consulta por linha. | `listarResponsaveisBatch` segue com a mesma assinatura (usada e testada hoje). Total de consultas = o de hoje + 1 (RNF-01). |
| Forma dos campos novos | `responsaveis`: lista de `{id, nome}`; `abertaPorNome`: texto. Ambos no fim do registro `SolicitacaoResponse`, depois de `acoesPermitidas`. | Trocar o tipo de `responsavelIds`. | RF-04, RNF-02 e Princípio 9: nenhum campo existente muda. |
| Resposta de ação e evento (RF-08) | As fábricas `from(s)` e `from(s, ids)` passam `null` nos dois campos novos; só o detalhe e a listagem preenchem. | Preencher em todo lugar. | Segue o padrão de `acoesPermitidas`, que já sai nulo nesses caminhos. Nada muda em `publicar(...)`. |
| Responsável sem usuário (RF-12) | Item `{id, nome: null}`; `abertaPorNome` nulo se quem abriu não for achado. | Omitir o item. | Preserva ordem e tamanho de `responsavelIds`, decisão do usuário. |
| Teste da transação (RNF-03) | Teste com Postgres real (Testcontainers) sobre o adaptador, **sem** a transação que o `@DataJpaTest` abre por padrão (`NOT_SUPPORTED`). | Só teste de unidade com mock; `@DataJpaTest` transacional. | O mock e a transação implícita do teste são o motivo de a falha não ter sido pega. Segue o precedente `SolicitacaoJpaRepositoryPostgresTest`. |
| Sem migração | Nenhuma migração Flyway. | — | Nada de esquema muda (Princípio 9). |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Unit of Work (via transação do adaptador) | `FotoGaleriaModeloRepositoryAdapter.salvarComoPrincipal` | Duas escritas que precisam valer juntas. | Método da porta com semântica de negócio ("salvar como capa"). |

> **Considerados e recusados:** *Decorator* de transação sobre o caso de uso (indireção para
> uma única chamada, e o `core` continuaria dependendo de contrato do Spring); *Specification*/DTO
> de projeção para os nomes (a consulta de usuários em lote já existe); *Strategy* para o
> preenchimento condicional dos campos (um `null` nas fábricas basta).

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado | Requisitos |
|---|---|---|---|---|
| interfaces (porta) | `P/core/domain/ports/repositories/FotoGaleriaModeloRepository.java` | alterar: troca `limparPrincipal` por `salvarComoPrincipal` | coberto pelos testes do adaptador e do caso de uso | RF-09, RF-10 |
| core/application | `P/core/application/usecases/modelo/EditarFotoGaleriaUseCase.java` | alterar | `T/core/application/usecases/modelo/EditarFotoGaleriaUseCaseTest.java` (alterar) | RF-09, RF-11 |
| adapters/repositories | `P/adapter/out/persistence/FotoGaleriaModeloRepositoryAdapter.java` | alterar: `@Transactional salvarComoPrincipal`; remove `limparPrincipal` | `T/adapter/out/persistence/FotoGaleriaModeloRepositoryAdapterTest.java` (alterar) e `T/adapter/out/persistence/FotoGaleriaModeloRepositoryAdapterPostgresTest.java` (criar) | RF-09, RF-10, RNF-03 |
| core/application | `P/core/application/usecases/solicitacao/ObterSolicitacaoUseCase.java` | alterar: `Output` ganha `responsaveis` e `abertaPorNome`; novo `resolverNomes`; novo registro `ResponsavelNome(id, nome)` | `T/core/application/usecases/solicitacao/ObterSolicitacaoUseCaseTest.java` (alterar) | RF-01, RF-02, RF-05, RF-06, RF-12 |
| adapters/controllers (DTO) | `P/adapter/in/web/dto/response/SolicitacaoResponse.java` | alterar: dois campos novos no fim e fábrica com nomes | `T/adapter/in/web/dto/response/SolicitacaoResponseTest.java` (criar) | RF-01..RF-04, RF-08 |
| adapters/controllers (DTO) | `P/adapter/in/web/dto/response/ResponsavelResponse.java` | criar (`id`, `nome`) | coberto por `SolicitacaoResponseTest` | RF-01 |
| adapters/controllers | `P/adapter/in/web/solicitacao/SolicitacaoController.java` | alterar: `buscarPorId` e `listar` passam os nomes | `T/adapter/in/web/solicitacao/SolicitacaoControllerTest.java` (alterar) | RF-03, RF-07, RF-08, RNF-01 |
| documentação | `openspec/README.md` | alterar: linhas de `solicitacoes-kanban` e `galeria-modelo` apontando para `specs/009-...` (Princípio 12) | — | — |

`UseCaseConfig` não muda: as assinaturas dos construtores dos casos de uso ficam iguais.
`GaleriaModeloController` não muda. O repositório JPA (`FotoGaleriaModeloJpaRepository`)
não muda.

## Contrato entre camadas

- **Detalhe:** `SolicitacaoController.buscarPorId` → `ObterSolicitacaoUseCase.execute` →
  `Output(solicitacao, responsavelIds, responsaveis, abertaPorNome, acoesPermitidas)` →
  `SolicitacaoResponse.from(...)`. O acesso continua decidido por `AcessoSolicitacao.validarLeitura`
  antes de qualquer nome sair (RF-07).
- **Listagem:** o controller obtém `responsaveisPorSol` (consulta em lote que já existe), junta
  os ids de responsáveis e de quem abriu da página, chama `resolverNomes` uma vez e monta cada
  item. Ausente no mapa → nome nulo (RF-12).
- **Capa:** `GaleriaModeloController` → `EditarFotoGaleriaUseCase.execute` →
  `salvarComoPrincipal` (transação do adaptador: `UPDATE` que limpa + gravação da nova). Erro
  em qualquer um desfaz os dois e sobe como hoje; o tratamento de erro HTTP não muda.

## Dependências externas

| Dependência | Versão | Justificativa | Simulada nos testes por |
|---|---|---|---|
| Nenhuma nova | — | Testcontainers/PostgreSQL já estão no `pom.xml` | Mockito nos testes de unidade; Postgres real no teste do adaptador |

## Impacto no contrato de operação

Nenhum alvo novo de Makefile, serviço de compose ou variável de ambiente. O teste com Postgres
roda em `make test`, como os `*PostgresTest` existentes (exige Docker). Critério de pronto:
`make validate`.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Existe `SolicitacaoResponse` construído direto em teste com construtor posicional, quebrando ao acrescentar campos | média | Os dois campos entram no fim; ajustar os testes que constroem o registro, e preferir as fábricas |
| Entidade de capa antiga já carregada na sessão do pedido (sessão aberta por requisição) ficar desatualizada após o `UPDATE` em massa | baixa | O teste Postgres relê as fotos do banco depois da operação e confere 1 capa |
| Teste com Docker indisponível no ambiente do desenvolvedor | baixa | Mesmo requisito dos testes `*PostgresTest` atuais |
| Página de 100 itens gera lista grande no `IN` da consulta de nomes | baixa | Limite de 3 ids por item (≤ 300); uma consulta só |
| Cobertura de arquivo tocado abaixo de 95% (P10) | baixa | Cada ramo novo (nome nulo, lista vazia, inativo, falha) tem teste |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Contrato de operação | Nenhum alvo novo; validação por `make validate`; ferramentas de linguagem só via alvos. |
| Arquitetura limpa | A transação fica no adaptador; `core` não ganha import de Spring. A porta continua em `core`, o adaptador implementa. Dependência aponta para dentro. |
| Testes provam a entrega | Cada RF tem teste (ver arquivos); o teste da capa roda com transação real e falha sem a correção; AAA e `shouldXWhenY` nos testes novos ou alterados (P11). |
| Simplicidade defensável | Um método de porta no lugar de dois; nenhum padrão novo; campos novos opcionais e ao fim. |
| P7 Mapa de camadas | Nenhum pacote novo; nomes do legado mantidos. |
| P8 Linguagem ubíqua | `responsaveis`, `abertaPorNome`, `salvarComoPrincipal`; sem sinônimo em inglês. |
| P9 Compatibilidade | `responsavelIds` e `abertaPorUsuarioId` intactos; nenhuma migração; nenhum campo removido; mudança só aditiva, sem ordem de publicação a respeitar. |
| P10 Cobertura | Mínimo do JaCoCo intacto; nenhuma exclusão nova. |
| P11 Padrão de teste | Só os testes tocados seguem o padrão novo. |
| P12 Uma fonte de especificação | Spec em `specs/009-...`; `openspec/` só recebe as linhas da tabela do README. |
