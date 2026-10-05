## MODIFIED Requirements

### Requirement: Anexar evidencia
O sistema SHALL permitir anexar um arquivo (imagem JPEG/PNG/GIF/WEBP, PDF ou
MP4, ate 10 MB) a uma solicitacao nao-encerrada, desde que o usuario tenha
acesso a ela (atribuido, ou perfil com gestao de modelos/usuarios).

Quando a evidencia anexada for do tipo SERVICO_REALIZADO ou CONCLUSAO, o
arquivo for uma imagem em um formato aceito pela galeria do modelo
(JPEG/PNG/WEBP — GIF nao qualifica), e a solicitacao ja tiver um modelo
vinculado, o sistema SHALL automaticamente adicionar essa mesma foto a
galeria daquele modelo (ver capacidade `galeria-modelo`), sem exigir que
quem anexou a evidencia tenha permissao de gestao de galeria.

#### Scenario: Anexo bem-sucedido
- **WHEN** um usuario com acesso anexa um arquivo valido a uma solicitacao
  nao-terminal
- **THEN** a evidencia e criada e associada a solicitacao, e uma atividade de
  "evidencia adicionada" e registrada

#### Scenario: Arquivo excede o limite
- **WHEN** o arquivo excede 10 MB
- **THEN** o sistema rejeita o upload

#### Scenario: Tipo de arquivo nao permitido
- **WHEN** o mime type nao esta entre os aceitos
- **THEN** o sistema rejeita o upload

#### Scenario: Solicitacao encerrada
- **WHEN** a solicitacao esta CONCLUIDA ou CANCELADA
- **THEN** o sistema rejeita "Nao e possivel anexar evidencia a solicitacao
  encerrada"

#### Scenario: Usuario sem acesso
- **WHEN** o usuario nao esta atribuido a solicitacao e nao possui perfil de
  gestao (GESTOR/ADMINISTRADOR)
- **THEN** o sistema rejeita por falta de autorizacao

#### Scenario: Evidencia de SERVICO_REALIZADO em imagem alimenta a galeria
- **WHEN** uma evidencia do tipo SERVICO_REALIZADO e anexada como imagem
  JPEG/PNG/WEBP a uma solicitacao que ja tem modelo vinculado
- **THEN** a mesma foto e adicionada automaticamente a galeria daquele
  modelo, com autor igual a quem anexou a evidencia, independente do
  perfil desse usuario

#### Scenario: Evidencia de CONCLUSAO em imagem alimenta a galeria
- **WHEN** uma evidencia do tipo CONCLUSAO e anexada como imagem
  JPEG/PNG/WEBP a uma solicitacao que ja tem modelo vinculado
- **THEN** a mesma foto e adicionada automaticamente a galeria daquele
  modelo

#### Scenario: Tipo de evidencia nao elegivel nao alimenta a galeria
- **WHEN** uma evidencia dos tipos GERAL, ABERTURA, INSTRUCAO_SERVICO ou
  DEVOLUCAO e anexada
- **THEN** nenhuma foto e adicionada a galeria do modelo

#### Scenario: Evidencia em formato nao aceito pela galeria
- **WHEN** uma evidencia do tipo SERVICO_REALIZADO ou CONCLUSAO e anexada
  como GIF, PDF ou MP4
- **THEN** a evidencia e criada normalmente, mas nenhuma foto e adicionada
  a galeria (formato incompativel com a galeria)

#### Scenario: Solicitacao ainda sem modelo vinculado
- **WHEN** uma evidencia elegivel (SERVICO_REALIZADO ou CONCLUSAO, imagem
  compativel) e anexada a uma solicitacao do tipo CRIACAO que ainda nao
  foi concluida (sem `modeloId`)
- **THEN** a evidencia e criada normalmente, mas nenhuma foto e adicionada
  a nenhuma galeria — nao ha retroatividade quando o modelo for criado
  depois
