# Plano de implementação — Ações permitidas na solicitação

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.

Caminhos relativos a `app/src/main/java/com/rgm/api/` (produção) e
`app/src/test/java/com/rgm/api/` (teste), conforme o Princípio 7.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Onde mora o cálculo | Classe de domínio `AcoesPermitidasSolicitacao`, de funções puras | Calcular no controller ou no caso de uso | É regra de negócio (Princípio 2); o caso de uso só reúne os dados e o controller só traduz |
| Fonte das regras | `PerfilUsuario.pode*()`, o status da solicitação, autoria e atribuição | Chamar a validação de cada caso de uso e capturar a exceção | Exceção como fluxo de controle; exigiria instanciar oito casos de uso |
| Acesso a evidências | Reusar `AcessoEvidenciaSolicitacao`, com uma consulta booleana nova (`podeListar`) | Repetir a regra | A duplicação dessa regra foi a causa da #86 |
| Vocabulário das ações | Enum `AcaoSolicitacao` no domínio, com os nomes da issue | Textos soltos | Nome errado quebra a compilação, não o frontend |
| Onde calcular | Só na consulta pelo identificador | Também na listagem | RNF-03; ver Ambiguidade 1 da spec |
| Dados para o cálculo | Usuário carregado pelo caso de uso (1 consulta nova); atribuição e existência de responsável tiradas da lista de responsáveis que ele já carrega | Consultas próprias de atribuição | RNF-02: no máximo 1 consulta adicional |
| Representação quando não calculado | Campo nulo | Lista vazia; omitir o campo | Lista vazia significaria "nenhuma ação"; omitir exigiria configuração de serialização por campo |
| Assinatura do caso de uso | `execute` passa a receber solicitação e usuário; a saída ganha as ações | Método novo ao lado do antigo | O antigo ficaria sem uso em produção |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Nenhum | — | — | — |

Considerados e recusados: **Specification** (uma regra componível por ação), porque nove
condições curtas e fixas cabem em um método legível; **Strategy por perfil**, porque o enum
de perfil já responde às perguntas de permissão; **State** para o status, porque a máquina
de estados já está no enum `StatusSolicitacao`.

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| core/domain | `core/domain/model/enums/AcaoSolicitacao.java` | criar | coberto por `core/domain/validation/AcoesPermitidasSolicitacaoTest.java` |
| core/domain | `core/domain/validation/AcoesPermitidasSolicitacao.java` | criar | `core/domain/validation/AcoesPermitidasSolicitacaoTest.java` (criar) |
| core/domain | `core/domain/validation/AcessoEvidenciaSolicitacao.java` | alterar: consulta `podeListar` | `core/domain/validation/AcessoEvidenciaSolicitacaoTest.java` (existente) |
| core/application | `core/application/usecases/solicitacao/ObterSolicitacaoUseCase.java` | alterar: recebe o usuário e devolve as ações | `core/application/usecases/solicitacao/ObterSolicitacaoUseCaseTest.java` (alterar) |
| infra/init | `adapter/config/UseCaseConfig.java` | alterar: injeta o repositório de usuários | fora da conta de cobertura (IoC) |
| adapters/controllers | `adapter/in/web/dto/response/SolicitacaoResponse.java` | alterar: campo `acoesPermitidas` | coberto por `adapter/in/web/solicitacao/SolicitacaoControllerTest.java` |
| adapters/controllers | `adapter/in/web/solicitacao/SolicitacaoController.java` | alterar: consulta pelo identificador repassa o usuário e as ações | `adapter/in/web/solicitacao/SolicitacaoControllerTest.java` (alterar) |
| documentação | `docs/casos-de-uso.md` | alterar: tabela de ações | — |

Rastreabilidade: RF-02 a RF-10 em `AcoesPermitidasSolicitacao`; RF-09 também em
`AcessoEvidenciaSolicitacao`; RF-01 e RNF-02 em `ObterSolicitacaoUseCase` e no controller;
RF-11, RNF-01 e RNF-03 em `SolicitacaoResponse`.

## Contrato entre camadas

O controller extrai o identificador do usuário autenticado e chama o caso de uso. O caso de
uso carrega a solicitação, o usuário e os responsáveis ativos, e entrega ao domínio: o
usuário, a solicitação, se o usuário é responsável e se existe algum responsável. O domínio
devolve um conjunto imutável de ações, na ordem do enum. O controller converte o conjunto em
lista de textos. Solicitação ou usuário inexistente é `RecursoNaoEncontradoException` (404).
`core` continua sem importar framework.

## Dependências externas

| Dependência | Versão | Justificativa | Simulada nos testes por |
|---|---|---|---|
| Nenhuma nova | — | — | — |

## Impacto no contrato de operação

Nenhum.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| O cálculo divergir das validações dos casos de uso, porque as regras continuam escritas nos dois lugares | média | Tabela de ações em `docs/casos-de-uso.md`; o passo seguinte, fora desta feature, é os casos de uso passarem a consultar a mesma classe |
| Frontend ler nulo como "nenhuma ação" | média | Documentado; ver Ambiguidade 5 da spec |
| Ação exibida e recusada por pré-condição de dados | média | Declarado em "Fora de escopo"; ver Ambiguidade 3 |
| Usuário autenticado inexistente passa a receber 404 na consulta | baixa | Só ocorre com token de usuário removido |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Contrato de operação | Validação por `make validate`; nenhum alvo novo. Vale o desvio da feature 001: sem alvos `it` e `bdd` |
| Arquitetura limpa | O cálculo fica em `core/domain`; o caso de uso orquestra e o controller traduz; `core` sem import de framework |
| Testes provam a entrega | Testes novos em AAA, `shouldXWhenY`, um comportamento por teste e `verifyNoMoreInteractions` |
| Simplicidade defensável | Uma classe de funções puras e um enum; três padrões avaliados e recusados |
| Autoria | Commits e PR só com o autor do `git config` |
| Idioma | Artefatos em português; termos de domínio em português |
| Mapa de camadas (7) | Pacotes existentes; nenhum pacote novo no padrão da organização |
| Linguagem ubíqua (8) | Ações com os nomes de domínio da issue (`TRIAR`, `ENCERRAR`, `ALTERAR_RESPONSAVEIS`) |
| Compatibilidade com produção (9) | Só acrescenta um campo à resposta; nenhum campo, endpoint ou migração removido ou renomeado |
| Cobertura não regride (10) | Mínimo do JaCoCo intocado e nenhuma exclusão nova; arquivos alterados com 95% ou mais de linha |
| Padrão de teste no que for tocado (11) | Só testes novos e alterados seguem o padrão |
| Uma fonte de especificação (12) | Spec nesta pasta; `openspec/` não é alterado |
