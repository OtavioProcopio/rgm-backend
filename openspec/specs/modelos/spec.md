# Modelos Specification

## Purpose

Um Modelo representa uma maquina/encaixe cadastrado no sistema, alvo das
Solicitacoes de manutencao. GESTOR e ADMINISTRADOR cadastram, editam,
ativam/desativam e consultam modelos; qualquer usuario autenticado pode
listar/consultar. O sistema mantem automaticamente uma flag
`temPendenciaAberta` sincronizada com o estado das Solicitacoes vinculadas.

## Requirements

### Requirement: Somente GESTOR/ADMINISTRADOR gerencia modelos
O sistema SHALL restringir criacao, edicao, ativacao e desativacao de modelos
aos perfis GESTOR e ADMINISTRADOR.

#### Scenario: OPERADOR ou EXTERNO tenta gerenciar modelo
- **WHEN** um usuario OPERADOR ou EXTERNO tenta criar, editar, ativar ou
  desativar um modelo
- **THEN** o sistema rejeita com "Perfil sem permissao para gerenciar
  modelos"

### Requirement: Cadastrar modelo
O sistema SHALL permitir que GESTOR/ADMINISTRADOR cadastre um modelo com
codigo, descricao, observacoes opcionais e uma maquina valida do catalogo. A
versao do modelo e calculada automaticamente como
`quantidade de modelos existentes com o mesmo codigo+maquina + 1`.

#### Scenario: Primeiro cadastro de um codigo+maquina
- **WHEN** nao existe nenhum modelo com o mesmo codigo e maquina
- **THEN** o modelo e criado com `versao = 1`, ativo, sem pendencia aberta

#### Scenario: Recadastro do mesmo codigo+maquina
- **WHEN** ja existem N modelos com o mesmo codigo e maquina (ex.: revisoes
  anteriores)
- **THEN** o novo modelo e criado com `versao = N + 1`

#### Scenario: Maquina invalida
- **WHEN** a maquina informada nao existe ou esta inativa no catalogo
- **THEN** o sistema rejeita a criacao

### Requirement: Editar modelo
O sistema SHALL permitir que GESTOR/ADMINISTRADOR edite codigo, descricao,
observacoes e maquina de um modelo existente, revalidando a maquina.

#### Scenario: Edicao bem-sucedida
- **WHEN** o modelo existe e a nova maquina e valida
- **THEN** os campos sao atualizados e `atualizadoEm` e renovado

### Requirement: Ativar e desativar modelo
O sistema SHALL permitir alternar o estado ativo de um modelo. Modelos
inativos nao devem ser usados para abrir novas solicitacoes.

#### Scenario: Desativar modelo
- **WHEN** um GESTOR/ADMINISTRADOR desativa um modelo ativo
- **THEN** `ativo` passa a `false`

#### Scenario: Reativar modelo
- **WHEN** um GESTOR/ADMINISTRADOR reativa um modelo inativo
- **THEN** `ativo` passa a `true`

### Requirement: Pendencia aberta sincronizada com Solicitacoes
O sistema SHALL manter `temPendenciaAberta` do modelo verdadeiro sempre e
somente quando existir ao menos uma Solicitacao vinculada em status
nao-terminal (A_FAZER, EM_ANDAMENTO ou EM_VALIDACAO).

#### Scenario: Abertura de solicitacao gera pendencia
- **WHEN** a primeira solicitacao nao-terminal e criada para um modelo sem
  pendencia
- **THEN** `temPendenciaAberta` passa a `true`

#### Scenario: Ultima solicitacao encerrada remove pendencia
- **WHEN** a ultima solicitacao nao-terminal do modelo e concluida ou
  cancelada
- **THEN** `temPendenciaAberta` passa a `false`

#### Scenario: Recalculo e idempotente
- **WHEN** o valor atual de `temPendenciaAberta` ja reflete o estado real das
  solicitacoes
- **THEN** o sistema nao grava nenhuma alteracao

### Requirement: Listar e filtrar modelos
O sistema SHALL permitir listar modelos com paginacao e filtros por
ativo/codigo/maquina/descricao, retornando tambem a URL da foto de capa (foto
principal da galeria) de cada modelo.

#### Scenario: Listagem paginada com filtros
- **WHEN** um usuario autenticado lista modelos com filtros
- **THEN** o sistema retorna a pagina de resultados correspondente, cada item
  com a foto de capa resolvida (se houver)

### Requirement: Exportar relatorios em PDF
O sistema SHALL gerar um PDF com a lista de modelos filtrada, e um PDF com a
ficha completa de um modelo especifico (dados + eventos + solicitacoes
vinculadas).

#### Scenario: Exportar lista filtrada
- **WHEN** um usuario autenticado solicita a exportacao da lista com os
  mesmos filtros aplicados na tela
- **THEN** o sistema retorna um PDF tabular com os modelos filtrados

#### Scenario: Exportar ficha de um modelo
- **WHEN** um usuario autenticado solicita a ficha de um modelo existente
- **THEN** o sistema retorna um PDF com os dados do modelo, seu historico de
  eventos e as solicitacoes vinculadas
