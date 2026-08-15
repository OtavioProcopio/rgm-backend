## MODIFIED Requirements

### Requirement: Exportar relatorios em PDF
O sistema SHALL gerar um PDF com a lista de modelos filtrada, e um PDF com a
ficha completa de um modelo especifico (dados + eventos + solicitacoes
vinculadas). Quando o modelo tiver ao menos 2 solicitacoes concluidas, a
ficha SHALL incluir tambem o tempo medio de resolucao e o intervalo medio
entre solicitacoes daquele modelo (mesmo calculo da capacidade
`metricas-dashboard`).

#### Scenario: Exportar lista filtrada
- **WHEN** um usuario autenticado solicita a exportacao da lista com os
  mesmos filtros aplicados na tela
- **THEN** o sistema retorna um PDF tabular com os modelos filtrados

#### Scenario: Exportar ficha de um modelo
- **WHEN** um usuario autenticado solicita a ficha de um modelo existente
- **THEN** o sistema retorna um PDF com os dados do modelo, seu historico de
  eventos e as solicitacoes vinculadas

#### Scenario: Ficha com metricas de tempo
- **WHEN** o modelo tem 2 ou mais solicitacoes concluidas
- **THEN** a ficha PDF inclui o tempo medio de resolucao e o intervalo medio
  entre solicitacoes daquele modelo

#### Scenario: Ficha sem dados suficientes para metricas
- **WHEN** o modelo tem menos de 2 solicitacoes concluidas
- **THEN** a ficha PDF omite as metricas de tempo, sem erro
