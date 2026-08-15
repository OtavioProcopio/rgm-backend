# Galeria Modelo Specification

## Purpose

Cada Modelo tem uma galeria de fotos de apresentacao/estado atual (0..N
fotos), independente do historico de evidencias de solicitacoes. No maximo uma
foto pode ser marcada como principal (usada como capa nas listagens). Gerenciar
a galeria e restrito a GESTOR/ADMINISTRADOR.

## Requirements

### Requirement: Somente GESTOR/ADMINISTRADOR gerencia a galeria
O sistema SHALL restringir adicionar, editar e remover fotos da galeria aos
perfis GESTOR e ADMINISTRADOR.

#### Scenario: OPERADOR ou EXTERNO tenta gerenciar a galeria
- **WHEN** um usuario sem permissao tenta adicionar/editar/remover uma foto
- **THEN** o sistema rejeita com "Perfil sem permissao para gerenciar galeria
  do Modelo"

### Requirement: Adicionar foto a galeria
O sistema SHALL permitir anexar uma foto (JPEG/PNG/WEBP, ate 10 MB) a um
modelo existente, com uma identificacao (rotulo) obrigatoria. A primeira foto
de um modelo e automaticamente marcada como principal.

#### Scenario: Primeira foto do modelo
- **WHEN** o modelo ainda nao tem nenhuma foto na galeria
- **THEN** a foto adicionada e marcada como `principal = true`

#### Scenario: Fotos subsequentes
- **WHEN** o modelo ja tem ao menos uma foto
- **THEN** a nova foto e adicionada com `principal = false`

#### Scenario: Identificacao ausente
- **WHEN** a identificacao nao e informada ou esta em branco
- **THEN** o sistema rejeita com "Identificacao da foto e obrigatoria"

#### Scenario: Arquivo invalido
- **WHEN** o arquivo excede 10 MB ou nao e JPEG/PNG/WEBP
- **THEN** o sistema rejeita o upload

### Requirement: No maximo uma foto principal por modelo
O sistema SHALL garantir que cada modelo tenha no maximo uma foto marcada como
principal a qualquer momento.

#### Scenario: Marcar outra foto como principal
- **WHEN** uma foto diferente da atual principal e marcada como principal
- **THEN** o sistema desmarca a foto principal anterior antes de marcar a nova

### Requirement: Editar identificacao e foto principal
O sistema SHALL permitir renomear a identificacao de uma foto e/ou
alterar sua condicao de principal, desde que a foto pertenca ao modelo
informado.

#### Scenario: Renomear foto
- **WHEN** uma nova identificacao nao vazia e informada
- **THEN** a identificacao e atualizada

#### Scenario: Foto de outro modelo
- **WHEN** o `fotoId` informado nao pertence ao `modeloId` informado
- **THEN** o sistema rejeita com "Foto nao pertence a galeria deste Modelo"

### Requirement: Remover foto da galeria
O sistema SHALL permitir remover uma foto da galeria, apagando tanto o
registro quanto o arquivo fisico no storage (MinIO/S3).

#### Scenario: Remocao bem-sucedida
- **WHEN** a foto pertence ao modelo informado
- **THEN** o registro e removido e o arquivo e apagado do storage

### Requirement: Listar galeria de um modelo
O sistema SHALL permitir listar todas as fotos da galeria de um modelo.

#### Scenario: Listagem
- **WHEN** um usuario autenticado consulta a galeria de um modelo
- **THEN** o sistema retorna todas as fotos, indicando qual e a principal
