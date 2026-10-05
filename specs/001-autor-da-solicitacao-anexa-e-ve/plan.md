# Plano de implementação — Autor da solicitação anexa e vê evidências

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.

Caminhos abaixo são relativos a `app/src/main/java/com/rgm/api/` (produção) e
`app/src/test/java/com/rgm/api/` (teste), conforme o mapa de camadas do Princípio 7.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Onde mora a regra de acesso a evidências | Uma classe de domínio nova, `AcessoEvidenciaSolicitacao`, com duas operações: validar listagem e validar anexo | (a) Corrigir o `validarAcesso` privado nos dois casos de uso; (b) método novo em `Solicitacao` | (a) mantém a duplicação que causou o bug e deixa regra de negócio no caso de uso, contra o Princípio 2. (b) `Solicitacao.java` já tem 617 linhas, acima do limite de 500 |
| Forma da classe de regra | Métodos estáticos puros, sem estado e sem dependência injetada | Componente injetado com interface em `ports` | Não faz I/O nem tem variação de implementação; segue o precedente de `Solicitacao.validarAutorizacaoMover` e `DomainValidations`. Interface aqui seria abstração sem segundo implementador |
| Como a regra sabe se o usuário é responsável | Recebe um `BooleanSupplier`, avaliado só quando o perfil e a autoria não decidem | Passar `boolean` já consultado | Preserva o comportamento atual de não consultar atribuição para GESTOR e ADMINISTRADOR, e cumpre o RNF-03 (nenhuma consulta adicional) |
| Ordem das verificações no anexo | Tamanho e tipo de arquivo → solicitação existe → solicitação não encerrada → acesso | Verificar acesso antes do status | Mantém a ordem e os códigos de resposta da v1.5.0 (RNF-01): solicitação encerrada continua respondendo 422, não 403 |
| Erro para usuário inativo e para tipo não permitido ao autor | `NaoAutorizadoException`, que já é traduzida para 403 | Exceção nova | O tipo já existe e já tem tradução; a spec pede 403 nos dois casos |
| Listagem em solicitação encerrada | A validação de listagem não olha o status | Reusar a validação de anexo | RF-02: o autor lista mesmo depois do encerramento |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Nenhum padrão GoF | — | — | — |

Considerados e recusados:

- **Specification** (compor regras `ehAutor.or(ehResponsavel)`): recusado. São quatro
  condições fixas; a composição não seria reutilizada em outro ponto hoje.
- **Strategy por perfil**: recusado. `PerfilUsuario` já responde `podeGerenciarModelos()` e
  `podeGerenciarUsuariosEMaquinas()`; uma hierarquia de estratégias duplicaria o enum.
- **Chain of Responsibility** para a sequência de verificações: recusado. A sequência é
  linear, curta e não muda em tempo de execução.

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| core/domain | `core/domain/validation/AcessoEvidenciaSolicitacao.java` | criar | `core/domain/validation/AcessoEvidenciaSolicitacaoTest.java` (criar) |
| core/application | `core/application/usecases/evidencia/AnexarEvidenciaUseCase.java` | alterar: remover `validarAcesso` privado e delegar à classe de domínio, passando o tipo da evidência | `core/application/usecases/evidencia/AnexarEvidenciaUseCaseTest.java` (alterar) |
| core/application | `core/application/usecases/evidencia/VisualizarEvidenciaUseCase.java` | alterar: remover `validarAcesso` privado e delegar à classe de domínio, usando a solicitação já carregada | `core/application/usecases/evidencia/VisualizarEvidenciaUseCaseTest.java` (alterar) |
| documentação | `docs/casos-de-uso.md` | alterar: regra de acesso de UC-08 (anexar) e do caso de uso de visualização | — |

Sem mudança em controller, DTO, repositório, migração ou `UseCaseConfig`: os construtores
dos dois casos de uso não mudam.

Rastreabilidade dos requisitos:

| Requisito | Onde é atendido |
|---|---|
| RF-01, RF-07 | `AcessoEvidenciaSolicitacao` (validar anexo) + `AnexarEvidenciaUseCase` |
| RF-02 | `AcessoEvidenciaSolicitacao` (validar listagem) + `VisualizarEvidenciaUseCase` |
| RF-03, RF-05, RF-06, RF-08 | `AcessoEvidenciaSolicitacao`, usada pelos dois casos de uso |
| RF-04 | `AnexarEvidenciaUseCase` (verificação de status existente, mantida antes do acesso) |
| RNF-01 | nenhum arquivo de contrato alterado |
| RNF-02 | testes dos três arquivos de produção |
| RNF-03 | `BooleanSupplier` para a consulta de atribuição |

## Contrato entre camadas

- `EvidenciaController` (inalterado) chama os casos de uso com o id do usuário autenticado.
- Cada caso de uso carrega a `Solicitacao` e o `Usuario` pelos repositórios que já recebe e
  chama `AcessoEvidenciaSolicitacao`, entregando: usuário, solicitação, um fornecedor que
  consulta `SolicitacaoAtribuicaoRepository` e, no anexo, o `TipoEvidencia`.
- A classe de domínio decide nesta ordem: usuário inativo → recusa; GESTOR ou ADMINISTRADOR
  → libera; responsável atribuído → libera; autor → libera a listagem e, no anexo, só
  ABERTURA e GERAL; demais → recusa.
- A recusa é `NaoAutorizadoException`; solicitação ou usuário inexistente continua
  `RecursoNaoEncontradoException`; solicitação encerrada continua `BusinessRuleException`.
  `GlobalExceptionHandler` (inalterado) traduz para 403, 404 e 422.
- `core` continua sem importar framework: a classe nova usa só `java.util`.

## Dependências externas

| Dependência | Versão | Justificativa | Simulada nos testes por |
|---|---|---|---|
| Nenhuma nova | — | — | — |

## Impacto no contrato de operação

Nenhum alvo, serviço ou variável de ambiente novo.

Desvio registrado: o projeto ainda não tem os alvos `it` e `bdd` nem Cucumber (ver
`.specify/memory/as-is.md`, seções 4 e 7). Os cenários de aceite da spec serão cobertos por
testes unitários da classe de domínio e dos dois casos de uso, um teste por cenário, com o
nome do cenário no nome do método. Introduzir Cucumber e o alvo `bdd` é trabalho da Onda 1
do plano de adoção e não entra nesta correção.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Testes existentes dependem de o `Usuario` de teste estar ativo ou da ordem das chamadas aos mocks | média | Rodar a suíte dos três arquivos antes de alterar e ajustar só a montagem dos dados, sem afrouxar asserção |
| Recusa de usuário inativo (RF-08) atinge GESTOR e ADMINISTRADOR inativos com token ainda válido | baixa | É o comportamento pedido; fica documentado em `docs/casos-de-uso.md` |
| O frontend continua escondendo o 403 dos tipos que o autor não pode anexar | média | Fora de escopo (rgm-frontend#112); citar a dependência no PR |
| Cobertura do bundle cair abaixo de 95% | baixa | A mudança remove código duplicado e acrescenta testes; `make coverage` antes do PR |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Contrato de operação | Nenhum alvo novo; validação por `make lint`, `make test` e `make coverage`. A ausência de `it`/`bdd` está declarada acima como desvio herdado |
| Arquitetura limpa | A regra sai do caso de uso e vai para `core/domain`; o caso de uso só orquestra; `core` não ganha import de framework |
| Testes provam a entrega | Teste espelhado para cada arquivo de produção; testes novos em AAA, `shouldXWhenY`, um comportamento por teste e `verifyNoMoreInteractions` |
| Simplicidade defensável | Uma classe pequena de funções puras; três padrões avaliados e recusados com motivo |
| Autoria | Commits e PR só com o autor do `git config`, sem crédito a ferramenta |
| Idioma | Artefatos em português; termos de domínio em português, termos técnicos em inglês |
| Mapa de camadas do legado (7) | Usa os pacotes existentes (`core.domain.validation`, `core.application.usecases`); nenhum pacote novo no padrão da organização |
| Linguagem ubíqua (8) | Classe e métodos nomeados com os termos do domínio já em uso |
| Compatibilidade com produção (9) | Nenhum campo, endpoint, código de resposta ou migração alterado; só acessos antes negados passam a ser aceitos e usuário inativo passa a ser recusado |
| Cobertura não regride (10) | Mínimo do JaCoCo intocado, nenhuma exclusão nova, 95% por arquivo modificado |
| Padrão de teste no que for tocado (11) | Só os testes novos e os alterados seguem o padrão; os demais ficam como estão |
| Uma fonte de especificação (12) | Spec em `specs/001-...`; `openspec/` não é alterado |
