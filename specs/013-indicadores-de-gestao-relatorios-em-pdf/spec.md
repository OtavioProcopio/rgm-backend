# Especificação — Indicadores de gestão, relatórios em PDF fiéis à tela e limpeza do repositório

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.
>
> Origem: issues `rgm-backend#119`, `#120`, `#121` (indicadores) e `#110` (relatórios), mais
> dois pedidos do usuário de 2026-10-11: (a) "cada exportação de PDF é importante, o foco que
> está na tela": a lista acompanha o filtro aplicado e o detalhe do modelo mostra histórico e
> fotos; (b) remover arquivo morto, duplicidade e teste incoerente e organizar o repositório.
> Consumidor: `rgm-frontend#150` (dashboard de gestão). Base: `develop` com as specs 011 e 012
> mergeadas. Sistema interno da fábrica: sem metas de desempenho em escala.

## Problema

1. **O gestor não sabe se o prazo está sendo cumprido (#119).** Só existe a média de resolução,
   sem período, e uma solicitação parada por semanas a distorce. Nada diz quantas ficam fora do
   prazo, qual o caso típico e o pior caso, há quanto tempo o backlog está parado, nem quantas
   abertas ninguém assumiu.
2. **Não se sabe onde o tempo se perde (#120).** Só se mede da abertura à conclusão: não se vê
   se vai na fila, na execução ou na validação, nem quanto trabalho volta para refazer.
3. **Não há corte por máquina nem por pessoa (#121).** O gestor não vê qual máquina concentra
   atrasos, qual placa volta sempre para reparo, nem quem está sobrecarregado.
4. **Os PDFs não dizem o que mostram e podem estar incompletos (#110).**
   - A lista de solicitações em PDF não acompanha a busca e a ordenação da tela, e o PDF não
     informa os filtros usados: quem lê não sabe o que está vendo.
   - A lista de modelos corta em 1000 linhas **sem avisar**; o ranking por modelo e a lista de
     solicitações carregam tudo, sem teto.
   - A ficha do modelo traz eventos e solicitações, mas **não traz as fotos** que a tela de
     detalhe mostra, e consulta as atividades uma solicitação por vez.
5. **O repositório acumulou resto.** Há portas e métodos sem nenhum chamador, arquivos de
   operação duplicados e desatualizados, configuração de editor apontando para o que não existe,
   histórico morto de propostas antigas, testes duplicados, teste sem verificação e teste fora
   do pacote do código que cobre.

**Custo de não resolver:** o dashboard de gestão do frontend fica sem dado, um PDF incompleto
circula como se fosse completo, e cada mudança futura carrega peso morto e testes que não provam
nada.

## Objetivo

- O gestor e o administrador obtêm, por período de 7, 30 ou 90 dias, indicadores de prazo,
  tempo por etapa, retrabalho, máquinas, reincidência e carga por pessoa.
- Cada PDF mostra exatamente o que a tela mostrava ao exportar: mesmos filtros, busca e
  ordenação, com os filtros escritos no cabeçalho e o total de linhas; a ficha do modelo traz o
  histórico e as fotos.
- Nenhum PDF omite linhas sem dizer.
- O repositório não tem código, configuração, documento nem teste sem uso, duplicado ou
  incoerente, e o comportamento observável da API não muda por causa da limpeza.

## Fora de escopo

- Métricas de negócio e de operação no Micrometer (#123), usuário desativado com solicitações
  sem responsável (#104), anexo no mesmo request das ações (#91), refresh token em cookie (#93),
  erros com código estável (#130), busca de usuários na administração (#134), limite de upload
  por tipo (#135) e a dívida da spec 010 (#124).
- PDF de uma solicitação isolada: não existe hoje e não é pedido nesta spec.
- Filtro por tipo na lista de modelos: a tela de modelos não tem esse filtro; o PDF acompanha os
  filtros que a tela tem.
- Comparação com o período anterior nos indicadores novos (o painel da spec 011 já a oferece
  para os seus números).
- Qualquer mudança no frontend: a tela de gestão é a #150, e a tela de tipos de modelo é a #156.
- Renomear em massa os testes antigos para o padrão de nome novo.
- Remover campo ou endpoint que o frontend em produção usa (Princípio 9).

## Personas e cenários de uso

- **Gestor:** abre o dashboard e vê, dos últimos 30 dias, quantas concluídas cumpriram o prazo,
  em que etapa o tempo se perde, qual máquina concentra atraso e quem está sobrecarregado.
- **Gestor exportando:** filtra a lista de solicitações por máquina e atrasadas, busca "correia",
  ordena por prazo e exporta; o PDF traz exatamente aquelas linhas, na mesma ordem, e diz quais
  filtros foram usados.
- **Gestor imprimindo a ficha de um modelo:** quer o histórico e as fotos do modelo, como na tela.
- **Operador:** não vê indicadores agregados (403); exporta a lista do que lhe cabe ver.
- **Quem mantém o sistema:** quer um repositório sem resto, onde cada arquivo e teste tem motivo.

## Requisitos funcionais

### Indicadores de gestão

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | Os indicadores aceitam só os períodos de 7, 30 e 90 dias; outro valor responde 400 com mensagem em português que cita os valores aceitos | obrigatório |
| RF-02 | Os indicadores respondem 403 ao operador e 401 a quem não está autenticado | obrigatório |
| RF-03 | Prazo cumprido: das solicitações concluídas no período (pela data de conclusão) e com prioridade, informa quantas foram concluídas até o prazo da prioridade e a porcentagem, no total e por prioridade | obrigatório |
| RF-04 | Concluída sem prioridade (nunca triada) fica fora da porcentagem de prazo, e a resposta diz quantas ficaram de fora | obrigatório |
| RF-05 | Tempo de resolução: informa a mediana e o percentil 90, em segundos, das concluídas no período; sem concluída no período os valores vêm vazios, não zero | obrigatório |
| RF-06 | Idade do backlog: as solicitações em aberto agora são distribuídas em faixas de menos de 1 dia, de 1 a 3 dias e de mais de 3 dias desde a abertura, e a soma das faixas é o total em aberto | obrigatório |
| RF-07 | A resposta informa quantas solicitações em aberto não têm nenhum responsável | obrigatório |
| RF-08 | Tempo por etapa: informa, em segundos, a mediana e a média da espera até iniciar (abertura até a primeira passagem para Em andamento), da execução (Em andamento até Em validação) e da espera de validação (Em validação até a conclusão ou a devolução), das solicitações que passaram pela etapa no período | obrigatório |
| RF-09 | Solicitação devolvida soma os intervalos de cada passagem pela mesma etapa; etapa sem nenhum dado no período vem vazia, não zero | obrigatório |
| RF-10 | Taxa de devolução: devoluções (Em validação de volta para Em andamento) divididas pelas solicitações enviadas para validação no período, com os dois números na resposta | obrigatório |
| RF-11 | Por máquina: para cada máquina com solicitação no período, informa as abertas agora, as em atraso agora e o tempo médio de resolução das concluídas no período | obrigatório |
| RF-12 | Modelos reincidentes: lista os modelos com 3 ou mais solicitações de reparo abertas no período (o limite de 3 é fixo); solicitação de criação não conta | obrigatório |
| RF-13 | Carga por responsável: lista, com o nome, as solicitações em aberto atribuídas a cada responsável; solicitação com dois responsáveis conta para os dois, e a resposta informa o total de solicitações distintas para a soma não enganar | obrigatório |
| RF-14 | Os indicadores novos são aditivos: nenhum campo ou endpoint existente é removido ou renomeado | obrigatório |

### Relatórios em PDF

| ID | Requisito | Prioridade |
|---|---|---|
| RF-15 | O PDF da lista de solicitações aceita os mesmos filtros, a mesma busca textual e a mesma ordenação da listagem, e contém exatamente as solicitações que a listagem devolve para esses parâmetros, na mesma ordem | obrigatório |
| RF-16 | Todo PDF de lista (solicitações, modelos e ranking por modelo) mostra no cabeçalho os filtros aplicados, escritos de forma legível (ou "sem filtros"), e o total de linhas | obrigatório |
| RF-17 | O PDF da lista de modelos acompanha os filtros da tela de modelos (situação, código, máquina e descrição) e inclui o tipo de cada modelo | obrigatório |
| RF-18 | O PDF do ranking por modelo mostra a ordenação e os filtros que a tela usava | obrigatório |
| RF-19 | A ficha de detalhe do modelo traz o histórico (eventos do modelo e solicitações com suas atividades) e as fotos da galeria, com a capa em destaque, como a tela de detalhe mostra | obrigatório |
| RF-20 | Foto da galeria que não pode ser lida não impede a ficha: o PDF segue sem ela e indica que a foto está indisponível | obrigatório |
| RF-21 | O PDF traz **todas** as linhas que a tela mostraria; o relatório só se limita acima de 5000 linhas, uma rede de segurança de memória, e nesse caso o PDF traz o aviso "exibindo N de M" e orienta refinar os filtros. Nenhum relatório corta em silêncio | obrigatório |
| RF-22 | A ficha do modelo busca as atividades de todas as solicitações do modelo de uma vez, com número fixo de consultas independente da quantidade de solicitações | obrigatório |

### Limpeza do repositório

| ID | Requisito | Prioridade |
|---|---|---|
| RF-23 | O repositório não contém código sem nenhum chamador, operação de persistência sem uso, nem configuração que aponte para algo que não existe | obrigatório |
| RF-24 | Não há arquivo de operação duplicado ou desatualizado em relação ao que o projeto usa de fato, nem documento de proposta antiga arquivado que ninguém referencia | obrigatório |
| RF-25 | Não há teste duplicado, teste sem nenhuma verificação nem teste fora do pacote do código que cobre; a cobertura por arquivo segue em 95% ou mais | obrigatório |
| RF-26 | A documentação e o índice de capacidades descrevem o que existe depois da limpeza | obrigatório |
| RF-27 | A limpeza não altera nenhum endpoint, campo de resposta, regra de negócio nem migração já publicada | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Exatidão dos indicadores | Resultado idêntico a uma conta manual sobre um conjunto conhecido: 100% dos casos dos critérios de aceite |
| RNF-02 | Sem carga de entidades | Nenhum indicador carrega a lista de solicitações em memória: o teste prova que a busca por lista não é chamada |
| RNF-03 | Fidelidade do PDF | Para os mesmos parâmetros, o número de linhas do PDF é igual ao `totalElements` da listagem (ou ao teto, com o aviso) em 100% dos casos testados |
| RNF-04 | Consultas da ficha | O número de consultas ao banco da ficha é o mesmo com 1 e com 50 solicitações; teste de contagem |
| RNF-05 | Cobertura | ≥ 95% de linhas por arquivo tocado; `make validate` com 0 falhas |
| RNF-06 | Compatibilidade (Princípio 9) | 0 campos removidos ou renomeados nas respostas atuais; teste de contrato do formato antigo |
| RNF-07 | Limpeza sem regressão | Suíte antes e depois da limpeza com 0 falhas, e a lista de rotas da API idêntica (teste de rotas existentes) |

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Indicadores de gestão, relatórios em PDF fiéis à tela e limpeza

  Cenário: Concluída um minuto antes do prazo conta como no prazo
    Dado uma solicitação de prioridade ALTA concluída 23 horas e 59 minutos depois de aberta
    E outra da mesma prioridade concluída 24 horas e 1 minuto depois de aberta
    Quando o gestor pede os indicadores dos últimos 30 dias
    Então a primeira conta como no prazo e a segunda como fora
    E a porcentagem de prazo da prioridade ALTA é 50%

  Cenário: Concluída sem prioridade fica fora da taxa
    Dado uma solicitação concluída sem nunca ter sido triada
    Quando o gestor pede os indicadores
    Então ela não entra na porcentagem de prazo
    E a resposta diz que 1 solicitação ficou de fora

  Cenário: Mediana e percentil 90 do tempo de resolução
    Dado dez solicitações concluídas com tempos de 1 a 10 horas
    Quando o gestor pede os indicadores
    Então a mediana é 5,5 horas em segundos
    E o percentil 90 é 9,1 horas em segundos

  Cenário: Sem concluída no período
    Dado nenhuma solicitação concluída nos últimos 7 dias
    Quando o gestor pede os indicadores de 7 dias
    Então mediana, percentil 90 e porcentagem de prazo vêm vazios
    Mas não vêm como zero

  Cenário: Faixas de idade do backlog somam o total em aberto
    Dado solicitações abertas há 2 horas, há 2 dias e há 5 dias
    Quando o gestor pede os indicadores
    Então as faixas trazem 1, 1 e 1
    E a soma é o total em aberto

  Cenário: Abertas sem responsável
    Dado duas solicitações abertas sem responsável e uma com responsável
    Quando o gestor pede os indicadores
    Então a resposta informa 2 abertas sem responsável

  Cenário: Tempo por etapa
    Dado uma solicitação com 2 horas em A Fazer, 5 horas em Em andamento e 1 hora em Em validação
    Quando o gestor pede os indicadores
    Então a espera até iniciar é 2 horas, a execução 5 horas e a espera de validação 1 hora

  Cenário: Devolução soma as passagens e conta uma devolução
    Dado uma solicitação que ficou 3 horas em Em andamento, foi devolvida e ficou mais 2 horas
    Quando o gestor pede os indicadores
    Então a execução dessa solicitação é 5 horas
    E a taxa de devolução conta 1 devolução para 2 envios à validação

  Cenário: Etapa sem dado
    Dado nenhuma solicitação que passou pela validação no período
    Quando o gestor pede os indicadores
    Então a espera de validação vem vazia
    Mas não vem como zero

  Cenário: Indicadores por máquina
    Dado solicitações abertas, em atraso e concluídas em duas máquinas
    Quando o gestor pede os indicadores
    Então cada máquina traz suas abertas, suas em atraso e o tempo médio das concluídas

  Cenário: Modelo reincidente
    Dado um modelo com 3 solicitações de reparo no período e outro com 2
    E uma solicitação de criação
    Quando o gestor pede os indicadores
    Então só o primeiro aparece como reincidente
    Mas a solicitação de criação não conta

  Cenário: Carga por responsável com dois responsáveis
    Dado uma solicitação aberta com dois responsáveis
    Quando o gestor pede os indicadores
    Então ela conta na carga dos dois, com o nome de cada um
    E a resposta informa o total de solicitações distintas

  Cenário: Período inválido
    Quando o gestor pede os indicadores de 15 dias
    Então a resposta é 400
    E a mensagem cita 7, 30 e 90

  Cenário: Operador não vê os indicadores
    Quando um operador pede os indicadores
    Então a resposta é 403

  Cenário: PDF da lista de solicitações acompanha filtros, busca e ordem da tela
    Dado solicitações de máquinas e prioridades variadas
    Quando se exporta o PDF com filtro de máquina, atrasadas, busca "correia" e ordenação por prazo
    Então o PDF tem exatamente as solicitações que a listagem devolve com esses parâmetros
    E na mesma ordem

  Cenário: Cabeçalho dos PDFs de lista mostra os filtros e o total
    Dado uma exportação com filtros aplicados
    Quando o PDF é gerado
    Então o cabeçalho escreve cada filtro de forma legível e o total de linhas
    Mas sem filtros o cabeçalho diz "sem filtros"

  Cenário: PDF da lista de modelos acompanha os filtros da tela
    Dado modelos ativos e inativos de máquinas diferentes
    Quando se exporta com situação ativa e máquina FBOX
    Então o PDF só traz modelos ativos da máquina FBOX
    E mostra o tipo de cada um

  Cenário: Ficha do modelo traz histórico e fotos
    Dado um modelo com eventos, solicitações com atividades e três fotos na galeria, uma delas a capa
    Quando se exporta a ficha do modelo
    Então o PDF traz os eventos, as solicitações com suas atividades e as três fotos
    E a capa aparece em destaque

  Cenário: Foto indisponível não derruba a ficha
    Dado um modelo cuja foto da galeria não pode ser lida
    Quando se exporta a ficha
    Então o PDF é gerado sem a foto e indica que ela está indisponível

  Cenário: Nenhum PDF omite linhas sem dizer
    Dado 5001 linhas que a tela mostraria
    Quando o PDF é exportado
    Então o relatório diz que exibe 5000 de 5001 e orienta refinar os filtros
    Mas não termina em silêncio no corte

  Cenário: Abaixo da rede de segurança o PDF traz tudo
    Dado 1500 linhas que a tela mostraria
    Quando o PDF é exportado
    Então o PDF tem as 1500 linhas
    E não traz aviso de corte

  Cenário: Ficha com número fixo de consultas
    Dado um modelo com 1 solicitação e outro com 50
    Quando se exporta a ficha de cada um
    Então o número de consultas ao banco é o mesmo

  Cenário: Limpeza não muda a API
    Dado a lista de rotas da API antes da limpeza
    Quando a limpeza termina
    Então a lista de rotas é idêntica
    E a suíte de testes passa sem falhas
```

## Ambiguidades

- Nenhuma marca aberta (ver Esclarecimentos).
- Quais filtros e ordenação o ranking por modelo aceita hoje na tela (RF-18) será confirmado
  contra o código no plano, sem mudar o que a spec promete.

## Esclarecimentos

| Pergunta | Resposta | Data |
|---|---|---|
| O PDF deve ter limite de linhas? | Não deveria ter: tem de trazer tudo. Como rede de segurança de memória, só acima de 5000 linhas o PDF corta, e então avisa "exibindo N de M" | 2026-10-11 |
| Valor da rede de segurança | 5000 linhas | 2026-10-11 |

## Métricas de sucesso

- O dashboard de gestão do frontend (#150) consegue ser montado só com os indicadores novos,
  sem cálculo no navegador.
- 0 PDF cujo conteúdo diverge da listagem para os mesmos parâmetros.
- 0 métodos de porta, arquivos de operação ou testes sem uso/duplicados na revisão de
  convergência.
