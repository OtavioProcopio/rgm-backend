# Plano de implementação — Usuário desativado perde o acesso imediatamente

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.

Caminhos relativos a `app/src/main/java/com/rgm/api/` (produção) e
`app/src/test/java/com/rgm/api/` (teste), conforme o Princípio 7.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Ponto de bloqueio | O filtro de autenticação consulta o usuário e só autentica se ele existe e está ativo | Verificar em cada caso de uso de ação (sete classes) | Um ponto cobre todas as chamadas, inclusive consultas e abertura de solicitação, e não depende de lembrar da verificação em caso de uso novo |
| Resposta ao usuário inativo | Não autenticar e deixar a cadeia seguir; a regra de segurança existente responde 401 | Responder 403 direto no filtro | Mesmo caminho de um acesso vencido; o frontend já trata 401 (ver spec) |
| Conexão de tempo real | O controller de SSE, que valida o acesso por conta própria, faz a mesma consulta | Passar o SSE pelo filtro | O `EventSource` do navegador não envia cabeçalho; o acesso vem na URL e é validado no controller |
| Custo | 1 consulta por identificador a cada chamada autenticada | Cache em memória de usuários ativos | Consulta por chave primária; cache traria janela de acesso residual, que é o problema a resolver |
| Perfil usado na autorização | Continua vindo do acesso, como hoje | Usar o perfil lido do banco | Mudança de perfil na sessão aberta está fora de escopo |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Nenhum novo | — | — | — |

Considerados e recusados: **Decorator** sobre os casos de uso para verificar o usuário
ativo, porque o filtro já é o ponto único por onde toda chamada passa; **lista de acessos
revogados**, porque exige armazenamento e limpeza para um ganho que a consulta ao usuário
já entrega.

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| infra/tools | `adapter/out/security/JwtAuthenticationFilter.java` | alterar: recebe o repositório de usuários e só autentica usuário ativo | `adapter/out/security/JwtAuthenticationFilterTest.java` (alterar) |
| adapters/controllers | `adapter/in/web/solicitacao/SolicitacaoSseController.java` | alterar: recusa usuário inativo ou inexistente | `adapter/in/web/solicitacao/SolicitacaoSseControllerTest.java` (criar) |
| documentação | `docs/casos-de-uso.md` | alterar: regra de UC-01 e da seção de eventos | — |

Rastreabilidade: RF-01, RF-02, RF-03, RF-05 e RNF-01 a RNF-03 em `JwtAuthenticationFilter`;
RF-04 em `SolicitacaoSseController`; RNF-04 nos dois arquivos de teste.

`SolicitacaoSseController` está na lista de exclusões do JaCoCo desde antes; ganha teste
espelhado nesta feature, mas a exclusão não é removida aqui.

## Contrato entre camadas

O filtro valida a assinatura, a validade e o tipo do acesso como hoje. Em seguida pede o
usuário ao `UsuarioRepository` pelo identificador do acesso. Usuário ausente ou inativo: o
filtro registra em log e segue a cadeia sem autenticar. Usuário ativo: autentica como hoje.
O controller de SSE faz a mesma consulta depois de validar o acesso e responde 401 quando o
usuário está ausente ou inativo.

## Dependências externas

| Dependência | Versão | Justificativa | Simulada nos testes por |
|---|---|---|---|
| Nenhuma nova | — | — | — |

## Impacto no contrato de operação

Nenhum.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Consulta adicional em toda chamada aumentar a latência | baixa | Busca por chave primária; RNF-02 limita a 1 consulta |
| Banco indisponível passa a derrubar a autenticação | baixa | Sem banco a API já não atende; a falha é registrada e a chamada segue sem autenticação (401) |
| Testes de fatia web quebrarem com a dependência nova do filtro | baixa | Os testes de controller já excluem o filtro |
| Identificador do acesso em formato inválido | baixa | Tratado como falha de validação: segue sem autenticar |
| Conexão de tempo real aberta antes da desativação continua recebendo avisos | certa | Fora de escopo; termina no tempo limite de 30 min |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Contrato de operação | Validação por `make validate`; nenhum alvo novo. Vale o desvio da feature 001: sem alvos `it` e `bdd` |
| Arquitetura limpa | A verificação fica na borda (filtro e controller de SSE) e consulta o usuário pela porta de repositório do domínio; `core` não muda |
| Testes provam a entrega | Testes novos em AAA, `shouldXWhenY`, um comportamento por teste e `verifyNoMoreInteractions`; os testes existentes do filtro só recebem a dependência nova |
| Simplicidade defensável | Um ponto de bloqueio em vez de sete verificações espalhadas; nenhum padrão novo |
| Autoria | Commits e PR só com o autor do `git config` |
| Idioma | Artefatos em português |
| Mapa de camadas (7) | Pacotes existentes (`adapter.out.security`, `adapter.in.web.solicitacao`) |
| Linguagem ubíqua (8) | Usa `Usuario.isAtivo()`, já existente |
| Compatibilidade com produção (9) | Nenhuma mudança para usuário ativo; usuário inativo passa a receber 401, que o frontend da v1.5.0 já trata |
| Cobertura não regride (10) | Mínimo do JaCoCo intocado e nenhuma exclusão nova |
| Padrão de teste no que for tocado (11) | Testes novos no padrão; os antigos do filtro mantêm o formato |
| Uma fonte de especificação (12) | Spec nesta pasta |
