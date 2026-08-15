# Eventos Modelo Specification

## Purpose

Manter um historico cronologico (prontuario) de intervencoes fisicas
concluidas em um Modelo. Diferente da galeria (fotos de apresentacao) e das
evidencias (anexos de solicitacao), os eventos registram formalmente QUANDO e
O QUE foi feito no modelo. Hoje o unico gatilho de criacao e a conclusao
bem-sucedida de uma Solicitacao — nao ha cadastro manual de evento.

## Requirements

### Requirement: Evento gerado automaticamente ao concluir solicitacao
O sistema SHALL registrar automaticamente um evento no historico do modelo
sempre que uma Solicitacao vinculada a ele for concluida com sucesso (nunca ao
ser cancelada).

#### Scenario: Solicitacao de REPARO concluida
- **WHEN** uma solicitacao de tipo REPARO vinculada a um modelo e concluida
- **THEN** um evento de tipo REPARO e criado, com titulo igual ao titulo da
  solicitacao, descricao igual ao comentario final, autor o usuario que
  encerrou, e referencia a solicitacao de origem

#### Scenario: Solicitacao de INSPECAO concluida
- **WHEN** uma solicitacao de tipo INSPECAO e concluida
- **THEN** o evento criado e do tipo INSPECAO

#### Scenario: Solicitacao de REENGENHARIA concluida
- **WHEN** uma solicitacao de tipo REENGENHARIA e concluida
- **THEN** o evento criado e do tipo MODIFICACAO

#### Scenario: Solicitacao cancelada nao gera evento
- **WHEN** uma solicitacao e cancelada (em vez de concluida)
- **THEN** nenhum evento de modelo e criado

#### Scenario: Solicitacao sem modelo vinculado
- **WHEN** uma solicitacao concluida nao possui `modeloId` (nao deveria
  ocorrer no fluxo normal, mas e tolerado defensivamente)
- **THEN** nenhum evento e criado

### Requirement: Consultar historico de eventos de um modelo
O sistema SHALL permitir que qualquer usuario autenticado liste o historico
completo de eventos de um modelo existente.

#### Scenario: Listagem
- **WHEN** um usuario autenticado consulta os eventos de um modelo existente
- **THEN** o sistema retorna todos os eventos registrados para aquele modelo

#### Scenario: Modelo inexistente
- **WHEN** o modelo informado nao existe
- **THEN** o sistema retorna erro de recurso nao encontrado
