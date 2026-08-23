## MODIFIED Requirements

### Requirement: Abrir solicitacao
O sistema SHALL permitir que qualquer usuario interno (OPERADOR, GESTOR ou
ADMINISTRADOR) abra uma nova solicitacao em A_FAZER para um modelo ativo,
informando titulo, descricao e tipo (REPARO, INSPECAO ou REENGENHARIA).

Adicionalmente, o sistema SHALL permitir que apenas GESTOR ou ADMINISTRADOR
abram uma solicitacao do tipo CRIACAO — um pedido de novo modelo. Uma
solicitacao CRIACAO nasce em A_FAZER **sem nenhum modelo vinculado**;
em vez de um `modeloId`, ela carrega os dados do modelo a ser criado
(codigo, maquina, descricao e observacoes), sujeitos as mesmas validacoes
aplicadas ao cadastro direto de um modelo (maquina deve existir no
catalogo, campos obrigatorios nao-vazios).

#### Scenario: Abertura bem-sucedida
- **WHEN** um usuario interno abre uma solicitacao (tipo REPARO, INSPECAO ou
  REENGENHARIA) para um modelo ativo
- **THEN** a solicitacao e criada em A_FAZER, sem prioridade, e o modelo
  passa a ter `temPendenciaAberta = true` (se ainda nao tinha)

#### Scenario: Modelo inativo
- **WHEN** o modelo informado esta inativo
- **THEN** o sistema rejeita com "Modelo inativo"

#### Scenario: EXTERNO tenta abrir solicitacao
- **WHEN** um usuario EXTERNO tenta abrir uma solicitacao
- **THEN** o sistema rejeita com "Perfil EXTERNO nao pode abrir solicitacoes"

#### Scenario: GESTOR/ADMINISTRADOR abre solicitacao de CRIACAO
- **WHEN** um GESTOR ou ADMINISTRADOR abre uma solicitacao do tipo CRIACAO
  informando codigo, maquina, descricao e observacoes do modelo pretendido
- **THEN** a solicitacao e criada em A_FAZER, sem `modeloId` e sem
  prioridade, carregando os dados do modelo informados

#### Scenario: OPERADOR tenta abrir solicitacao de CRIACAO
- **WHEN** um OPERADOR tenta abrir uma solicitacao do tipo CRIACAO
- **THEN** o sistema rejeita — apenas GESTOR/ADMINISTRADOR podem abrir esse
  tipo

#### Scenario: Dados do modelo invalidos na abertura de CRIACAO
- **WHEN** os dados do modelo informados na abertura de uma solicitacao
  CRIACAO sao invalidos (ex.: maquina inexistente no catalogo, codigo ou
  descricao em branco)
- **THEN** o sistema rejeita, sem criar a solicitacao

### Requirement: Concluir solicitacao
O sistema SHALL permitir que GESTOR/ADMINISTRADOR conclua uma solicitacao em
EM_VALIDACAO, exigindo um comentario final. Ao concluir uma solicitacao de
tipo REPARO, INSPECAO ou REENGENHARIA, um evento e registrado
automaticamente no historico do modelo vinculado (ver capacidade
`eventos-modelo`).

Ao concluir uma solicitacao do tipo CRIACAO, o sistema SHALL, na mesma
operacao: criar o Modelo com os dados carregados pela solicitacao (mesma
regra de versionamento por codigo+maquina do cadastro direto), associar o
`modeloId` do Modelo recem-criado a solicitacao, e so entao marcar a
solicitacao como CONCLUIDA. Se a criacao do modelo falhar (ex.: maquina
deixou de existir no catalogo entre a abertura e a conclusao), a conclusao
inteira SHALL falhar, sem deixar a solicitacao CONCLUIDA sem modelo
vinculado.

#### Scenario: Conclusao bem-sucedida
- **WHEN** um GESTOR/ADMINISTRADOR conclui uma solicitacao de tipo REPARO,
  INSPECAO ou REENGENHARIA com comentario final informado
- **THEN** a solicitacao vai para CONCLUIDA, `concluidaEm` e preenchido, e um
  evento e criado no historico do modelo vinculado

#### Scenario: Conclusao de solicitacao CRIACAO cria o modelo
- **WHEN** um GESTOR/ADMINISTRADOR conclui uma solicitacao do tipo CRIACAO
  com comentario final informado
- **THEN** um novo Modelo e criado com os dados carregados pela
  solicitacao, a solicitacao passa a ter esse `modeloId`, vai para
  CONCLUIDA e `concluidaEm` e preenchido

#### Scenario: Falha ao criar o modelo na conclusao de CRIACAO
- **WHEN** a criacao do modelo falha durante a conclusao de uma solicitacao
  CRIACAO (ex.: dado invalido persistido na abertura que so e revalidado na
  conclusao)
- **THEN** a conclusao inteira e rejeitada — a solicitacao permanece em
  EM_VALIDACAO, sem `modeloId` e sem Modelo criado

### Requirement: Editar dados da solicitacao
O sistema SHALL permitir editar titulo e descricao de uma solicitacao
apenas enquanto ela estiver em status nao-terminal. O tipo da solicitacao
SHALL ser imutavel apos a abertura — nao pode ser alterado para nenhum
outro valor, incluindo de/para CRIACAO.

#### Scenario: Edicao permitida
- **WHEN** a solicitacao esta em A_FAZER, EM_ANDAMENTO ou EM_VALIDACAO
- **THEN** titulo e descricao podem ser alterados

#### Scenario: Edicao bloqueada apos encerramento
- **WHEN** a solicitacao esta CONCLUIDA ou CANCELADA
- **THEN** o sistema rejeita a edicao

#### Scenario: Tentativa de alterar o tipo
- **WHEN** uma edicao tenta alterar o `tipo` da solicitacao para um valor
  diferente do original
- **THEN** o sistema rejeita a edicao, independente do status
