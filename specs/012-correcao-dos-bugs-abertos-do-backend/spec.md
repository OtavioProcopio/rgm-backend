# Especificação — Correção dos bugs abertos do backend e melhorias que o frontend vai consumir

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.
>
> Origem: issues `rgm-backend#132`, `#111`, `#106`, `#103`, `#102`, `#101`, `#100` e `#105`
> (correções) e `#128`, `#129`, `#133`, `#131` e `#127` (melhorias que o frontend consome).
> Consumidores: `rgm-frontend#129` (card do quadro), `#132` (triagem com busca), `#142`
> (colunas encerradas) e `#149` (dashboard). Base: `develop` com a spec 011 mergeada. Os
> comportamentos "hoje" vêm do corpo das issues, medidos em `develop` em 2026-10-10, e são
> reconferidos no plano. Decisões já tomadas pelo usuário estão em **Esclarecimentos**.

## Problema

**Correções**

1. **Conta pública de administrador (#132).** O inicializador de dados cria, também em
   produção, `admin@rgm.com` com a senha `admin123` quando o e-mail não existe. Os dois valores
   estão no repositório. Num banco novo (restauração, novo ambiente) qualquer pessoa que leia o
   código entra como ADMINISTRADOR.
2. **Login entrega informação (#100).** Conta inativa responde "Usuário inativo" antes de a
   senha ser conferida; quem não sabe a senha descobre que o e-mail existe e foi desativado.
3. **Upload aceita o que não devia (#101).** Arquivo sem tipo declarado passa pela lista de
   tipos permitidos; o tipo é o que o cliente diz, não o conteúdo; o nome original do arquivo
   compõe o endereço do objeto, que é público.
4. **Versão de modelo repete (#102).** A versão nova é "quantidade + 1" sem restrição única:
   duas criações simultâneas ou uma criação depois de uma exclusão repetem a versão, e a
   identificação código + versão deixa de ser única. Toda violação de integridade vira a mesma
   mensagem genérica.
5. **Modelo travado pela máquina (#103).** Depois que a máquina é desativada, nenhum modelo dela
   pode ser editado, nem para corrigir a descrição.
6. **Responsáveis quebram no limite (#111).** Id repetido na lista responde "não encontrado";
   a troca de responsáveis não atualiza a solicitação e escapa do bloqueio otimista (duas
   trocas simultâneas se sobrepõem); dá para atribuir responsáveis antes da triagem, e a partir
   daí o operador que abriu não consegue cancelar.
7. **Cancelamento deixa trilhas diferentes (#106).** Cancelar por "cancelar" grava status e
   motivo no histórico; cancelar por "encerrar sem concluir" grava só o status.
8. **Edição sem rastro (#105).** Alterar título e descrição não deixa histórico, e o operador
   edita em qualquer etapa, inclusive durante a validação.

**Melhorias de que o frontend depende**

9. **Listagem sem código e máquina do modelo (#128).** `modeloCodigo` e `modeloMaquina` vêm
   nulos em 94 de 100 itens que têm modelo; o frontend faz uma requisição por item (fila de
   atenção) e faria 20 por coluna no quadro.
10. **Responsáveis limitados aos 100 primeiros usuários (#129).** A triagem monta a lista a
    partir da primeira página de usuários e filtra no navegador; quem passa do centésimo
    usuário não aparece, e a lista traz perfis que não podem ser atribuídos.
11. **Sem busca na listagem (#133).** Com 701 solicitações, achar uma só exige navegar páginas.
12. **Colunas encerradas sem ordem útil (#131).** As colunas Concluída e Cancelada só fazem
    sentido "encerradas mais recentemente primeiro", e a listagem não ordena por encerramento.
13. **Distribuição por tipo e prioridade custa 8 requisições (#127).** O dashboard conta por
    valor, uma requisição por tipo e por prioridade.

**Custo de não resolver:** uma conta de administrador conhecida por quem lê o repositório,
dados de modelo duplicados na fábrica, trilhas de auditoria incompletas, e um frontend que
continua com N+1, listas truncadas e contagens em duplicidade.

## Objetivo

- Nenhuma conta com senha fixa e pública existe fora de `dev` e `test`; o primeiro
  administrador de produção nasce de configuração do ambiente.
- Cada issue de bug listada acima deixa de reproduzir, com teste que a reproduz antes e passa
  depois.
- A listagem de solicitações traz o código, a máquina e a versão do modelo em uma consulta,
  aceita busca textual e ordenação por encerramento.
- Existe uma consulta de responsáveis disponíveis paginada e com busca, e as métricas trazem a
  contagem em aberto por tipo e por prioridade.
- Nada que o frontend em produção (v1.5.0) usa hoje deixa de funcionar (Princípio 9).

## Fora de escopo

- Indicadores de gestão novos (#119, #120, #121) e métricas de operação no Micrometer (#123).
- Mensagens de erro padronizadas com código estável (#130), busca de usuários na administração
  (#134), limite de upload por tipo (#135), refresh token em cookie (#93), anexo no mesmo
  request das ações (#91), relatórios em PDF (#110), usuário desativado com solicitações sem
  responsável (#104) e a dívida da spec 010 (#124).
- Servir evidências por URL assinada de curta duração (citado na #101): vira issue própria.
- Qualquer mudança no frontend: o consumo é das specs do frontend.
- Trocar a senha do administrador do banco de produção: ação operacional, independente do
  código, registrada em **Esclarecimentos**.
- Remover ou renomear campos e endpoints que o frontend v1.5.0 usa.

## Personas e cenários de uso

- **Operador da fábrica:** abre solicitação, edita enquanto ela está em A Fazer, cancela a que
  abriu; vê só o que abriu ou recebeu e pode buscá-lo por texto.
- **Gestor:** tria, escolhe responsáveis dentre todos os operadores ativos por busca, ordena as
  colunas, abre o dashboard com as contagens por tipo e prioridade.
- **Administrador:** gerencia o catálogo de máquinas e modelos, edita modelo de máquina
  desativada, e é o único com a senha inicial definida pela operação.
- **Pessoa sem conta:** não deve descobrir quais e-mails existem nem entrar com credencial
  pública.
- **Quem opera a produção:** define o primeiro administrador por configuração do ambiente.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | Em produção, o sistema não cria conta de administrador com senha fixa: o e-mail e a senha iniciais vêm de configuração do ambiente; sem a senha configurada, nenhuma conta é criada e o log registra aviso claro | obrigatório |
| RF-02 | Em produção, a senha inicial do administrador segue a política de senha vigente e a senha pública `admin123` é recusada | obrigatório |
| RF-03 | Em `dev` e `test`, a conta `admin@rgm.com` / `admin123` continua sendo criada (o e2e do frontend depende dela) | obrigatório |
| RF-04 | A documentação de operação e o exemplo de variáveis explicam como definir o primeiro administrador e como trocar a senha | obrigatório |
| RF-05 | Login com conta inativa e senha errada responde exatamente o mesmo que e-mail inexistente; a informação de conta inativa só chega a quem acertou a senha | obrigatório |
| RF-06 | O limite de tentativas de login continua valendo sem alteração | obrigatório |
| RF-07 | Upload de evidência sem tipo declarado responde 400 | obrigatório |
| RF-08 | Upload cujo conteúdo não corresponde ao tipo declarado (primeiros bytes) responde 400, para todos os tipos aceitos de evidência (JPEG, PNG, GIF, WebP, PDF e MP4) | obrigatório |
| RF-09 | O nome do objeto guardado é gerado pelo servidor; o nome original do arquivo aparece só nos dados da evidência, nunca no endereço | obrigatório |
| RF-10 | A versão de um modelo novo é a maior versão existente da mesma máquina e código mais 1; criar depois de excluir não repete versão | obrigatório |
| RF-11 | Não podem coexistir dois modelos com a mesma máquina, código e versão, nem sob criações simultâneas | obrigatório |
| RF-12 | Editar um modelo cujo código ou máquina passa a colidir com um modelo existente responde 409 com mensagem em português que diz qual combinação colidiu | obrigatório |
| RF-13 | A máquina informada para o modelo é gravada sem espaços nas pontas | obrigatório |
| RF-14 | Editar um modelo sem trocar a máquina funciona mesmo que a máquina esteja inativa; trocar para uma máquina inativa continua recusado | obrigatório |
| RF-15 | Lista de responsáveis com id repetido na triagem e na troca de responsáveis funciona como a lista sem repetição | obrigatório |
| RF-16 | A troca de responsáveis atualiza a solicitação (data de atualização e controle de versão); duas trocas concorrentes sobre a mesma versão resultam em 409 na segunda | obrigatório |
| RF-17 | A troca de responsáveis em solicitação ainda em A Fazer é recusada com mensagem que remete à triagem | obrigatório |
| RF-18 | Cancelar por "cancelar" e por "encerrar sem concluir" geram as mesmas atividades no histórico, incluindo o motivo; o contrato dos dois endpoints não muda | obrigatório |
| RF-19 | Toda edição de título ou descrição registra no histórico uma atividade com autor, data e os valores anteriores | obrigatório |
| RF-20 | O operador só edita a solicitação em A Fazer; fora disso recebe 403. Gestor e administrador editam em qualquer etapa aberta | obrigatório |
| RF-21 | As ações permitidas da solicitação refletem RF-17 e RF-20 para o frontend esconder os botões | obrigatório |
| RF-22 | A listagem e o detalhe de solicitação trazem código, máquina e versão do modelo para toda solicitação que tem modelo, sem consulta por item; solicitação de criação sem modelo mantém os dados informados na abertura | obrigatório |
| RF-23 | Existe uma consulta paginada de responsáveis disponíveis, com busca por parte do nome sem distinguir maiúsculas e acentos, ordenada por nome, que devolve só usuários ativos de perfil atribuível, com identificador, nome e perfil | obrigatório |
| RF-24 | A consulta de responsáveis disponíveis é restrita a quem tria (gestor e administrador); operador recebe 403 e não autenticado 401; o tamanho de página é limitado a 100 | obrigatório |
| RF-25 | A listagem de solicitações aceita busca textual `q` que casa, sem distinguir maiúsculas e acentos, com título, descrição e código do modelo, somada aos demais filtros e à visibilidade do operador | obrigatório |
| RF-26 | `q` com menos de 2 ou mais de 100 caracteres responde 400 com mensagem em português | obrigatório |
| RF-27 | A listagem aceita `sort=concluidaEm`, que ordena pela data de encerramento (conclusão ou cancelamento), com solicitações sem encerramento ao fim e desempate estável | obrigatório |
| RF-28 | A mensagem de erro de `sort` inválido lista `concluidaEm` entre os valores aceitos | obrigatório |
| RF-29 | As métricas trazem, de forma aditiva, a contagem das solicitações em aberto por tipo e por prioridade, com "sem prioridade" como grupo próprio; a soma de cada distribuição é igual ao total em aberto | obrigatório |
| RF-30 | A contagem por tipo e prioridade é restrita a gestor e administrador; operador recebe 403 | obrigatório |
| RF-31 | Nenhum campo ou endpoint usado pelo frontend v1.5.0 é removido ou renomeado | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Listagem com dados do modelo sem N+1 (RF-22) | Número de consultas ao banco por requisição de listagem igual para 10 e para 100 itens; teste de contagem |
| RNF-04 | Segurança do login (RF-05) | Resposta de e-mail inexistente e de conta inativa com senha errada idênticas em status, corpo e cabeçalhos; teste compara as duas |
| RNF-05 | Concorrência (RF-11, RF-16) | Duas criações simultâneas do mesmo código nunca geram a mesma versão (a segunda recebe 409 com mensagem clara); duas trocas simultâneas de responsáveis: 1 sucesso e 1 resposta 409 |
| RNF-06 | Cobertura | ≥ 95% de linhas por arquivo tocado (Princípio 10); `make validate` com 0 falhas |
| RNF-07 | Compatibilidade (Princípio 9) | 0 campos removidos ou renomeados nas respostas atuais; teste de contrato do formato antigo |
| RNF-08 | Migração de dados (RF-11) | Aplicar a migração sobre um banco com duplicatas não perde nenhum modelo nem altera código ou máquina; teste de migração |

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Correção dos bugs abertos do backend e melhorias para o frontend

  Cenário: Produção sem senha configurada não cria administrador
    Dado o perfil de produção sem a senha inicial do administrador configurada
    Quando a aplicação sobe
    Então nenhum usuário é criado
    E o log registra um aviso dizendo como configurar o administrador
    Mas a aplicação sobe normalmente

  Cenário: Produção com senha configurada cria o administrador com ela
    Dado o perfil de produção com e-mail e senha iniciais configurados
    Quando a aplicação sobe
    Então o administrador existe e entra com a senha configurada
    Mas a senha admin123 não funciona para ele

  Cenário: Produção recusa a senha pública
    Dado o perfil de produção com a senha inicial igual a admin123
    Quando a aplicação sobe
    Então nenhum usuário é criado
    E o log registra que a senha é recusada

  Cenário: Desenvolvimento e teste mantêm a conta de apoio
    Dado o perfil de desenvolvimento ou de teste
    Quando a aplicação sobe
    Então admin@rgm.com existe com a senha admin123

  Cenário: Conta inativa com senha errada é indistinguível de e-mail inexistente
    Dado um usuário inativo e um e-mail que não existe
    Quando cada um tenta entrar com uma senha errada
    Então as duas respostas têm o mesmo status e a mesma mensagem
    Mas nenhuma delas diz que a conta está inativa

  Cenário: Upload sem tipo declarado
    Dado uma solicitação aberta
    Quando se anexa uma evidência sem tipo de conteúdo
    Então a resposta é 400
    E nenhum arquivo é guardado

  Cenário: Upload com conteúdo diferente do tipo declarado
    Dado um arquivo de texto enviado com tipo image/png
    Quando se anexa como evidência
    Então a resposta é 400
    Mas um PNG verdadeiro continua sendo aceito

  Cenário: Nome do objeto não vem do cliente
    Dado uma evidência com o nome original "../../laudo final.png"
    Quando ela é guardada
    Então o endereço do objeto não contém o nome original
    E o nome original aparece nos dados da evidência

  Cenário: Versão do modelo depois de excluir
    Dado os modelos código X versões 1, 2 e 3 da mesma máquina
    E o modelo da versão 2 é excluído
    Quando se cria um novo modelo com código X nessa máquina
    Então a versão nova é 4

  Cenário: Criações simultâneas do mesmo código
    Dado nenhum modelo com o código Y
    Quando duas criações do código Y chegam ao mesmo tempo
    Então uma é aceita com a versão 1
    E a outra recebe 409 com mensagem clara
    Mas nenhuma versão se repete

  Cenário: Edição que colide com modelo existente
    Dado o modelo A código X versão 1 e o modelo B código Z versão 1 na mesma máquina
    Quando se edita o modelo B trocando o código para X
    Então a resposta é 409
    E a mensagem diz que já existe o código X, versão 1, nessa máquina

  Cenário: Editar descrição de modelo com máquina inativa
    Dado um modelo cuja máquina foi desativada
    Quando se altera só a descrição
    Então a edição é aceita

  Cenário: Trocar a máquina do modelo para uma inativa
    Dado um modelo com máquina ativa e outra máquina inativa
    Quando se troca a máquina do modelo para a inativa
    Então a edição é recusada

  Cenário: Responsáveis com id repetido
    Dado uma solicitação triada
    Quando o gestor define os responsáveis com o mesmo id duas vezes
    Então o resultado é igual ao de uma lista com o id uma vez

  Cenário: Duas trocas simultâneas de responsáveis
    Dado uma solicitação em andamento na versão 5
    Quando duas trocas de responsáveis partem da versão 5
    Então uma é aceita
    E a outra recebe 409

  Cenário: Responsáveis antes da triagem
    Dado uma solicitação em A Fazer
    Quando o gestor tenta trocar os responsáveis
    Então a resposta é recusada com orientação de usar a triagem
    E as ações permitidas da solicitação não oferecem a troca

  Cenário: Cancelar pelos dois caminhos gera o mesmo histórico
    Dado duas solicitações em andamento iguais
    Quando uma é cancelada por "cancelar" e a outra por "encerrar sem concluir", com o mesmo motivo
    Então as duas têm as mesmas atividades, incluindo o motivo

  Cenário: Edição registra o histórico
    Dado uma solicitação em A Fazer com título "Troca de correia"
    Quando o operador que a abriu altera o título para "Troca de correia dentada"
    Então o histórico ganha uma atividade com autor, data e o título anterior "Troca de correia"

  Cenário: Operador não edita depois da triagem
    Dado uma solicitação já triada aberta pelo operador
    Quando o operador tenta editar o título
    Então a resposta é 403
    E as ações permitidas não oferecem a edição a ele
    Mas o gestor consegue editar a mesma solicitação

  Cenário: Listagem traz o modelo de todas as solicitações
    Dado 100 solicitações com modelo, a maioria de manutenção
    Quando o gestor lista com tamanho 100
    Então as 100 trazem código, máquina e versão do modelo
    E o número de consultas é o mesmo que com 10 itens

  Cenário: Solicitação de criação sem modelo mantém seus dados
    Dado uma solicitação de criação ainda sem modelo, com código e máquina informados na abertura
    Quando ela aparece na listagem
    Então traz o código e a máquina informados na abertura

  Cenário: Responsáveis disponíveis além dos 100 primeiros usuários
    Dado 188 usuários, entre eles um operador ativo cujo nome começa com "Zélia"
    Quando o gestor busca responsáveis com "zelia"
    Então o operador aparece
    Mas administradores e externos não aparecem em nenhuma busca

  Cenário: Operador não lista responsáveis disponíveis
    Quando um operador consulta os responsáveis disponíveis
    Então a resposta é 403

  Cenário: Busca textual
    Dado solicitações com "correia" no título, na descrição e no código do modelo, e outras sem
    Quando o gestor lista com q=CORREIA
    Então vêm só as que contêm "correia", em qualquer caixa e acentuação

  Cenário: Busca respeita a visibilidade do operador
    Dado uma solicitação com "correia" que o operador não abriu nem recebeu
    Quando o operador lista com q=correia
    Então ela não aparece

  Cenário: Busca com tamanho inválido
    Quando se lista com q de 1 caractere ou com 101 caracteres
    Então a resposta é 400 com mensagem em português

  Cenário: Ordenar por encerramento
    Dado solicitações concluídas e canceladas em datas diferentes
    Quando se lista com sort=concluidaEm e dir=desc
    Então a encerrada mais recentemente vem primeiro, concluída ou cancelada
    E as que não foram encerradas vão para o fim

  Cenário: Ordenação inválida cita o novo critério
    Quando se lista com sort=titulo
    Então a resposta é 400
    E a mensagem inclui concluidaEm entre os valores aceitos

  Cenário: Contagem em aberto por tipo e prioridade
    Dado solicitações em aberto de tipos e prioridades variados, algumas sem prioridade, e outras encerradas
    Quando o gestor consulta as métricas
    Então vêm as contagens por tipo e por prioridade só das em aberto
    E "sem prioridade" aparece como grupo próprio
    E a soma de cada distribuição é o total em aberto
    Mas o operador recebe 403
```

## Adendo de 2026-10-11 — tipo do modelo cadastrável

Pedido do usuário (estava na lista de pendências do início da sessão e ficou de fora da primeira
versão da spec). O tipo do modelo passa a ser um catálogo administrável, como a máquina.

| ID | Requisito | Prioridade |
|---|---|---|
| RF-32 | O administrador cadastra, renomeia, ativa e desativa tipos de modelo; qualquer usuário autenticado lista | obrigatório |
| RF-33 | Os cinco tipos existentes entram no catálogo e nenhum modelo muda de tipo | obrigatório |
| RF-34 | Ao criar ou editar um modelo, o tipo (opcional) precisa existir e estar ativo no catálogo; editar sem trocar o tipo vale mesmo com o tipo desativado | obrigatório |
| RF-35 | Renomear um tipo de modelo atualiza o nome nos modelos que o usam; o mesmo vale para renomear uma máquina (hoje os modelos ficavam com o nome antigo) | obrigatório |

```gherkin
# language: pt
  Cenário: Tipo novo cadastrado pelo administrador
    Dado um administrador autenticado
    Quando ele cadastra o tipo "Aço inox"
    Então o tipo aparece na lista e pode ser usado em um modelo

  Cenário: Tipo fora do catálogo
    Quando se cria um modelo com um tipo que não está no catálogo
    Então a resposta é 400

  Cenário: Renomear tipo ou máquina atualiza os modelos
    Dado modelos que usam o tipo "RESINA"
    Quando o administrador renomeia o tipo para "Resina epóxi"
    Então os modelos passam a ter o tipo "Resina epóxi"
```

## Ambiguidades

Nenhuma aberta. As decisões pendentes que as issues traziam (#111 e #105) já estavam fechadas
no roteiro de 2026-10-10 (ver Esclarecimentos). Duas escolhas desta spec são **padrões
adotados** pelo redator, não decisões do usuário, e estão marcadas abaixo para revisão.

## Esclarecimentos

Decisões do usuário (roteiro de 2026-10-10 e pedido de 2026-10-11):

- **Escopo da 012:** todas as issues de bug abertas (#132, #111, #106, #103, #102, #101, #100),
  mais #105, mais as melhorias que o frontend vai consumir (#128, #129, #133, #131, #127), "sem
  gerar mais pendências". O que ficou de fora está em **Fora de escopo**.
- **#111:** responsáveis antes da triagem são recusados (RF-17).
- **#105:** o operador edita só em A Fazer, com histórico (RF-19 e RF-20).
- **Operacional (#132):** trocar a senha de `admin@rgm.com` no banco de produção não depende
  desta spec e deve ser feita já.

Pontos que o plano deve resolver com os dados reais, sem mudar o que a spec promete:

- Conferir se há modelos duplicados (máquina, código, versão) em produção antes da migração do
  RF-11 e como renumerar sem perda (RNF-08).
- Decidir o nome do caminho da consulta de responsáveis disponíveis (RF-23) e se a contagem
  por tipo e prioridade (RF-29) entra no endpoint de métricas existente ou no painel por
  período, sem remover nada (RF-31).
- **Padrão adotado (revisar):** quem acerta a senha de conta inativa continua recebendo a
  mensagem "usuário inativo" (RF-05); a alternativa seria a mensagem genérica para todos.
- **Padrão adotado (revisar):** os cinco itens de melhoria (#128, #129, #133, #131, #127) são
  os que o usuário chamou de "melhorias que vamos precisar"; #119, #120 e #121 (indicadores
  de gestão do front #150) ficaram para uma spec própria por serem trabalho de produto novo,
  não correção.

### Rodada de clarificação — 2026-10-11

Nenhuma marca aberta. Os dois padrões adotados pelo redator foram apresentados ao usuário em
"Revise antes de eu planejar", que respondeu "ok" e pediu para seguir.

| Pergunta | Resposta | Data |
|---|---|---|
| Quem acerta a senha de conta inativa continua vendo "usuário inativo"? | Sim (aceito com o "ok") | 2026-10-11 |
| Entram só #128, #129, #133, #131 e #127 como melhorias; #119–#121 ficam em spec própria? | Sim (aceito com o "ok") | 2026-10-11 |

## Métricas de sucesso

- Em produção, 0 contas com senha pública: o login com `admin123` responde 401 depois do deploy
  e da troca operacional.
- 0 respostas de listagem com `modeloId` e `modeloCodigo` nulo ao mesmo tempo para solicitação
  de manutenção; frontend sem requisição de modelo por item na fila e no quadro.
- 0 pares (máquina, código, versão) repetidos em `modelos`, conferido por consulta mensal.
- Dashboard com 1 requisição para a distribuição em vez de 8.
- Triagem alcança todos os operadores ativos por busca, mesmo passando de 100 usuários.
