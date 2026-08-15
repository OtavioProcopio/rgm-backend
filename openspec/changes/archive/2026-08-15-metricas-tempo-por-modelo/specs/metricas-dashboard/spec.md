## ADDED Requirements

### Requirement: Ranking de metricas por modelo
O sistema SHALL retornar, por modelo, o tempo medio de resolucao (segundos,
media de `concluidaEm - criadaEm` entre as solicitacoes CONCLUIDA daquele
modelo) e o intervalo medio entre solicitacoes (segundos, media dos
intervalos entre `criadaEm` de solicitacoes consecutivas daquele modelo,
ordenadas cronologicamente), paginado e ordenavel por qualquer uma das duas
metricas (ascendente ou descendente). O calculo e feito sob demanda a partir
das solicitacoes existentes — nenhum valor e persistido.

#### Scenario: Modelo com dados suficientes para as duas metricas
- **WHEN** um modelo tem 2 ou mais solicitacoes concluidas
- **THEN** o sistema retorna tanto o tempo medio de resolucao quanto o
  intervalo medio entre solicitacoes daquele modelo

#### Scenario: Modelo com apenas 1 solicitacao concluida
- **WHEN** um modelo tem exatamente 1 solicitacao concluida
- **THEN** o sistema retorna o tempo medio de resolucao (baseado nessa unica
  solicitacao), mas o intervalo medio entre solicitacoes fica ausente/nulo
  (nao ha um segundo ponto para calcular intervalo)

#### Scenario: Modelo sem nenhuma solicitacao concluida
- **WHEN** um modelo nao tem nenhuma solicitacao concluida (mesmo que tenha
  solicitacoes em aberto ou canceladas)
- **THEN** esse modelo nao aparece no ranking

#### Scenario: Ordenacao por tempo medio de resolucao
- **WHEN** o cliente solicita ordenacao por tempo medio de resolucao
  descendente
- **THEN** os modelos mais demorados para resolver aparecem primeiro

#### Scenario: Ordenacao por intervalo medio entre solicitacoes
- **WHEN** o cliente solicita ordenacao por intervalo medio entre
  solicitacoes ascendente
- **THEN** os modelos que mais frequentemente voltam a precisar de
  manutencao (menor intervalo) aparecem primeiro

#### Scenario: Paginacao
- **WHEN** o cliente solicita uma pagina especifica com um tamanho de pagina
- **THEN** o sistema retorna apenas os modelos daquela pagina, respeitando a
  ordenacao solicitada

### Requirement: Exportar ranking de modelos por tempo em PDF
O sistema SHALL gerar um PDF tabular com o ranking de modelos por tempo
(tempo medio de resolucao e intervalo medio entre solicitacoes), respeitando
a ordenacao solicitada.

#### Scenario: Exportar ranking
- **WHEN** um usuario autenticado solicita a exportacao do ranking com uma
  ordenacao especifica
- **THEN** o sistema retorna um PDF com os modelos naquela ordem
