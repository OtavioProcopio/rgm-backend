# Especificação — Segurança e robustez da API: permissões, exposição e limites

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.
>
> Origem: issues #97, #122, #99, #113 e #89 do `rgm-backend`. Base: `develop`. Todos os
> comportamentos "hoje" abaixo foram **medidos em 2026-10-10** contra o `develop` em execução
> (`39ce64c`), com um operador de teste e o administrador do ambiente de desenvolvimento.

## Problema

A API tem cinco falhas de permissão, exposição e tratamento de entrada que não impedem o uso
do dia a dia, mas deixam o sistema inseguro e frágil para ir a produção:

1. **Indicadores agregados abertos a qualquer perfil (#97).** `GET /api/solicitacoes/metricas`,
   `/metricas/historico` e `/metricas/por-modelo` respondem **200** a um operador autenticado.
   O caso de uso UC-16 (`docs/casos-de-uso.md`) define os atores como gestor e administrador, e
   o painel esconde os indicadores do operador, mas a API entrega os números a quem estiver
   logado.
2. **Endpoints operacionais abertos sem login (#122).** `GET /actuator/prometheus`,
   `GET /actuator` e `GET /swagger-ui/index.html` respondem **200 sem token**. O primeiro expõe
   memória, pools de conexão, nomes de rota e contagem de requisições a quem alcançar a porta.
   O `rgm-infra` não bloqueia esses caminhos.
3. **Entrada inválida vira erro interno (#99).** Corpo JSON malformado em
   `POST /api/solicitacoes`, datas inválidas nos filtros (`criadaEmInicio`, `criadaEmFim`,
   `dataInicio`, `dataFim`) e `page` negativo ou `size` zero ou negativo respondem **500**, com a
   mensagem genérica "Erro interno do servidor". Erro do cliente deveria ser 4xx, e cada 500 sujo
   o registro de erros e esconde falhas reais. Os valores de enumeração inválidos já respondem
   400, mas a mensagem expõe o nome completo da classe interna.
4. **Falta de permissão responde 401 sem corpo (#113).** Um operador autenticado em
   `GET /api/admin/usuarios` recebe **401 com corpo vazio**, em vez de 403 com o formato de erro
   do resto da API. O frontend trata 401 como sessão expirada e tenta renovar ou sair; hoje isso
   só não derruba a sessão porque a tela de administração bloqueia o operador antes de chamar.
5. **Tamanho de página sem limite (#89).** `GET /api/solicitacoes?size=5000` devolveu os 636
   itens do ambiente de uma vez. Com mais dados, consultas pesadas e risco de estouro de memória.
   O frontend já não pede página grande (o maior pedido é 100), então o limite pode entrar.

**Custo de não resolver:** quem tem acesso à rede da API lê métricas internas; o operador vê
totais de gestão; e o primeiro cliente novo que bata num 403 derruba a sessão do usuário.

## Objetivo

A API passa a: restringir os indicadores agregados a gestor e administrador; expor sem login
somente a verificação de saúde; responder 4xx com mensagem clara a toda entrada inválida do
cliente; responder 403 com corpo padrão a quem está autenticado mas sem permissão; e limitar o
tamanho da página.

## Fora de escopo

- #100 (login revela conta inativa) e #101 (conteúdo do arquivo enviado como evidência): têm
  issue própria e não entram aqui.
- Novos indicadores e novos endpoints de métricas (#117 a #121 e #123).
- Mover o refresh token para cookie `HttpOnly` e o token curto do SSE (#93).
- Mudar o limite de tentativas de login, o formato do token ou o tempo de sessão.
- Mudar o frontend. A dependência dele (ver RF-06) é registrada aqui e entregue em spec do
  `rgm-frontend`.
- Mudar o comportamento dos relatórios em PDF de lista (`/api/solicitacoes/relatorio`,
  `/api/modelos/relatorio` e `/api/modelos/{id}/relatorio`): a restrição fica só nos quatro
  endpoints de indicadores (Esclarecimento 3). Os relatórios viram issue própria, junto com a
  regra de visibilidade do operador.
- Dar ao operador totais próprios na tela de perfil: o card some para ele (Esclarecimento 1).
- Alterar o `rgm-infra` (configuração do Prometheus, compose ou variáveis): o usuário não o usa
  de fato (Esclarecimento 6). Quem passar a coletar configura a credencial por conta própria,
  seguindo a documentação desta entrega.

## Personas e cenários de uso

- **Operador:** abre e acompanha as próprias solicitações. Não deve receber indicadores
  agregados, e não pode ser deslogado por um erro de permissão.
- **Gestor e administrador:** usam o painel, o ranking por modelo e a administração de
  usuários. Nada do que usam hoje pode passar a falhar.
- **Quem opera o ambiente:** precisa que o orquestrador consulte a saúde sem credencial e que
  as métricas internas não fiquem abertas.
- **Cliente da API (frontend e integrações):** precisa distinguir "não autenticado" (401),
  "sem permissão" (403) e "pedido inválido" (400) pelo código e pelo corpo.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | Os indicadores agregados de solicitações (`/api/solicitacoes/metricas`, `/metricas/historico`, `/metricas/por-modelo` e `/metricas/por-modelo/pdf`) devem responder somente a gestor e administrador. Operador recebe 403. | obrigatório |
| RF-02 | Toda requisição autenticada que a regra de perfil recusa deve responder **403** com corpo no formato de erro padrão da API (status, tipo do erro, mensagem em português e instante). Sem credencial válida, continua 401. | obrigatório |
| RF-03 | Sem credencial, somente a verificação de saúde (`/actuator/health`) deve responder. O endpoint de métricas do Prometheus e o índice do actuator devem exigir uma **credencial de coleta própria**, definida por configuração do ambiente (variável de ambiente, fora do repositório) e distinta de qualquer usuário humano. Sem a credencial, ou com credencial errada, respondem 401. Sem a credencial configurada no ambiente, o endpoint de métricas não aceita ninguém. | obrigatório |
| RF-04 | **Retirado em 2026-10-10** (Esclarecimento 6): o `rgm-infra` não é usado de fato, então a configuração do coletor dele não muda nesta entrega. O ID fica reservado para não renumerar. | — |
| RF-05 | Entrada inválida do cliente nunca deve responder 5xx: corpo JSON malformado ou com tipo errado, datas fora do formato e `page` ou `size` fora do intervalo válido respondem **400** com mensagem em português que diz o campo e o formato esperado. | obrigatório |
| RF-06 | A restrição do RF-01 não pode ser publicada antes de o frontend deixar de chamar os indicadores agregados para o operador. Hoje a tela de perfil chama `/api/solicitacoes/metricas` para qualquer perfil e mostra a seção "Visão Geral do Sistema"; a seção passa a aparecer só para gestor e administrador, e o operador não a vê. A ordem de publicação é: frontend, depois backend. Se o backend for publicado antes, a seção de totais do operador some, porque a resposta 403 é tratada como ausência de dado, e a sessão dele não é derrubada (403 com credencial válida não renova nem encerra a sessão). | obrigatório |
| RF-07 | As listagens paginadas (`/api/solicitacoes`, `/api/solicitacoes/metricas/por-modelo`, `/api/modelos` e `/api/admin/usuarios`) devem aceitar `size` entre 1 e 100. Valor acima de 100 responde 400 com mensagem que diz o máximo. Sem `size`, vale o padrão atual (20). | obrigatório |
| RF-08 | A mensagem de erro de valor de enumeração inválido (status, tipo, prioridade, tipo de data e perfil) deve dizer o campo e os valores aceitos, sem nome de classe ou pacote interno. | obrigatório |
| RF-09 | Nenhum caminho, campo, código de status de sucesso ou evento existente é removido ou renomeado. | obrigatório |
| RF-10 | Em produção, a interface Swagger UI não deve ser servida; em desenvolvimento, segue como hoje. | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Nenhuma entrada inválida do cliente produz 5xx | 0 respostas 5xx nos 12 casos de entrada inválida da seção "Fatos medidos" (corpo malformado, 4 datas, `page=-1`, `size=0`, `size=-5`, `size=101`, `size=5000`) |
| RNF-02 | Segurança por padrão | Das rotas do actuator, apenas `health` responde 200 sem token; `/actuator`, `/actuator/prometheus` e `/actuator/metrics` respondem 401 sem token |
| RNF-03 | A verificação de saúde continua barata e anônima | `GET /actuator/health` responde 200 em até 1 s sem token, medido no ambiente de desenvolvimento |
| RNF-04 | Cobertura de teste não regride (Princípio 10 da constituição) | Cada arquivo modificado com 95% ou mais de linha; limite do conjunto mantido em 95% |
| RNF-05 | Compatibilidade com a versão em produção (Princípio 9) | 0 rotas, campos ou eventos removidos ou renomeados em relação ao `develop` de partida |

## Fatos medidos

Entradas inválidas que respondem 500 hoje (RNF-01 parte daqui):

| Pedido | Hoje | Esperado |
|---|---|---|
| `POST /api/solicitacoes` com JSON quebrado | 500 | 400 |
| `GET /api/solicitacoes?criadaEmInicio=ontem` | 500 | 400 |
| `GET /api/solicitacoes?criadaEmFim=ontem` | 500 | 400 |
| `GET /api/solicitacoes?dataInicio=32/13/2026` | 500 | 400 |
| `GET /api/solicitacoes?dataFim=xx` | 500 | 400 |
| `GET /api/solicitacoes?page=-1` | 500 | 400 |
| `GET /api/solicitacoes?size=0` e `size=-5` | 500 | 400 |
| `GET /api/modelos?size=0` | 500 | 400 |
| `GET /api/admin/usuarios?size=-1` | 500 | 400 |
| `GET /api/solicitacoes?size=101` | 200 com 101 itens | 400 |
| `GET /api/solicitacoes?size=5000` | 200 com 636 itens | 400 |

Já corretos e que não podem regredir: `status`, `tipo`, `prioridade`, `tipoData` e `perfil`
inválidos respondem 400; `/metricas/historico?dias=abc` responde 400; `/metricas/por-modelo?sort=XXX`
responde 400.

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Segurança e robustez da API

  Cenário: Operador não recebe indicadores agregados
    Dado um usuário autenticado com perfil OPERADOR
    Quando ele consulta "/api/solicitacoes/metricas"
    Então a resposta tem status 403
    E o corpo traz status, tipo do erro, mensagem e instante
    Mas o corpo não traz nenhum indicador

  Cenário: Operador não recebe o histórico nem o ranking por modelo
    Dado um usuário autenticado com perfil OPERADOR
    Quando ele consulta "/api/solicitacoes/metricas/historico", "/api/solicitacoes/metricas/por-modelo" e "/api/solicitacoes/metricas/por-modelo/pdf"
    Então as três respostas têm status 403

  Cenário: Gestor e administrador continuam recebendo os indicadores
    Dado um usuário autenticado com perfil GESTOR
    Quando ele consulta "/api/solicitacoes/metricas"
    Então a resposta tem status 200
    E o mesmo vale para o perfil ADMINISTRADOR

  Cenário: Falta de permissão responde 403 com corpo padrão
    Dado um usuário autenticado com perfil OPERADOR
    Quando ele consulta "/api/admin/usuarios"
    Então a resposta tem status 403
    E o corpo traz status, tipo do erro, mensagem em português e instante
    Mas a resposta não tem status 401

  Cenário: Sem credencial continua 401
    Dado uma requisição sem credencial
    Quando ela consulta "/api/solicitacoes/metricas"
    Então a resposta tem status 401

  Cenário: A saúde continua anônima
    Dado uma requisição sem credencial
    Quando ela consulta "/actuator/health"
    Então a resposta tem status 200 e o corpo indica "UP"

  Cenário: As métricas internas exigem credencial
    Dado uma requisição sem credencial
    Quando ela consulta "/actuator/prometheus"
    Então a resposta tem status 401
    Mas a consulta a "/actuator/health" na mesma execução continua com status 200

  Cenário: A credencial de coleta abre as métricas
    Dado a credencial de coleta configurada no ambiente
    Quando uma requisição a "/actuator/prometheus" apresenta essa credencial
    Então a resposta tem status 200 e traz as métricas

  Cenário: Credencial errada não abre as métricas
    Dado a credencial de coleta configurada no ambiente
    Quando uma requisição a "/actuator/prometheus" apresenta outra credencial
    Então a resposta tem status 401

  Cenário: Um usuário humano, mesmo administrador, não abre as métricas pela sessão
    Dado um usuário autenticado com perfil ADMINISTRADOR e sem a credencial de coleta
    Quando ele consulta "/actuator/prometheus"
    Então a resposta tem status 401 ou 403
    Mas a resposta não traz métricas

  Cenário: Sem credencial configurada ninguém coleta
    Dado um ambiente sem credencial de coleta configurada
    Quando qualquer requisição consulta "/actuator/prometheus"
    Então a resposta não traz métricas

  Cenário: O Swagger UI não é servido em produção
    Dado a aplicação executando com a configuração de produção
    Quando uma requisição sem credencial consulta "/swagger-ui/index.html"
    Então a resposta não traz a interface do Swagger
    E o status é 401 ou 404 (rota inexistente sem credencial responde 401 nesta API)
    Mas a consulta a "/actuator/health" continua com status 200

  Cenário: O Swagger UI segue disponível em desenvolvimento
    Dado a aplicação executando com a configuração de desenvolvimento
    Quando uma requisição consulta "/swagger-ui/index.html"
    Então a resposta tem status 200

  Cenário: Corpo JSON malformado responde 400
    Dado um usuário autenticado
    Quando ele envia "{ json quebrado" para "POST /api/solicitacoes"
    Então a resposta tem status 400
    E a mensagem está em português
    Mas a resposta não tem status 500

  Cenário: Data inválida no filtro responde 400
    Dado um usuário autenticado
    Quando ele consulta "/api/solicitacoes?criadaEmInicio=ontem"
    Então a resposta tem status 400
    E a mensagem diz o campo e o formato de data esperado

  Esquema do Cenário: Paginação fora do intervalo responde 400
    Dado um usuário autenticado
    Quando ele consulta "<caminho>?<parametro>"
    Então a resposta tem status 400
    E a mensagem diz o intervalo válido

    Exemplos:
      | caminho                          | parametro |
      | /api/solicitacoes                | page=-1   |
      | /api/solicitacoes                | size=0    |
      | /api/solicitacoes                | size=-5   |
      | /api/solicitacoes                | size=101  |
      | /api/solicitacoes                | size=5000 |
      | /api/modelos                     | size=0    |
      | /api/admin/usuarios              | size=-1   |
      | /api/solicitacoes/metricas/por-modelo | size=101 |

  Cenário: Página no limite é aceita
    Dado um usuário autenticado
    Quando ele consulta "/api/solicitacoes?size=100"
    Então a resposta tem status 200
    E a página traz no máximo 100 itens

  Cenário: Sem tamanho informado vale o padrão
    Dado um usuário autenticado
    Quando ele consulta "/api/solicitacoes"
    Então a resposta tem status 200
    E a página traz no máximo 20 itens

  Cenário: Valor de enumeração inválido não expõe a classe interna
    Dado um usuário autenticado
    Quando ele consulta "/api/solicitacoes?status=INVALIDO"
    Então a resposta tem status 400
    Mas a mensagem não contém "com.rgm" nem "No enum constant"

  Cenário: O frontend não pede indicadores agregados ao operador (verificado no rgm-frontend)
    Dado um usuário autenticado com perfil OPERADOR
    Quando ele abre a tela de perfil
    Então nenhuma chamada a "/api/solicitacoes/metricas" é feita
    Mas a tela abre sem erro e sem a seção de totais globais

  Cenário: As rotas existentes seguem iguais
    Dado a lista de rotas, campos de resposta e eventos do "develop" de partida
    Quando ela é comparada com a da entrega
    Então toda rota, campo e evento existente continua presente com o mesmo nome
    Mas rotas de sucesso não mudam de código de status
```

## Ambiguidades

Nenhuma em aberto. As cinco decisões estão na tabela **Esclarecimentos**, ao fim do documento.

## Métricas de sucesso

- 0 respostas 200 a operador nos quatro endpoints de indicadores agregados, medido por
  sonda automática no ambiente de desenvolvimento depois da entrega.
- 0 respostas 5xx para os 12 casos de entrada inválida da seção "Fatos medidos".
- `/actuator/prometheus` anônimo responde 401 e `/actuator/health` anônimo responde 200.
- 403 (e não 401) em `GET /api/admin/usuarios` para o operador, medido pela mesma sonda.

## Esclarecimentos

Rodada de 2026-10-10. Todas as respostas foram dadas pelo usuário.

| # | Pergunta | Resposta | Onde entrou |
|---|---|---|---|
| 1 | O operador continua vendo os totais globais na tela de perfil, já que a #97 restringe os indicadores agregados? | **Esconder do operador.** O frontend para de pedir os indicadores e a seção "Visão Geral do Sistema" some só para ele; gestor e administrador seguem iguais. Sem endpoint novo de totais do operador. | RF-06, Fora de escopo, cenário do perfil |
| 2 | Como o coletor de métricas acessa o `/actuator/prometheus` depois de deixar de ser anônimo? | **Credencial de coleta própria, por variável de ambiente**, distinta de usuário humano. O ajuste do coletor do `rgm-infra` foi retirado pelo Esclarecimento 6. | RF-03, cenários da credencial |
| 3 | Os PDFs de lista (`/api/solicitacoes/relatorio`, `/api/modelos/relatorio`, `/api/modelos/{id}/relatorio`) entram na restrição do RF-01? | **Fora desta spec.** Só os quatro endpoints de indicadores. Os relatórios viram issue própria, com a regra de visibilidade do operador. | RF-01, Fora de escopo |
| 4 | O Swagger UI, que abre sem login e vazio, deve exigir autenticação? | **Desligar em produção** (a recomendação era exigir autenticação; o usuário escolheu não servir em produção). Em desenvolvimento segue como hoje. | RF-10, cenários do Swagger |
| 5 | A mensagem de enumeração inválida sem nome de classe interna entra nesta entrega? | **Entra.** A mensagem passa a dizer o campo e os valores aceitos. | RF-08 (de desejável para obrigatório) |
| 6 | O coletor de desenvolvimento do `rgm-infra` precisa continuar coletando (RF-04)? | **Não.** O usuário disse, em 2026-10-10, que o `rgm-infra` "nem está sendo usado", só foi citado para eu saber que existe. Nenhuma mudança nele; o RF-04 e o cenário do coletor saem. A credencial de coleta (decisão 2) continua valendo para quem passar a coletar. | RF-04 (retirado), Fora de escopo |
| 7 | Revisão dos checklists (`requisitos`, `seguranca` e `operacao`, 43 itens): três lacunas encontradas ao gerá-los. | O usuário respondeu "Aprovo as 3 correções" (2026-10-10): (1) o RF-06 diz o que acontece se o backend sair antes do frontend; (2) o cenário do frontend diz que é verificado no `rgm-frontend`; (3) a métrica de "operador deslogado" sai, porque nenhum registro existente a mostra, e entra a sonda do 403 em `/api/admin/usuarios`. A revisão fica registrada aqui; os checklists mantêm as caixas desmarcadas, como na spec 013 do `rgm-frontend`. | RF-06, cenário do frontend, Métricas de sucesso |
| 8 | **Correção de fato, não é decisão do usuário** (apontada pelo `/bu:analyze`): o cenário "O Swagger UI não é servido em produção" afirmava status 404. | Medido em 2026-10-10: rota inexistente sem credencial responde 401 nesta API, mesmo quando liberada. O cenário passou a exigir "não traz a interface do Swagger, com status 401 ou 404". O RF-10 não mudou. | Cenário do Swagger |
