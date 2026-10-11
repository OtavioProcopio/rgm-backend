# Especificação — Painel de métricas por período e ordenação da listagem

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.
>
> Origem: issues `rgm-backend#117`, `#108`, `#118` e `#107`, com consumidores
> `rgm-frontend#149` (dashboard) e `#142` (quadro). Base: `develop` com a spec 010 já mergeada
> (indicadores restritos a gestor e administrador). Decisões do usuário de 2026-10-10 estão
> registradas em **Esclarecimentos**. Os comportamentos "hoje" foram lidos no código de
> `develop` (`a793048`).

## Problema

1. **O painel não sabe comparar períodos (#117).** `GET /api/solicitacoes/metricas` é global,
   sem período. O histórico (`/metricas/historico`) agrupa por **data de criação**, então uma
   solicitação criada há 20 dias e concluída hoje não conta como concluída hoje. O frontend
   contorna com listagens de até 100 itens e média no navegador, o que erra quando passa de 100.
2. **O histórico erra o dia, o tempo e o volume (#108).** Os dias são montados em UTC: uma
   solicitação aberta às 22h de Brasília cai no dia seguinte. A média de resolução é dividida por
   3600 com inteiros: 30 minutos viram "0 h". `dias` não tem teto e todas as solicitações do
   período são carregadas em memória; o filtro de modelo é aplicado depois, em memória.
3. **A listagem não ordena (#118).** `GET /api/solicitacoes` devolve sempre da mais antiga para
   a mais nova. Com 20 por coluna, as recém-abertas ficam atrás do "Carregar mais" (#142), e a
   fila "o que precisa de mim agora" só consegue ordenar por atraso entre as 100 primeiras.
4. **O tempo de criar um modelo distorce o ranking (#107).** Ao concluir uma solicitação de
   criação, ela passa a apontar para o modelo recém-criado e entra no tempo médio de resolução e
   no intervalo entre solicitações, que medem manutenção, não criação.

**Custo de não resolver:** o gestor lê um "concluídas" que não é o de hoje, vê "0 h" para
reparos de 30 minutos, o dia da noite aparece no dia errado, e o front continua carregando
listas e calculando no navegador, com número errado acima de 100.

## Objetivo

- O gestor e o administrador obtêm, em **uma requisição**, os indicadores de um período e do
  período anterior de mesmo tamanho, com concluídas contadas no dia em que foram concluídas e o
  tempo de resolução em segundos.
- A listagem de solicitações pode ser ordenada por prazo, criação ou prioridade, sem depender do
  tamanho da página.
- Os dias das séries seguem o fuso da fábrica, e o período é limitado.
- Solicitação de criação deixa de entrar nos tempos do modelo.
- Nada que o frontend em produção (v1.5.0) usa hoje deixa de funcionar.

## Fora de escopo

- Indicadores de gestão novos: % no prazo, mediana e p90, idade do backlog, abertas sem
  responsável (#119); tempo por etapa e taxa de devolução (#120); por máquina, reincidentes e
  carga por responsável (#121); métricas do Micrometer (#123).
- Bugs pequenos do catálogo e das ações (#111, #102, #103, #100, #106, #105) e usuário
  desativado (#104).
- Qualquer mudança no frontend: o consumo do painel é a #149.
- Mudar o que conta como "em atraso" (SLA vencido, solicitação em aberto), definição que o
  backend já usa.
- Remover ou renomear campos e endpoints que o frontend v1.5.0 usa.

## Personas e cenários de uso

- **Gestor:** abre o dashboard e compara os últimos 30 dias com os 30 anteriores; precisa de
  "concluídas" no dia em que aconteceram e do tempo médio em minutos ou horas com leitura.
- **Gestor no quadro:** quer ver primeiro as solicitações recém-abertas em A Fazer e as mais
  atrasadas em Em Andamento.
- **Administrador:** mesmos indicadores, e ranking por modelo sem o ruído de pedidos de criação.
- **Operador:** não vê indicadores agregados (403, regra da spec 010); pode ordenar a listagem
  das próprias solicitações.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | A listagem de solicitações aceita um critério de ordenação (`sort`: criação, prazo limite ou prioridade) e um sentido (`dir`: crescente ou decrescente) | obrigatório |
| RF-02 | Valor de `sort` ou `dir` fora da lista fechada responde 400, com mensagem em português que nomeia o parâmetro e os valores aceitos | obrigatório |
| RF-03 | Ordenar por prazo limite coloca a solicitação mais atrasada primeiro (sentido crescente); solicitação sem prioridade (não triada) vai sempre para o fim | obrigatório |
| RF-04 | Sem `sort`, a ordem é a de hoje (criação crescente), e a paginação continua estável | obrigatório |
| RF-05 | Existe uma consulta de painel por período para gestor e administrador que devolve, na mesma resposta, os indicadores do período pedido e do período anterior de mesmo tamanho | obrigatório |
| RF-06 | O painel conta solicitações concluídas pela **data de conclusão** e solicitações criadas pela **data de criação**, cada uma no seu dia (por semana quando o período passa de 30 dias) | obrigatório |
| RF-07 | O painel informa o tempo médio de resolução em **segundos**; tempo abaixo de 1 hora não vira zero | obrigatório |
| RF-08 | O painel e o histórico agrupam os dias no fuso da fábrica (`America/Sao_Paulo`, configurável): solicitação aberta às 22h aparece no dia em que foi aberta | obrigatório |
| RF-09 | O painel aceita só os períodos de 7, 30 e 90 dias; o histórico existente aceita de 1 a 365 dias; valor fora da faixa responde 400 com mensagem em português | obrigatório |
| RF-10 | O painel informa abertas e em atraso agora e, em série, abertas e em atraso ao fim de cada dia do período, usando a prioridade que a solicitação tem hoje | obrigatório |
| RF-11 | O painel e o histórico aceitam filtro por modelo aplicado na consulta, não em memória depois de carregar tudo | obrigatório |
| RF-12 | O painel responde 403 ao operador e 401 a quem não está autenticado | obrigatório |
| RF-13 | Os campos e endpoints de métricas que o frontend v1.5.0 usa continuam respondendo com o mesmo formato (`slaMediaHoras` e `slaGlobalMediaHoras` mantidos); o novo é aditivo | obrigatório |
| RF-14 | Solicitação do tipo criação deixa de entrar no tempo médio de resolução e no intervalo entre solicitações do modelo (ranking por modelo e resumo do modelo); continua contando no total de solicitações do modelo | obrigatório |
| RF-15 | O histórico existente passa a respeitar o fuso (RF-08), o limite de período (RF-09) e a agregação na consulta (RF-11), sem mudar o formato da resposta | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Tempo de resposta do painel | p95 ≤ 500 ms com 10 000 solicitações no período, medido em servidor real |
| RNF-02 | Consumo de memória do painel | Zero carregamento de entidade de solicitação por requisição: o caso de uso do painel não lista solicitações; teste prova que nenhuma busca por lista é chamada |
| RNF-03 | Exatidão | Resultado idêntico a uma conta manual sobre um conjunto conhecido: 100% dos casos do critério de aceite |
| RNF-04 | Cobertura | ≥ 95% de linhas por arquivo tocado (Princípio 10); `make validate` com 0 falhas |
| RNF-05 | Compatibilidade (Princípio 9) | 0 campos removidos ou renomeados nas respostas atuais; teste de contrato do formato antigo |
| RNF-06 | Estabilidade da paginação ordenada | Com `sort` e páginas consecutivas, 0 itens repetidos ou perdidos entre páginas num conjunto sem alteração |

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Painel de métricas por período e ordenação da listagem

  Cenário: Ordenar por prazo limite, mais atrasada primeiro
    Dado que existem solicitações em aberto com prioridades e datas de criação diferentes
    E uma solicitação ainda não triada
    Quando o gestor lista com sort=prazoLimite e dir=asc
    Então a solicitação com o prazo mais vencido vem primeiro
    E a solicitação sem prioridade vem por último
    Mas a ordem não depende do tamanho da página

  Cenário: Ordenar por criação, mais nova primeiro
    Dado que existem solicitações criadas em dias diferentes
    Quando se lista com sort=criadaEm e dir=desc
    Então a mais recente vem primeiro

  Cenário: Sem sort a ordem é a de hoje
    Dado que existem solicitações criadas em dias diferentes
    Quando se lista sem sort
    Então a mais antiga vem primeiro

  Cenário: Critério de ordenação inválido
    Quando se lista com sort=titulo
    Então a resposta é 400
    E a mensagem nomeia o parâmetro sort e os valores aceitos
    Mas nenhuma lista é devolvida

  Cenário: Concluída conta no dia da conclusão
    Dado uma solicitação criada há 20 dias e concluída ontem
    Quando o gestor pede o painel dos últimos 7 dias
    Então ela conta como concluída no dia de ontem
    E não conta como criada nos últimos 7 dias

  Cenário: Período anterior na mesma resposta
    Dado solicitações concluídas nos últimos 30 dias e nos 30 dias anteriores
    Quando o gestor pede o painel de 30 dias
    Então a resposta traz os mesmos indicadores para o período atual e para o anterior
    E o período anterior tem 30 dias de tamanho e termina onde o atual começa

  Cenário: Tempo de resolução curto em segundos
    Dado uma solicitação concluída 30 minutos depois de criada
    Quando o gestor pede o painel
    Então o tempo médio de resolução é 1800 segundos
    Mas não aparece como zero

  Cenário: Dia no fuso da fábrica
    Dado uma solicitação criada às 22h de Brasília de uma terça-feira
    Quando o gestor pede o painel ou o histórico
    Então ela aparece no dia da terça-feira
    Mas não na quarta-feira

  Cenário: Período fora da faixa do painel
    Quando o gestor pede o painel com 15 dias
    Então a resposta é 400 com mensagem em português que lista 7, 30 e 90

  Cenário: Período do histórico acima do teto
    Quando o gestor pede o histórico com 100000 dias
    Então a resposta é 400 com mensagem em português

  Cenário: Operador não vê o painel
    Dado que estou autenticado como operador
    Quando peço o painel por período
    Então a resposta é 403
    E o corpo está no formato de erro da API

  Cenário: Formato antigo preservado
    Dado o frontend v1.5.0 em produção
    Quando ele pede /metricas e /metricas/historico como antes
    Então recebe os mesmos campos de antes, inclusive slaMediaHoras e slaGlobalMediaHoras

  Cenário: Criação fora dos tempos do modelo
    Dado um modelo criado por uma solicitação de criação concluída e sem nenhuma manutenção
    Quando o gestor consulta o ranking e o resumo do modelo
    Então o modelo não aparece no ranking por tempo
    E o resumo mostra o tempo médio e o intervalo vazios
    Mas a solicitação de criação continua contada no total de solicitações do modelo

  Cenário: Filtro por modelo na consulta
    Dado solicitações de dois modelos no período
    Quando o gestor pede o painel filtrado por um modelo
    Então só entram solicitações desse modelo
```

## Ambiguidades

Nenhuma ambiguidade aberta (ver Esclarecimentos).

## Esclarecimentos

| Pergunta | Resposta | Data |
|---|---|---|
| Ordenação da listagem: pela API ou só no front? (#142) | Pela API, com `sort` e `dir`; ordem padrão atual preservada | 2026-10-10 |
| A criação conta no total do modelo? (#107) | Sim; sai só das consultas de tempo | 2026-10-10 |
| Versão alvo | v1.6.0, aditiva e compatível com o front v1.5.0 (Princípio 9) | 2026-10-10 |
| Quais períodos o painel aceita? (RF-05, RF-09) | O painel aceita só 7, 30 e 90 dias; o histórico existente segue de 1 a 365 | 2026-10-10 |
| Prioridade usada em "em atraso ao fim de cada dia" (RF-10) | A prioridade que a solicitação tem hoje (aproximação aceita, sem reconstruir pelo histórico); abertas ao fim do dia saem das datas de criação, conclusão e cancelamento | 2026-10-10 |
| Onde entra o painel? (RF-05) | Endpoint novo, ao lado dos atuais; o histórico existente só ganha fuso, limite e agregação na consulta | 2026-10-10 |
| Meta de tempo do painel (RNF-01) | p95 ≤ 500 ms com 10 000 solicitações no período | 2026-10-10 |
| Revisão do checklist `requisitos.md` (6 lacunas) | Aceitas como no plano: prioridade crescente = mais urgente primeiro (URGENTE, ALTA, MEDIA, BAIXA), sem prioridade sempre no fim; semana de segunda a domingo; "abertas agora" e "em atraso agora" só no período atual (o anterior traz apenas a série ao fim de cada dia); formato da resposta do painel definido no plano e no UC-16 | 2026-10-10 |

## Métricas de sucesso

- O dashboard (#149) deixa de usar a amostra de 100 itens e a "média de 100 de N" some.
- A aba Solicitações do dashboard faz menos requisições que na spec 013.
- O quadro mostra a solicitação recém-aberta no primeiro bloco de A Fazer (#142).
- Em produção, nenhum aumento de 5xx nos endpoints de métricas depois da publicação.
