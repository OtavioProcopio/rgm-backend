# Metricas Dashboard Specification

## Purpose

Fornecer KPIs consolidados (totais, distribuicao por status, tempo medio de
resolucao) e uma serie temporal historica de solicitacoes, para alimentar o
dashboard gerencial.

## Requirements

### Requirement: Metricas consolidadas atuais
O sistema SHALL retornar, em uma unica chamada, o total de usuarios, total de
modelos, total de solicitacoes, a contagem por status, quantas estao abertas
(status nao-terminal), quantas pendentes de validacao (EM_VALIDACAO), quantas
concluidas, o tempo medio de resolucao em segundos, e a contagem de
solicitacoes por modelo.

#### Scenario: Consulta de metricas
- **WHEN** um usuario autenticado consulta as metricas
- **THEN** o sistema retorna todos os agregados calculados a partir do estado
  atual do banco

### Requirement: Historico temporal de metricas
O sistema SHALL retornar uma serie temporal (diaria se o periodo pedido for
de ate 30 dias, semanal se maior) com total, abertas, concluidas, canceladas e
SLA medio (horas entre criacao e conclusao) por bucket, alem do SLA medio
global do periodo. O periodo e opcionalmente filtravel por modelo.

#### Scenario: Serie diaria (periodo curto)
- **WHEN** o periodo solicitado e de ate 30 dias
- **THEN** o sistema retorna um ponto por dia no intervalo

#### Scenario: Serie semanal (periodo longo)
- **WHEN** o periodo solicitado e maior que 30 dias
- **THEN** o sistema agrupa os pontos por semana

#### Scenario: Filtro por modelo
- **WHEN** um `modeloId` e informado
- **THEN** apenas solicitacoes daquele modelo entram no calculo da serie

#### Scenario: Bucket sem conclusoes
- **WHEN** um bucket do periodo nao tem nenhuma solicitacao concluida
- **THEN** o SLA medio daquele bucket e reportado como zero, sem erro
