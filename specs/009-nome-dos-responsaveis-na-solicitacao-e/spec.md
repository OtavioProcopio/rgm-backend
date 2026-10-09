# Especificação — Nome dos responsáveis na solicitação e capa da galeria sem erro

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.

Origem: OtavioProcopio/rgm-backend#114 e #115, pedidas numa especificação só em 2026-10-09.
Relacionadas no frontend: rgm-frontend#129 e #130 (spec 012, quadro e detalhe da solicitação)
e a ficha do modelo com foto em destaque.

## Problema

**Nome de quem cuida da solicitação (#114).** A resposta da solicitação, no detalhe e nas
listagens, traz só identificadores de quem a abriu e de quem é responsável por ela. O
frontend não consegue mostrar "de quem é":

1. O operador, que é quem mais usa o sistema na fábrica, não tem como saber o nome de um
   responsável: a consulta de usuários é restrita a gestor e administrador e responde 403 a ele.
2. Gestor e administrador só veem os nomes porque o frontend baixa até 100 usuários ativos e
   cruza por identificador. O cruzamento falha para quem não está nessa lista (administrador,
   usuário externo, usuário inativo, instalação com mais de 100 usuários) e custa uma chamada
   grande por tela.
3. O card do quadro e o detalhe da solicitação precisam mostrar, de relance, de quem é e quem abriu.

**Foto de capa (#115).** Definir uma foto como capa do modelo responde erro 500 e nenhuma foto
vira capa. A capa é o que identifica o molde na ficha e nas listas. A ação "Definir como capa"
do carrossel da galeria não funciona. Além de falhar hoje, a operação precisa ser indivisível:
trocar a capa passa por desmarcar a antiga e marcar a nova, e uma falha no meio não pode
deixar o modelo sem capa.

## Objetivo

Quem lê uma solicitação, inclusive o operador, recebe da própria API o nome dos responsáveis e
de quem abriu, sem consulta extra. Definir a foto de capa funciona e, quando falha, o modelo
mantém a capa que tinha.

## Fora de escopo

- Remover ou renomear `responsavelIds` e `abertaPorUsuarioId`: continuam na resposta (Princípio 9).
- Abrir o acesso do operador à consulta de usuários.
- Devolver nomes nos eventos de tempo real e nas respostas de ações (triar, mover, devolver,
  comentar e semelhantes): seguem como hoje, sem os campos novos.
- Devolver outros dados do usuário além do identificador e do nome (e-mail, perfil, situação).
- Mudar quem pode ver quais solicitações.
- Ajustes no frontend (consumir os campos novos, remover o cruzamento por identificador).
- Mudar as demais regras da galeria: limite de fotos, exclusão, legenda, ordem.
- Corrigir outras operações de escrita que já funcionam.

## Personas e cenários de uso

- **Operador** abre o detalhe de uma solicitação e vê o nome de quem é responsável e de quem
  a abriu, sem que o sistema precise de permissão de listar usuários.
- **Gestor ou administrador** olha o quadro e lê em cada card o nome do responsável, mesmo
  quando ele está inativo ou não é do perfil esperado.
- **Gestor** abre a ficha de um modelo com várias fotos e define a segunda como capa; a
  primeira deixa de ser capa.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | O detalhe da solicitação deve devolver a lista de responsáveis com identificador e nome, na mesma ordem de `responsavelIds`. | obrigatório |
| RF-02 | O detalhe da solicitação deve devolver o nome de quem a abriu; se quem abriu não puder ser resolvido, o campo vem presente com valor nulo. | obrigatório |
| RF-03 | A listagem de solicitações deve devolver os mesmos dois campos novos em cada item. | obrigatório |
| RF-04 | Os campos antigos `responsavelIds` e `abertaPorUsuarioId` devem continuar na resposta, com o mesmo conteúdo de hoje. | obrigatório |
| RF-05 | Solicitação sem responsável deve devolver a lista de responsáveis vazia, e não ausente. | obrigatório |
| RF-12 | Se um identificador de responsável não corresponder a nenhum usuário, o item entra em `responsaveis` com o identificador e nome nulo, mantendo a ordem e o tamanho de `responsavelIds`. | obrigatório |
| RF-06 | O nome de usuário inativo deve aparecer normalmente, porque a solicitação é histórico. | obrigatório |
| RF-07 | Os campos novos devem estar disponíveis a qualquer perfil que já pode ler a solicitação, inclusive o operador. | obrigatório |
| RF-08 | Os eventos de tempo real e as respostas de ação não devem trazer os campos novos. | obrigatório |
| RF-09 | `PATCH` da foto da galeria com `principal: true` deve responder sucesso e deixar como capa só a foto escolhida. | obrigatório |
| RF-10 | A troca de capa deve ser indivisível: se gravar a nova capa falhar, a capa anterior continua como estava. | obrigatório |
| RF-11 | O comportamento das demais edições da foto (legenda, `principal: false`) não deve mudar. | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | A listagem não pode consultar nomes linha a linha. | Número de consultas da listagem = o de hoje + no máximo 1, para qualquer tamanho de página (10 ou 100 itens). |
| RNF-02 | Compatibilidade com a versão em produção. | 100% dos campos existentes da resposta mantidos, sem alteração de nome, tipo ou valor. |
| RNF-03 | Capa única por modelo. | Após qualquer troca de capa, 0 ou 1 foto marcada como capa por modelo, nunca 2; e após falha, a contagem é a de antes da chamada. |
| RNF-04 | Cobertura. | ≥ 95% de linha em cada arquivo modificado (Princípio 10). |

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Nome dos responsáveis na solicitação e capa da galeria sem erro

  Cenário: Operador vê os nomes no detalhe
    Dado uma solicitação aberta por "Ana" e com responsáveis "Bruno" e "Carla", nessa ordem
    E um operador autenticado que pode ler a solicitação
    Quando ele consulta o detalhe da solicitação
    Então a resposta traz "responsaveis" com Bruno e Carla, na ordem de "responsavelIds"
    E a resposta traz "abertaPorNome" igual a "Ana"
    Mas ele não precisou consultar a lista de usuários

  Cenário: Campos antigos continuam na resposta
    Dado uma solicitação com um responsável
    Quando alguém a consulta no detalhe ou na listagem
    Então "responsavelIds" e "abertaPorUsuarioId" aparecem com os mesmos valores de antes

  Cenário: Solicitação sem responsável
    Dado uma solicitação ainda não triada, sem responsável
    Quando alguém a consulta
    Então "responsaveis" é uma lista vazia
    E "abertaPorNome" traz o nome de quem a abriu

  Cenário: Responsável inativo continua com nome
    Dado uma solicitação cujo responsável foi inativado depois
    Quando alguém a consulta
    Então o nome dele aparece em "responsaveis"

  Cenário: Listagem traz os nomes sem consulta por linha
    Dado 100 solicitações, cada uma com dois responsáveis
    Quando um gestor lista as solicitações
    Então cada item traz "responsaveis" e "abertaPorNome"
    E o número de consultas é o de hoje mais, no máximo, uma
    Mas o número de consultas não cresce com a quantidade de itens

  Cenário: Respostas de ação e eventos ficam como estão
    Dado uma solicitação existente
    Quando um gestor realiza uma ação sobre ela, como triar ou mover
    Então a resposta da ação não traz "responsaveis" nem "abertaPorNome"
    E o evento de tempo real publicado não traz os campos novos

  Cenário: Definir a segunda foto como capa
    Dado um modelo com duas fotos na galeria e a primeira como capa
    Quando o gestor faz PATCH da segunda foto com "principal" verdadeiro
    Então a resposta é 200
    E só a segunda foto está marcada como capa
    Mas a primeira deixa de ser capa

  Cenário: Falha ao gravar a nova capa desfaz a limpeza
    Dado um modelo com duas fotos e a primeira como capa
    E uma falha ao gravar a segunda como capa
    Quando o gestor faz PATCH da segunda foto com "principal" verdadeiro
    Então a operação falha
    E a primeira foto continua como capa
    Mas o modelo não fica sem capa

  Cenário: Editar a legenda não mexe na capa
    Dado um modelo com a primeira foto como capa
    Quando o gestor altera só a legenda da segunda foto
    Então a primeira foto continua como capa
```

## Ambiguidades

Nenhuma em aberto. As decisões estão na tabela Esclarecimentos, ao fim.

## Métricas de sucesso

- Zero respostas 500 em `PATCH` da galeria com `principal: true` após a publicação.
- O frontend deixa de chamar a lista de 100 usuários para montar quadro e detalhe de solicitação.
- Operadores veem nome de responsável e de quem abriu em 100% das solicitações que podem ler.

## Esclarecimentos

| Pergunta | Resposta | Data |
|---|---|---|
| Se quem abriu não puder ser resolvido, como fica `abertaPorNome`? | Campo presente com valor nulo (RF-02). | 2026-10-09 |
| Se um identificador de responsável não corresponder a usuário algum, o que entra em `responsaveis`? | Item com o identificador e nome nulo, preservando ordem e tamanho (RF-12). | 2026-10-09 |
| Como provar a atomicidade da troca de capa? | Teste de integração com transação real que força falha ao gravar a nova capa e confere que a antiga continua (RF-10). | 2026-10-09 |
