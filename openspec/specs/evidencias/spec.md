# Evidencias Specification

## Purpose

Uma Evidencia e um anexo (foto/PDF/video) de ate 10 MB vinculado a uma
Solicitacao, categorizado por `tipo` conforme o ponto do fluxo Kanban em que
foi anexado, com uma `descricao` opcional (obrigatoria para o tipo
SERVICO_REALIZADO). Evidencias documentam o que foi pedido, o que foi feito e
o que ainda falta, ao longo do ciclo de vida da solicitacao.

## Requirements

### Requirement: Tipos de evidencia
O sistema SHALL categorizar cada evidencia com um dos tipos: GERAL (anexo
avulso, sem ponto especifico do fluxo), ABERTURA (anexada ao abrir a
solicitacao), INSTRUCAO_SERVICO (anexada na triagem, descrevendo o que fazer),
SERVICO_REALIZADO (anexada ao enviar para validacao, provando o servico
feito), CONCLUSAO (anexada ao concluir) e DEVOLUCAO (anexada ao devolver para
correcao).

#### Scenario: Tipo nao informado
- **WHEN** uma evidencia e anexada sem informar o tipo
- **THEN** o sistema assume GERAL

### Requirement: Anexar evidencia
O sistema SHALL permitir anexar um arquivo (imagem JPEG/PNG/GIF/WEBP, PDF ou
MP4, ate 10 MB) a uma solicitacao nao-encerrada, desde que o usuario tenha
acesso a ela (atribuido, ou perfil com gestao de modelos/usuarios).

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

### Requirement: Descricao obrigatoria para SERVICO_REALIZADO
O sistema SHALL exigir uma descricao nao vazia sempre que o tipo da evidencia
for SERVICO_REALIZADO, refletindo o que foi feito para resolver a
solicitacao.

#### Scenario: SERVICO_REALIZADO com descricao
- **WHEN** uma evidencia do tipo SERVICO_REALIZADO e anexada com descricao
  preenchida
- **THEN** a evidencia e criada normalmente

#### Scenario: SERVICO_REALIZADO sem descricao
- **WHEN** a descricao esta ausente ou em branco
- **THEN** o sistema rejeita a criacao da evidencia

### Requirement: Gate de envio para validacao exige SERVICO_REALIZADO especifico
O sistema SHALL exigir, para solicitacoes de REPARO/INSPECAO/REENGENHARIA,
que exista ao menos uma evidencia do tipo SERVICO_REALIZADO anexada a essa
solicitacao especifica antes de permitir o envio para EM_VALIDACAO — uma
evidencia antiga de outro tipo (ex.: ABERTURA) nao satisfaz essa exigencia.

#### Scenario: Evidencia de servico realizado presente
- **WHEN** existe ao menos uma evidencia SERVICO_REALIZADO vinculada a
  solicitacao
- **THEN** o envio para validacao e permitido

#### Scenario: Apenas evidencias de outros tipos presentes
- **WHEN** a solicitacao tem evidencias anexadas, mas nenhuma do tipo
  SERVICO_REALIZADO
- **THEN** o sistema rejeita o envio para validacao

### Requirement: Listar e excluir evidencias
O sistema SHALL permitir listar todas as evidencias de uma solicitacao. A
exclusao (removendo registro e arquivo fisico) e permitida apenas em
solicitacao nao-terminal, para quem enviou a evidencia, esta atribuido a
solicitacao, ou tem perfil de gestao (GESTOR/ADMINISTRADOR).

#### Scenario: Listagem
- **WHEN** um usuario com acesso a solicitacao lista suas evidencias
- **THEN** o sistema retorna todas, com tipo e descricao

#### Scenario: Exclusao pelo proprio autor ou por um gestor
- **WHEN** quem exclui e quem enviou a evidencia, ou tem perfil
  GESTOR/ADMINISTRADOR, e a solicitacao nao esta encerrada
- **THEN** o registro e o arquivo no storage sao removidos

#### Scenario: Exclusao em solicitacao encerrada
- **WHEN** a solicitacao esta CONCLUIDA ou CANCELADA
- **THEN** o sistema rejeita "Nao e possivel excluir evidencia de solicitacao
  encerrada"
