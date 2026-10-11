# Especificação — Filtros e resumos para a paginação

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.

Origem: decisões do usuário em 2026-10-05, ao esclarecer a feature 005 do frontend
(OtavioProcopio/rgm-frontend#114, "paginação real nas listas"). Não há issue própria no
GitHub. Relacionada: OtavioProcopio/rgm-backend#89, que limita o tamanho de página e só pode
entrar depois que o frontend deixar de pedir listas inteiras.

## Problema

O frontend pede listas inteiras à API porque a API não responde a quatro perguntas de outro
jeito:

1. **"Quais solicitações estão em aberto?"** A listagem filtra um status por vez. Para
   mostrar as solicitações em aberto de um usuário, ou contar as em aberto por prioridade,
   a tela busca tudo e filtra.
2. **"Quais solicitações foram canceladas neste período?"** O filtro por data de
   encerramento olha só a data de conclusão. Uma solicitação cancelada nunca aparece nele.
3. **"Quantos modelos existem, por situação e por máquina?"** O painel de modelos busca
   todos os modelos e conta na tela.
4. **"Como está o histórico de solicitações deste modelo?"** A ficha do modelo busca todas
   as solicitações dele e calcula totais e tempos na tela.

Enquanto essas perguntas não tiverem resposta direta, o limite de tamanho de página
(rgm-backend#89) quebraria o quadro, a aba pessoal, o painel e a ficha do modelo.

## Objetivo

A API responde às quatro perguntas diretamente, com filtros e resumos, para que nenhuma
tela precise trazer uma lista inteira.

## Fora de escopo

- Limitar o tamanho de página (rgm-backend#89): entra depois da feature 005 do frontend.
- Busca de usuário por nome.
- Mudar o que as listagens devolvem quando os filtros novos não são usados.
- Mudar o cálculo do ranking de modelos por tempo, que já existe.
- Mudar o restante da ficha em PDF do modelo; só os dois tempos dela mudam (RF-12).

## Personas e cenários de uso

- **Operador** abre a aba pessoal e vê, paginadas, as solicitações em aberto que abriu e as
  que estão com ele.
- **Gestor** abre o quadro e vê, na coluna Cancelada, as canceladas nos últimos 30 dias.
- **Gestor** abre o painel e vê quantas solicitações em aberto há por prioridade e quantos
  modelos há por máquina.
- **Qualquer usuário com acesso a modelos** abre a ficha de um modelo e vê o resumo do
  histórico dele.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | A listagem de solicitações deve aceitar um filtro "em aberto" que, ligado, devolve só solicitações em A Fazer, Em Andamento ou Em Validação | obrigatório |
| RF-02 | O filtro "em aberto" deve combinar com todos os filtros que a listagem já tem (quem abriu, responsável, prioridade, modelo, tipo, máquina, datas, atrasada) e com a paginação | obrigatório |
| RF-03 | O filtro "em aberto" desligado ou ausente não deve mudar o resultado da listagem | obrigatório |
| RF-04 | O filtro por data de encerramento deve considerar a data de conclusão das concluídas e a data de cancelamento das canceladas | obrigatório |
| RF-05 | O sistema deve oferecer um resumo de modelos com: total, ativos, inativos, com pendência aberta e a quantidade por máquina | obrigatório |
| RF-06 | O sistema deve oferecer um resumo das solicitações de um modelo com: total, em aberto, concluídas, canceladas, tempo médio de resolução e intervalo médio entre solicitações | obrigatório |
| RF-07 | O resumo de um modelo que não existe deve responder "não encontrado" | obrigatório |
| RF-08 | Os dois resumos devem exigir as mesmas permissões da consulta de modelos | obrigatório |
| RF-10 | O tempo médio de resolução do resumo de um modelo deve ser a média das solicitações concluídas dele, informado a partir de 1 concluída; sem concluída, vem vazio | obrigatório |
| RF-11 | O intervalo médio entre solicitações do resumo de um modelo deve considerar a data de abertura de todas as solicitações dele, em qualquer status, informado a partir de 2 solicitações; com menos de 2, vem vazio | obrigatório |
| RF-12 | A ficha em PDF do modelo deve mostrar os mesmos dois tempos do resumo (RF-10 e RF-11) | obrigatório |
| RF-09 | O relatório em PDF de solicitações, que usa os mesmos filtros da listagem, deve aceitar o filtro "em aberto" e seguir a regra de RF-04 | desejável |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Custo dos resumos | no máximo 3 consultas ao banco por chamada de resumo, sem carregar a lista de modelos ou de solicitações para a memória |
| RNF-02 | Compatibilidade do contrato | 0 campos removidos ou renomeados; os filtros novos são opcionais |
| RNF-03 | Cobertura de testes dos arquivos alterados | no mínimo 95% de linha em cada arquivo modificado que entra na medição |

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Filtros e resumos para a paginação

  Cenário: Só as em aberto
    Dado que existem solicitações em "A Fazer", "Em Andamento", "Em Validação", "Concluída" e "Cancelada"
    Quando listo as solicitações com o filtro "em aberto" ligado
    Então recebo só as de "A Fazer", "Em Andamento" e "Em Validação"
    E o total informado conta só essas

  Cenário: Em aberto de quem abriu, paginado
    Dado que um usuário abriu 25 solicitações que continuam em aberto e 40 já encerradas
    Quando listo as solicitações abertas por ele, em aberto, 10 por página
    Então recebo 10 solicitações
    E o total informado é 25

  Cenário: Em aberto por prioridade
    Dado que existem 3 solicitações em aberto com prioridade "Alta" e 2 encerradas com prioridade "Alta"
    Quando listo as solicitações em aberto com prioridade "Alta"
    Então o total informado é 3

  Cenário: Sem o filtro nada muda
    Dado que existem solicitações em todos os status
    Quando listo as solicitações sem o filtro "em aberto"
    Então recebo solicitações de todos os status

  Cenário: Canceladas por data de encerramento
    Dado que uma solicitação foi cancelada há 10 dias e outra há 60 dias
    Quando listo as canceladas com data de encerramento nos últimos 30 dias
    Então recebo só a cancelada há 10 dias

  Cenário: Concluídas por data de encerramento continuam valendo
    Dado que uma solicitação foi concluída há 10 dias e outra há 60 dias
    Quando listo as concluídas com data de encerramento nos últimos 30 dias
    Então recebo só a concluída há 10 dias

  Cenário: Resumo de modelos
    Dado que existem 5 modelos ativos e 2 inativos
    E 3 deles estão na máquina "FBOX" e 4 na máquina "DISA"
    E 2 deles têm pendência aberta
    Quando consulto o resumo de modelos
    Então recebo total 7, ativos 5, inativos 2 e com pendência 2
    E recebo "DISA" com 4 e "FBOX" com 3, da maior para a menor quantidade

  Cenário: Resumo das solicitações de um modelo
    Dado que um modelo tem 2 solicitações em aberto, 3 concluídas e 1 cancelada
    Quando consulto o resumo das solicitações desse modelo
    Então recebo total 6, em aberto 2, concluídas 3 e canceladas 1
    E recebo o tempo médio de resolução das concluídas

  Cenário: Tempos do resumo seguem a regra do ranking
    Dado que um modelo tem solicitações abertas nos dias 1, 11, 21 e 31
    E a do dia 1 foi concluída em 2 dias e a do dia 21 em 4 dias
    E a do dia 11 foi cancelada e a do dia 31 continua em aberto
    Quando consulto o resumo das solicitações desse modelo
    Então o tempo médio de resolução é de 3 dias
    E o intervalo médio entre solicitações é de 10 dias

  Cenário: Uma única solicitação concluída
    Dado que um modelo tem uma única solicitação, concluída em 2 dias
    Quando consulto o resumo das solicitações desse modelo
    Então o tempo médio de resolução é de 2 dias
    E o intervalo médio entre solicitações vem vazio

  Cenário: Modelo sem solicitações concluídas
    Dado que um modelo só tem solicitações em aberto
    Quando consulto o resumo das solicitações desse modelo
    Então o tempo médio de resolução vem vazio

  Cenário: Ficha em PDF usa os tempos do resumo
    Dado que um modelo tem uma única solicitação concluída
    Quando gero a ficha em PDF desse modelo
    Então a ficha mostra o tempo médio de resolução dessa solicitação

  Cenário: Modelo que não existe
    Quando consulto o resumo das solicitações de um modelo que não existe
    Então recebo "não encontrado"

  Cenário: Sem autenticação
    Quando consulto um dos resumos sem estar autenticado
    Então o acesso é recusado
```

## Ambiguidades

Nenhuma em aberto.

Decisões registradas em 2026-10-05:

- **"Em aberto"** são os três status não encerrados: A Fazer, Em Andamento e Em Validação.
- **"Em aberto" junto com um status:** os dois filtros se somam. Pedir "em aberto" e
  "Concluída" ao mesmo tempo devolve lista vazia.
- **Com pendência aberta e por máquina** contam modelos ativos e inativos, como a tela conta
  hoje.
- **Ordem do "por máquina":** da maior para a menor quantidade; empate em ordem alfabética.
- **Endpoints agregados em vez de paginar na tela:** resposta do usuário em 2026-10-05.

Decisões registradas em 2026-10-06:

- **Tempos no resumo do modelo:** a ficha do modelo calculava os dois tempos só com as
  solicitações concluídas e só com 2 ou mais; o ranking de modelos calcula o tempo de
  resolução com 1 concluída ou mais e o intervalo com todas as solicitações do modelo.
  Resposta do usuário: o resumo segue a regra do ranking (RF-10 e RF-11). Os números da
  ficha mudam em relação à v1.5.0: o intervalo tende a diminuir e modelo com uma única
  concluída passa a mostrar tempo de resolução.
- **Ficha em PDF:** passa a usar os mesmos tempos do resumo (RF-12), para a tela e o PDF do
  mesmo modelo não divergirem. Isso substitui a decisão do OpenSpec
  `metricas-tempo-por-modelo`, que mantinha a ficha com critério próprio. Inferência do
  agente a partir do motivo da escolha (mesmo número em ficha, ranking e PDF); não foi
  perguntada em separado.

## Métricas de sucesso

- A feature 005 do frontend deixa de pedir qualquer página com mais de 100 itens.
- A rgm-backend#89 pode limitar o tamanho de página sem quebrar nenhuma tela.
