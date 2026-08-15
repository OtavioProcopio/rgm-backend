# Admin Exclusao Specification

## Purpose

Permitir que um ADMINISTRADOR realize exclusao fisica (hard delete) de
Solicitacoes, Modelos ou Usuarios, para correcao de erros de cadastro — em
contraste com desativacao, que e o caminho normal para "remover" um registro
sem perder historico. Exclusao e deliberadamente restrita e bloqueada quando
haveria perda de historico relevante.

## Requirements

### Requirement: Somente ADMINISTRADOR exclui fisicamente
O sistema SHALL restringir a exclusao fisica de qualquer tipo de registro ao
perfil ADMINISTRADOR.

#### Scenario: Usuario sem permissao tenta excluir
- **WHEN** um usuario que nao seja ADMINISTRADOR tenta excluir um registro
- **THEN** o sistema rejeita com erro de nao autorizado

### Requirement: Excluir solicitacao
O sistema SHALL permitir excluir fisicamente uma solicitacao, removendo em
cascata suas evidencias (registro + arquivo no storage), atribuicoes e
atividades, e recalculando a pendencia do modelo vinculado se a solicitacao
excluida estava em status nao-terminal.

#### Scenario: Exclusao de solicitacao
- **WHEN** um ADMINISTRADOR exclui uma solicitacao existente
- **THEN** a solicitacao, suas evidencias, atribuicoes e atividades sao
  removidas, e a pendencia do modelo e recalculada se necessario

### Requirement: Excluir modelo
O sistema SHALL permitir excluir fisicamente um modelo apenas se ele nao
tiver nenhuma solicitacao vinculada (nem historico), removendo tambem os
arquivos de sua galeria de fotos do storage.

#### Scenario: Modelo sem solicitacoes vinculadas
- **WHEN** o modelo nao possui nenhuma solicitacao vinculada (nem historico)
- **THEN** o modelo e sua galeria de fotos sao excluidos, incluindo os
  arquivos no storage

#### Scenario: Modelo com solicitacoes vinculadas
- **WHEN** o modelo possui ao menos uma solicitacao vinculada (mesmo
  encerrada)
- **THEN** o sistema rejeita a exclusao, orientando a desativar o modelo em
  vez de exclui-lo

### Requirement: Excluir usuario
O sistema SHALL permitir excluir fisicamente um usuario apenas se ele nao
tiver nenhum historico associado (solicitacoes abertas por ele, atribuicoes
ativas, atividades registradas ou eventos de modelo executados por ele). Um
ADMINISTRADOR nunca pode excluir a propria conta.

#### Scenario: Usuario sem historico
- **WHEN** o usuario nao tem nenhum vinculo historico no sistema
- **THEN** o usuario e excluido fisicamente

#### Scenario: Usuario com historico
- **WHEN** o usuario tem qualquer historico (solicitacoes, atribuicoes,
  atividades ou eventos de modelo)
- **THEN** o sistema rejeita a exclusao, orientando a desativar o usuario em
  vez de exclui-lo

#### Scenario: Administrador tenta excluir a propria conta
- **WHEN** o `recursoId` da exclusao de usuario e igual ao id do
  administrador autenticado
- **THEN** o sistema rejeita "Nao e possivel excluir a propria conta de
  administrador"
