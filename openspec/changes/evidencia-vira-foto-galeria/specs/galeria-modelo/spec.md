## MODIFIED Requirements

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
