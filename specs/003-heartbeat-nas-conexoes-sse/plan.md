# Plano de implementação — Heartbeat nas conexões SSE

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.

Caminhos relativos a `app/src/main/java/com/rgm/api/` (produção) e
`app/src/test/java/com/rgm/api/` (teste), conforme o Princípio 7.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Forma do sinal | Comentário SSE `ping`, enviado pelo publicador de eventos | Evento nomeado `heartbeat` | Comentário é ignorado pelo `EventSource` do navegador; não exige mudança no frontend nem cria tipo de evento novo (RNF-03) |
| Disparo periódico | Tarefa agendada com taxa fixa de 25 s | Thread própria; um agendamento por conexão | O agendador do framework já existe; uma tarefa única percorre todas as conexões |
| Onde habilitar o agendamento | Classe de configuração própria em `adapter/config` | Anotar a classe principal | Na classe principal, o agendamento também subiria nos testes de fatia web |
| Envio a todas as conexões | Um método privado único, usado pela publicação de evento e pelo sinal | Duplicar o laço | Mesma regra de descarte de conexão para os dois envios |
| Tempo limite da conexão | 30 minutos | Manter 5 minutos | Com o sinal, a conexão não cai por inatividade; reconectar a cada 5 min deixa de ser necessário |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Nenhum novo | — | — | — |

O publicador já é um Observer (lista de conexões inscritas). Considerado e recusado:
**Template Method** para "enviar a todos", porque um método privado que recebe o construtor
do evento resolve sem hierarquia.

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| adapters/controllers | `adapter/in/web/solicitacao/SolicitacaoEventPublisher.java` | alterar: sinal periódico e envio único | `adapter/in/web/solicitacao/SolicitacaoEventPublisherTest.java` (alterar) |
| infra/init | `adapter/config/SchedulingConfig.java` | criar: habilita agendamento | carregada pelo teste de contexto `ApiApplicationTests` |
| adapters/controllers | `adapter/in/web/solicitacao/SolicitacaoSseController.java` | alterar: tempo limite de 30 min | sem teste (classe fora da medição desde antes) |
| documentação | `docs/casos-de-uso.md` | alterar: seção de eventos em tempo real | — |

Rastreabilidade: RF-01, RF-02, RF-03 e RNF-01 em `SolicitacaoEventPublisher` e
`SchedulingConfig`; RF-04 e RNF-02 em `SolicitacaoSseController`; RNF-03 sem arquivo de
contrato alterado.

## Contrato entre camadas

O agendador chama o publicador a cada 25 s. O publicador percorre as conexões inscritas e
envia o comentário; falha de envio ou conexão já encerrada remove a conexão da lista e é
registrada em log de depuração. Nada chega a `core`.

## Dependências externas

| Dependência | Versão | Justificativa | Simulada nos testes por |
|---|---|---|---|
| Nenhuma nova | — | — | — |

## Impacto no contrato de operação

Nenhum alvo, serviço ou variável de ambiente novo.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| O proxy do frontend continuar retendo os dados em buffer | alta | Depende da rgm-frontend#113; citar no PR |
| Tempo limite de 30 min sem teste automatizado | certa | Classe já estava fora da medição; verificar em homologação |
| Agendamento ativo em testes de contexto completo | baixa | Sem conexões, a tarefa não faz nada |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Contrato de operação | Validação por `make validate`; nenhum alvo novo. Vale o desvio da feature 001: sem alvos `it` e `bdd` |
| Arquitetura limpa | Mudança só na borda web e na configuração; `core` intocado |
| Testes provam a entrega | Testes novos em AAA, `shouldXWhenY`, um comportamento por teste e `verifyNoMoreInteractions` |
| Simplicidade defensável | Um método agendado e um método privado de envio; nenhum padrão novo |
| Autoria | Commits e PR só com o autor do `git config` |
| Idioma | Artefatos em português; termos de domínio em português |
| Mapa de camadas (7) | Pacotes existentes; nenhum pacote novo no padrão da organização |
| Linguagem ubíqua (8) | Nomes em português (`enviarHeartbeat`), termos técnicos em inglês |
| Compatibilidade com produção (9) | Nenhum evento removido ou renomeado; o comentário é ignorado por clientes existentes |
| Cobertura não regride (10) | Mínimo do JaCoCo intocado e nenhuma exclusão nova; arquivos alterados com 95% ou mais de linha |
| Padrão de teste no que for tocado (11) | Só testes novos e alterados seguem o padrão |
| Uma fonte de especificação (12) | Spec nesta pasta; `openspec/` não é alterado |
