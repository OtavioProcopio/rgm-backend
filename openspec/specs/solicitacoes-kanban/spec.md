# Solicitacoes Kanban Specification

## Purpose

O nucleo do sistema: uma Solicitacao e um chamado de manutencao vinculado a um
Modelo, que percorre um fluxo Kanban de 5 status
(A_FAZER → EM_ANDAMENTO → EM_VALIDACAO → CONCLUIDA/CANCELADA, com devolucao de
EM_VALIDACAO de volta a EM_ANDAMENTO). Cada transicao tem regras proprias de
autorizacao por perfil, e o historico de mudancas/comentarios fica registrado
como atividades auditaveis.

## Requirements

### Requirement: Maquina de estados do Kanban
O sistema SHALL permitir apenas as seguintes transicoes de status: A_FAZER →
EM_ANDAMENTO; A_FAZER → CANCELADA; EM_ANDAMENTO → EM_VALIDACAO; EM_ANDAMENTO →
CANCELADA; EM_VALIDACAO → EM_ANDAMENTO (devolucao); EM_VALIDACAO → CONCLUIDA;
EM_VALIDACAO → CANCELADA. CONCLUIDA e CANCELADA sao status terminais, sem
transicao de saida.

#### Scenario: Transicao valida
- **WHEN** a transicao solicitada esta entre as permitidas para o status atual
- **THEN** o sistema aplica a transicao

#### Scenario: Transicao invalida
- **WHEN** a transicao solicitada nao esta entre as permitidas (ex.: A_FAZER
  direto para CONCLUIDA, ou qualquer transicao a partir de um status terminal)
- **THEN** o sistema rejeita com erro de transicao de status invalida

#### Scenario: Prioridade obrigatoria a partir de EM_ANDAMENTO
- **WHEN** a solicitacao entra em EM_ANDAMENTO, EM_VALIDACAO ou CONCLUIDA
- **THEN** uma prioridade (BAIXA/MEDIA/ALTA/URGENTE) e obrigatoria

### Requirement: Autorizacao central por perfil para mover o card
O sistema SHALL centralizar a autorizacao de movimentacao do Kanban: GESTOR e
ADMINISTRADOR podem mover qualquer solicitacao entre quaisquer status
permitidos pela maquina de estados; OPERADOR so pode mover uma solicitacao a
que esteja atribuido como responsavel, e apenas de EM_ANDAMENTO para
EM_VALIDACAO; EXTERNO nunca move solicitacoes.

#### Scenario: GESTOR/ADMINISTRADOR move qualquer card
- **WHEN** um GESTOR ou ADMINISTRADOR aciona qualquer transicao valida
- **THEN** a movimentacao e permitida, independente de atribuicao

#### Scenario: OPERADOR atribuido envia para validacao
- **WHEN** um OPERADOR atribuido como responsavel move sua solicitacao de
  EM_ANDAMENTO para EM_VALIDACAO
- **THEN** a movimentacao e permitida

#### Scenario: OPERADOR nao atribuido tenta mover
- **WHEN** um OPERADOR sem atribuicao na solicitacao tenta mover qualquer
  card
- **THEN** o sistema rejeita com "Operador so pode mover solicitacoes
  atribuidas a ele"

#### Scenario: OPERADOR tenta transicao fora do seu escopo
- **WHEN** um OPERADOR atribuido tenta uma transicao diferente de
  EM_ANDAMENTO → EM_VALIDACAO (ex.: triar, concluir, devolver)
- **THEN** o sistema rejeita

#### Scenario: EXTERNO tenta mover
- **WHEN** um usuario EXTERNO tenta mover qualquer solicitacao
- **THEN** o sistema rejeita com "Perfil EXTERNO nao pode mover solicitacoes"

### Requirement: Abrir solicitacao
O sistema SHALL permitir que qualquer usuario interno (OPERADOR, GESTOR ou
ADMINISTRADOR) abra uma nova solicitacao em A_FAZER para um modelo ativo,
informando titulo, descricao e tipo (REPARO, INSPECAO ou REENGENHARIA).

#### Scenario: Abertura bem-sucedida
- **WHEN** um usuario interno abre uma solicitacao para um modelo ativo
- **THEN** a solicitacao e criada em A_FAZER, sem prioridade, e o modelo passa
  a ter `temPendenciaAberta = true` (se ainda nao tinha)

#### Scenario: Modelo inativo
- **WHEN** o modelo informado esta inativo
- **THEN** o sistema rejeita com "Modelo inativo"

#### Scenario: EXTERNO tenta abrir solicitacao
- **WHEN** um usuario EXTERNO tenta abrir uma solicitacao
- **THEN** o sistema rejeita com "Perfil EXTERNO nao pode abrir solicitacoes"

### Requirement: Triar e atribuir responsaveis
O sistema SHALL permitir que GESTOR/ADMINISTRADOR triem uma solicitacao em
A_FAZER, definindo prioridade e atribuindo ao menos um responsavel (perfil
OPERADOR ou GESTOR, obrigatoriamente ativo), movendo-a para EM_ANDAMENTO.

#### Scenario: Triagem bem-sucedida
- **WHEN** um GESTOR/ADMINISTRADOR define prioridade e ao menos um
  responsavel ativo e atribuivel
- **THEN** a solicitacao vai para EM_ANDAMENTO com os responsaveis atribuidos

#### Scenario: Sem responsavel informado
- **WHEN** a lista de responsaveis esta vazia ou ausente
- **THEN** o sistema rejeita com "Deve ter pelo menos 1 responsavel"

#### Scenario: Responsavel inativo ou nao atribuivel
- **WHEN** algum responsavel informado esta inativo, ou tem perfil
  ADMINISTRADOR/EXTERNO (nao atribuivel)
- **THEN** o sistema rejeita a triagem

### Requirement: Enviar para validacao
O sistema SHALL permitir mover EM_ANDAMENTO → EM_VALIDACAO, exigindo um
comentario descrevendo o servico realizado. Para solicitacoes de REPARO,
INSPECAO ou REENGENHARIA, exige tambem ao menos uma evidencia do tipo
SERVICO_REALIZADO ja anexada (ver capacidade `evidencias`).

#### Scenario: Envio bem-sucedido
- **WHEN** o comentario e informado e existe evidencia de SERVICO_REALIZADO
  anexada
- **THEN** a solicitacao vai para EM_VALIDACAO e o comentario e registrado
  como atividade

#### Scenario: Comentario ausente
- **WHEN** o comentario nao e informado ou esta em branco
- **THEN** o sistema rejeita

#### Scenario: Sem evidencia do servico realizado
- **WHEN** nao existe nenhuma evidencia do tipo SERVICO_REALIZADO anexada a
  esta solicitacao
- **THEN** o sistema rejeita, exigindo o anexo antes de prosseguir

### Requirement: Devolver para correcao
O sistema SHALL permitir que GESTOR/ADMINISTRADOR devolva uma solicitacao de
EM_VALIDACAO para EM_ANDAMENTO, exigindo um motivo e permitindo opcionalmente
ajustar a prioridade.

#### Scenario: Devolucao bem-sucedida
- **WHEN** um GESTOR/ADMINISTRADOR devolve com motivo informado
- **THEN** a solicitacao volta para EM_ANDAMENTO, mantendo a prioridade
  anterior se nenhuma nova for informada

#### Scenario: Motivo ausente
- **WHEN** o motivo nao e informado
- **THEN** o sistema rejeita com "Motivo e obrigatorio para devolucao"

### Requirement: Concluir solicitacao
O sistema SHALL permitir que GESTOR/ADMINISTRADOR conclua uma solicitacao em
EM_VALIDACAO, exigindo um comentario final. Ao concluir, um evento e
registrado automaticamente no historico do modelo (ver capacidade
`eventos-modelo`).

#### Scenario: Conclusao bem-sucedida
- **WHEN** um GESTOR/ADMINISTRADOR conclui com comentario final informado
- **THEN** a solicitacao vai para CONCLUIDA, `concluidaEm` e preenchido, e um
  evento e criado no historico do modelo vinculado

### Requirement: Cancelar solicitacao
O sistema SHALL permitir cancelar uma solicitacao de duas formas: (1)
GESTOR/ADMINISTRADOR pode cancelar a partir de qualquer status nao-terminal;
(2) o proprio OPERADOR que abriu a solicitacao pode cancela-la enquanto ela
ainda estiver em A_FAZER e sem nenhum responsavel atribuido. Em ambos os
casos um motivo e obrigatorio.

#### Scenario: GESTOR/ADMINISTRADOR cancela
- **WHEN** um GESTOR/ADMINISTRADOR cancela uma solicitacao nao-terminal com
  motivo informado
- **THEN** a solicitacao vai para CANCELADA, `canceladaEm` e preenchido

#### Scenario: OPERADOR cancela a propria solicitacao ainda nao triada
- **WHEN** o OPERADOR que abriu a solicitacao a cancela enquanto ela esta em
  A_FAZER e sem responsavel atribuido
- **THEN** a solicitacao vai para CANCELADA

#### Scenario: OPERADOR tenta cancelar solicitacao ja triada
- **WHEN** a solicitacao ja possui um responsavel atribuido
- **THEN** o sistema rejeita, orientando a solicitar ao gestor

#### Scenario: OPERADOR tenta cancelar solicitacao de outro usuario
- **WHEN** o OPERADOR nao e quem abriu a solicitacao
- **THEN** o sistema rejeita

#### Scenario: OPERADOR tenta cancelar fora de A_FAZER
- **WHEN** a solicitacao ja saiu de A_FAZER (EM_ANDAMENTO ou EM_VALIDACAO)
- **THEN** o OPERADOR nao pode mais cancela-la — apenas GESTOR/ADMINISTRADOR
  podem

#### Scenario: Motivo ausente
- **WHEN** o motivo do cancelamento nao e informado
- **THEN** o sistema rejeita

### Requirement: Gerenciar responsaveis
O sistema SHALL permitir que GESTOR/ADMINISTRADOR adicione/remova
responsaveis de uma solicitacao nao-terminal a qualquer momento (nao apenas
na triagem), mantendo sempre ao menos um responsavel.

#### Scenario: Substituir responsaveis
- **WHEN** um GESTOR/ADMINISTRADOR envia uma nova lista de responsaveis
  diferente da atual
- **THEN** os que saem sao removidos (soft-delete com `removidoEm`) e os que
  entram sao atribuidos, cada mudanca registrada como atividade

#### Scenario: Solicitacao terminal
- **WHEN** a solicitacao ja esta CONCLUIDA ou CANCELADA
- **THEN** o sistema rejeita a gestao de responsaveis

### Requirement: Editar dados da solicitacao
O sistema SHALL permitir editar titulo, descricao e tipo de uma solicitacao
apenas enquanto ela estiver em status nao-terminal.

#### Scenario: Edicao permitida
- **WHEN** a solicitacao esta em A_FAZER, EM_ANDAMENTO ou EM_VALIDACAO
- **THEN** titulo, descricao e tipo podem ser alterados

#### Scenario: Edicao bloqueada apos encerramento
- **WHEN** a solicitacao esta CONCLUIDA ou CANCELADA
- **THEN** o sistema rejeita a edicao

### Requirement: Comentar em solicitacao
O sistema SHALL permitir registrar comentarios como atividade auditavel.
GESTOR/ADMINISTRADOR podem comentar em qualquer solicitacao, inclusive
encerradas. OPERADOR so pode comentar em solicitacoes que abriu ou as quais
esteja atribuido, e nunca em solicitacoes encerradas. EXTERNO nunca comenta
diretamente (precisa de um GESTOR como procurador).

#### Scenario: OPERADOR comenta em solicitacao propria
- **WHEN** o OPERADOR que abriu ou esta atribuido comenta em uma solicitacao
  nao-terminal
- **THEN** o comentario e registrado

#### Scenario: OPERADOR tenta comentar sem vinculo
- **WHEN** o OPERADOR nao abriu nem esta atribuido a solicitacao
- **THEN** o sistema rejeita

#### Scenario: Comentario em solicitacao encerrada
- **WHEN** um OPERADOR tenta comentar em solicitacao CONCLUIDA/CANCELADA
- **THEN** o sistema rejeita; apenas GESTOR/ADMINISTRADOR podem

### Requirement: Listar, filtrar e exportar solicitacoes
O sistema SHALL permitir listar solicitacoes com paginacao e filtros (status,
modelo, tipo, periodo, maquina, atrasada), e exportar a lista filtrada em PDF
com trilha de auditoria (quem abriu, quem alterou, quando).

#### Scenario: Listagem paginada e filtrada
- **WHEN** um usuario autenticado lista solicitacoes com filtros
- **THEN** o sistema retorna a pagina correspondente

#### Scenario: Filtro por maquina
- **WHEN** um usuario autenticado lista solicitacoes filtrando por uma
  maquina do catalogo
- **THEN** o sistema retorna apenas solicitacoes cujo modelo pertence aquela
  maquina

#### Scenario: Filtro por atrasada
- **WHEN** um usuario autenticado lista solicitacoes com `atrasada=true`
- **THEN** o sistema retorna apenas solicitacoes cujo SLA (ver Requirement
  "SLA de solicitacoes") esta vencido, sem carregar solicitacoes em memoria
  para filtrar (calculo feito no banco)

#### Scenario: Exportar PDF
- **WHEN** um usuario autenticado exporta a lista com os filtros aplicados
- **THEN** o sistema retorna um PDF tabular com a auditoria completa

### Requirement: Lock otimista em transicoes de status
O sistema SHALL usar lock otimista (coluna `version`) em `Solicitacao` para
detectar e rejeitar transicoes concorrentes no mesmo card, evitando lost
update silencioso quando dois usuarios movem a mesma solicitacao quase
simultaneamente.

#### Scenario: Duas transicoes concorrentes na mesma solicitacao
- **WHEN** duas requisicoes leem a mesma solicitacao na mesma versao e ambas
  tentam persistir uma transicao de status
- **THEN** apenas a primeira a persistir tem sucesso; a segunda recebe 409
  Conflict, sem sobrescrever silenciosamente a mudanca da primeira

### Requirement: SLA de solicitacoes
O sistema SHALL calcular, para cada solicitacao, um prazo-limite de SLA
baseado na prioridade (URGENTE=4h, ALTA=24h, MEDIA=72h, BAIXA=168h) contado a
partir da abertura (`criadaEm`), expondo em toda resposta de solicitacao o
prazo-limite, o tempo restante, se esta atrasada e o tempo total de
resolucao quando concluida.

#### Scenario: Sem prazo antes da triagem
- **WHEN** a solicitacao ainda esta em A_FAZER, sem prioridade definida
- **THEN** prazo-limite, tempo restante e tempo de resolucao sao nulos, e
  `atrasada` e falso

#### Scenario: Prazo calculado apos a triagem
- **WHEN** a solicitacao recebe uma prioridade na triagem
- **THEN** o prazo-limite passa a ser `criadaEm` mais as horas de SLA da
  prioridade, e o tempo restante reflete a diferenca ate esse prazo

#### Scenario: Solicitacao atrasada em andamento
- **WHEN** o instante atual e posterior ao prazo-limite e a solicitacao
  ainda nao esta em status terminal
- **THEN** `atrasada` e verdadeiro e o tempo restante e negativo

#### Scenario: Atraso avaliado na conclusao
- **WHEN** a solicitacao e concluida
- **THEN** `atrasada` compara `concluidaEm` (nao o instante atual) com o
  prazo-limite, e `tempoResolucaoSegundos` passa a refletir `concluidaEm -
  criadaEm`

#### Scenario: Cancelamento nunca conta como atraso
- **WHEN** a solicitacao e cancelada, mesmo apos o prazo-limite ter passado
- **THEN** `atrasada` e sempre falso
