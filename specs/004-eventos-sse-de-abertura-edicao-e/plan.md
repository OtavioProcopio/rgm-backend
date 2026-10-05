# Plano de implementação — Eventos SSE de abertura, edição e responsáveis

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.

Caminhos relativos a `app/src/main/java/com/rgm/api/` (produção) e
`app/src/test/java/com/rgm/api/` (teste), conforme o Princípio 7.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Onde publicar | No controller, pelo método `publicar` que as cinco transições já usam | Publicar a partir dos casos de uso, por evento de domínio | Mantém um único ponto e um único formato; mover a publicação para o domínio é mudança maior, sem requisito que a peça |
| Dados do aviso | A mesma resposta devolvida ao chamador | Recarregar a solicitação com responsáveis | RNF-02: nenhuma consulta adicional |
| Aviso de comentário e de evidência | Evento próprio `solicitacao_atividade`, com `tipo` e `solicitacaoId` | Reusar o evento `solicitacao` com os dados completos da solicitação | Esses pontos de entrada não têm a solicitação em mãos; o evento completo custaria 1 consulta por comentário ou anexo (RNF-02). O frontend só precisa saber qual solicitação recarregar |
| Nome de evento separado | `solicitacao_atividade` | Mesmo nome `solicitacao` com outro formato de dado | Um cliente que lê `solicitacao.id` do evento atual quebraria com um dado sem `solicitacao` (RF-10) |
| Lista de responsáveis no aviso | Usar a lista recebida no pedido, já validada pelo caso de uso | Consultar as atribuições depois da alteração | O caso de uso substitui os responsáveis pela lista do pedido; não há consulta adicional |
| Onde publicar o aviso de evidência | No controller de evidências, que passa a receber o publicador | Publicar no caso de uso | Mesmo ponto das demais publicações; o caso de uso continua sem conhecer SSE |
| Documentação | Seção nova em `docs/casos-de-uso.md` | Swagger | O projeto documenta casos de uso nesse arquivo |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Nenhum novo | — | — | — |

Considerado e recusado: **Observer de domínio** (evento de domínio por operação, com um
ouvinte que publica no SSE). Resolveria o acoplamento do controller ao publicador, mas é
refatoração das oito publicações, fora do pedido.

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| adapters/controllers | `adapter/in/web/solicitacao/SolicitacaoController.java` | alterar: publicar em abrir, editar, gerenciar responsáveis (com a lista) e comentar | `adapter/in/web/solicitacao/SolicitacaoControllerTest.java` (alterar) |
| adapters/controllers | `adapter/in/web/solicitacao/SolicitacaoAtividadeEvent.java` | criar: dado do aviso de atividade | coberto pelos testes dos dois controllers |
| adapters/controllers | `adapter/in/web/evidencia/EvidenciaController.java` | alterar: publicar ao anexar | `adapter/in/web/evidencia/EvidenciaControllerTest.java` (alterar) |
| documentação | `docs/casos-de-uso.md` | alterar: tabela de tipos de aviso | — |

Rastreabilidade: RF-01 a RF-05, RF-07, RF-08 e RNF-01 a RNF-03 em `SolicitacaoController`;
RF-09 em `EvidenciaController`; RF-10 em `SolicitacaoAtividadeEvent` e nos dois
controllers; RF-11 nos testes dos dois controllers; RF-06 em `docs/casos-de-uso.md`.

## Contrato entre camadas

O controller chama o caso de uso, converte a solicitação resultante na resposta e entrega a
mesma resposta ao publicador, com o tipo do aviso. Se o caso de uso lança exceção, nada é
publicado. Para comentário e anexo, o controller publica o evento `solicitacao_atividade`
com o tipo e o identificador da solicitação, depois do retorno do caso de uso. `core` não muda.

## Dependências externas

| Dependência | Versão | Justificativa | Simulada nos testes por |
|---|---|---|---|
| Nenhuma nova | — | — | — |

## Impacto no contrato de operação

Nenhum.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Aviso publicado antes de a transação do caso de uso ser confirmada | baixa | A publicação ocorre depois do retorno do caso de uso, como nas cinco transições existentes |
| Frontend ignorar os tipos novos | média | Hoje ele invalida as listas para qualquer aviso `solicitacao`; confirmar na rgm-frontend#113 |
| Todos os usuários conectados recebem os dados de toda solicitação | certa | Comportamento já existente nos avisos atuais; filtrar por usuário está fora de escopo |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Contrato de operação | Validação por `make validate`; nenhum alvo novo. Vale o desvio da feature 001: sem alvos `it` e `bdd` |
| Arquitetura limpa | Mudança só no controller; nenhuma regra de negócio nova e `core` intocado |
| Testes provam a entrega | Testes novos em AAA, `shouldXWhenY`, um comportamento por teste e `verifyNoMoreInteractions` |
| Simplicidade defensável | Três chamadas ao método de publicação que já existe |
| Autoria | Commits e PR só com o autor do `git config` |
| Idioma | Artefatos em português; termos de domínio em português |
| Mapa de camadas (7) | Pacotes existentes; nenhum pacote novo no padrão da organização |
| Linguagem ubíqua (8) | Tipos de aviso em português, no padrão dos existentes (`aberta`, `editada`, `responsaveis_alterados`) |
| Compatibilidade com produção (9) | Só acrescenta tipos de aviso; os cinco existentes mantêm nome e formato |
| Cobertura não regride (10) | Mínimo do JaCoCo intocado e nenhuma exclusão nova; arquivos alterados com 95% ou mais de linha |
| Padrão de teste no que for tocado (11) | Só testes novos e alterados seguem o padrão |
| Uma fonte de especificação (12) | Spec nesta pasta; `openspec/` não é alterado |
