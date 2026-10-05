# Galeria Modelo Specification

## Purpose

Cada Modelo tem uma galeria de fotos de apresentacao/estado atual (0..N
fotos), independente do historico de evidencias de solicitacoes. No maximo uma
foto pode ser marcada como principal (usada como capa nas listagens). Gerenciar
a galeria e restrito a GESTOR/ADMINISTRADOR.

## Requirements

### Requirement: Somente GESTOR/ADMINISTRADOR gerencia a galeria
O sistema SHALL restringir adicionar, editar e remover fotos da galeria,
por acao direta de um usuario, aos perfis GESTOR e ADMINISTRADOR. Esta
restricao nao se aplica ao gatilho automatico de evidencia elegivel
(ver capacidade `evidencias`) — esse caminho e um efeito colateral do
sistema, nao uma acao direta de gestao de galeria, e nao exige o perfil do
autor da evidencia.

#### Scenario: OPERADOR ou EXTERNO tenta gerenciar a galeria
- **WHEN** um usuario sem permissao tenta adicionar/editar/remover uma foto
  diretamente pela interface de galeria
- **THEN** o sistema rejeita com "Perfil sem permissao para gerenciar galeria
  do Modelo"

#### Scenario: Evidencia de OPERADOR alimenta a galeria automaticamente
- **WHEN** um OPERADOR anexa uma evidencia elegivel (SERVICO_REALIZADO ou
  CONCLUSAO, imagem compativel) a uma solicitacao com modelo vinculado
- **THEN** a foto e adicionada a galeria automaticamente, mesmo o OPERADOR
  nao tendo permissao de gestao direta de galeria

### Requirement: Adicionar foto a galeria
O sistema SHALL permitir anexar uma foto (JPEG/PNG/WEBP, ate 10 MB) a um
modelo existente, com uma identificacao (rotulo) obrigatoria. A primeira foto
de um modelo e automaticamente marcada como principal.

Quando a foto e adicionada automaticamente a partir de uma evidencia
elegivel (ver capacidade `evidencias`), a identificacao SHALL ser gerada
automaticamente pelo sistema (ex.: a partir do tipo de evidencia e da
solicitacao de origem), e o arquivo SHALL ser uma copia independente no
storage — nunca compartilhando o mesmo objeto fisico da evidencia original,
para que excluir um nao afete o outro.

#### Scenario: Primeira foto do modelo
- **WHEN** o modelo ainda nao tem nenhuma foto na galeria
- **THEN** a foto adicionada e marcada como `principal = true`

#### Scenario: Fotos subsequentes
- **WHEN** o modelo ja tem ao menos uma foto
- **THEN** a nova foto e adicionada com `principal = false`

#### Scenario: Identificacao ausente
- **WHEN** a identificacao nao e informada ou esta em branco em uma
  adicao manual
- **THEN** o sistema rejeita com "Identificacao da foto e obrigatoria"

#### Scenario: Arquivo invalido
- **WHEN** o arquivo excede 10 MB ou nao e JPEG/PNG/WEBP
- **THEN** o sistema rejeita o upload

#### Scenario: Identificacao gerada automaticamente
- **WHEN** a foto e adicionada pelo gatilho automatico de evidencia
  elegivel
- **THEN** a identificacao e preenchida pelo sistema (ex.: "Servico
  realizado — <titulo da solicitacao>"), sem exigir entrada manual

#### Scenario: Arquivo duplicado, nao compartilhado
- **WHEN** uma foto e adicionada automaticamente a partir de uma
  evidencia
- **THEN** o arquivo da foto de galeria e um objeto de storage
  independente do arquivo da evidencia original

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
