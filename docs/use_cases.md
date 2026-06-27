# Casos de uso / especificação funcional — Sistema RGM

Este documento é a **especificação funcional principal** em formato de Casos de Uso, para facilitar a implementação e os testes.

Leituras complementares (detalhes):
- RBAC: [atores_e_permissoes.md](atores_e_permissoes.md)
- Fluxo Kanban: [fluxo_kanban.md](fluxo_kanban.md)
- Regras de negócio: [regras_de_negocio.md](regras_de_negocio.md)
- Modelo de dados: [modelo_de_dados.md](../agent/modelo_de_dados.md)
- Diagramas: [diagramas.md](../agent/diagramas.md)

---

## Convenções

- **Perfis**: `OPERADOR`, `GESTOR`, `ADMINISTRADOR`, `EXTERNO`
- **Status**: `A_FAZER`, `EM_ANDAMENTO`, `EM_VALIDACAO`, `CONCLUIDA`, `CANCELADA`
- **Regra-mãe (importante)**: existe diferença entre:
  - **Executar (ser responsável)** → depende de atribuição.
  - **Mover (poder de controle do card)** → `GESTOR` e `ADMINISTRADOR` podem mover qualquer card.

---

## UC-01 — Logar no sistema

**Atores**: Operador, Gestor, Administrador

**Objetivo**: autenticar e obter contexto do usuário (perfil).

**Regras**
- Usuários `EXTERNO` não fazem login (registro passivo).

**Fluxo principal**
1. Usuário informa credenciais.
2. Sistema valida credenciais e status ativo.
3. Sistema retorna token/sessão com `perfil`.

**Exceções**
- Credenciais inválidas → erro de autenticação.
- Usuário inativo → erro de autenticação.

---

## UC-02 — Abrir solicitação (`A_FAZER`)

**Ator primário**: Operador (Gestor também pode)

**Pré-condições**
- Usuário autenticado.
- `Modelo` existe e está ativo.

**Fluxo principal**
1. Ator cria solicitação (título, descrição, tipo, modelo).
2. Sistema cria `Solicitacao` em `A_FAZER`.
3. Sistema registra `AtividadeSolicitacao(ABERTURA)`.
4. Sistema garante `Modelo.temPendenciaAberta = true`.

**Exceções**
- Modelo inválido/inativo → erro de validação.

---

## UC-03 — Triar e atribuir (`A_FAZER` → `EM_ANDAMENTO`)

**Ator primário**: Gestor (Administrador herda)

**Pré-condições**
- Solicitação está em `A_FAZER`.

**Regras**
- Definir `prioridade` é obrigatório.
- Criar 1+ responsáveis (atribuições ativas).
- Responsáveis podem ser `OPERADOR`, `GESTOR` e/ou `EXTERNO`.
- `ADMINISTRADOR` não pode ser responsável atribuído.

**Fluxo principal**
1. Gestor informa prioridade e responsáveis.
2. Sistema valida transição.
3. Sistema muda status para `EM_ANDAMENTO` e salva prioridade.
4. Sistema cria `SolicitacaoAtribuicao` ativa para cada responsável.
5. Sistema registra `AtividadeSolicitacao(ATRIBUICAO)` e `AtividadeSolicitacao(MUDANCA_STATUS)`.

**Exceções**
- Sem responsáveis → erro de regra.
- Responsável inválido/inativo/administrador → erro de regra.
- Status atual não é `A_FAZER` → conflito de status.

---

## UC-04 — Movimentar solicitação no Kanban (autorização central)

**Atores**: Operador, Gestor, Administrador

**Objetivo**: centralizar e tornar explícita a regra do “quem pode mover”.

**Regras de autorização**
- `GESTOR` e `ADMINISTRADOR`:
  - podem mover qualquer solicitação, desde que a transição seja válida.
- `OPERADOR`:
  - só pode mover solicitações com atribuição ativa para ele.
  - e apenas para transições permitidas ao perfil.

**Saídas**
- Em sucesso: status atualizado + `AtividadeSolicitacao(MUDANCA_STATUS)`.

**Exceções**
- Não autorizado → `403`.
- Transição inválida → `409`.

---

## UC-05 — Enviar para validação (`EM_ANDAMENTO` → `EM_VALIDACAO`)

**Ator primário**: Operador atribuído

**Atores alternativos**: Gestor/Administrador (pode mover mesmo sem atribuição)

**Pré-condições**
- Solicitação está em `EM_ANDAMENTO`.

**Regras**
- Solicitações de `REPARO` ou `INSPECAO` exigem o anexo de pelo menos 1 evidência (foto/documento) antes de serem enviadas para validação (QC Gate).

**Fluxo principal**
1. Ator solicita mover para `EM_VALIDACAO`.
2. Sistema aplica UC-04 (autorização) + valida transição e exigência de evidência.
3. Sistema muda status para `EM_VALIDACAO` e registra atividade de mudança.

**Exceções**
- Operador não atribuído → `403`.
- Falha ao anexar evidência → ver UC-08.

---

## UC-06 — Devolver para correção (`EM_VALIDACAO` → `EM_ANDAMENTO`)

**Ator primário**: Gestor (Administrador herda)

**Regras**
- Motivo/comentário é obrigatório.
- Pode reatribuir responsáveis e ajustar prioridade (opcional).

**Fluxo principal**
1. Gestor devolve com motivo.
2. Sistema muda status para `EM_ANDAMENTO`.
3. Sistema registra atividade de mudança + comentário.

**Exceções**
- Sem motivo → erro de validação.

---

## UC-07 — Encerrar solicitação (`EM_VALIDACAO` → `CONCLUIDA`/`CANCELADA`)

**Ator primário**: Gestor (Administrador herda)

**Regras**
- Comentário final é obrigatório.

**Fluxo principal**
1. Gestor escolhe concluir ou cancelar.
2. Sistema valida transição e autorização.
3. Sistema grava data terminal (`concluidaEm` ou `canceladaEm`).
4. Sistema registra atividade.
5. Sistema publica evento `SolicitacaoFinalizadaEvent`.
6. Listener recalcula `Modelo.temPendenciaAberta` (ver UC-10).

**Exceções**
- Status atual não permite encerramento → `409`.

---

## UC-08 — Anexar evidência (upload)

**Atores**: Operador, Gestor, Administrador

**Objetivo**: anexar evidências no bucket (MinIO/S3) e persistir a `publicUrl` para exibição direta pelo frontend.

**Regras**
- Banco armazena `publicUrl` persistente (sem expiração).
- Não usar presigned URLs.
- O frontend acessa a evidência diretamente pela `publicUrl` (acesso por rede interna/VPN + política do bucket).

**Fluxo principal**
1. Ator envia imagem ao backend.
2. Backend valida permissão de acesso à solicitação.
3. Backend envia arquivo ao MinIO (S3 client) e define uma `publicUrl` persistente.
4. Backend grava `Evidencia` com metadados + `publicUrl`.
5. Backend relaciona evidência à solicitação.
6. Backend registra `AtividadeSolicitacao(EVIDENCIA_ADICIONADA)`.

**Exceções**
- MinIO indisponível/timeouts → erro de integração.
- Arquivo inválido (tipo/tamanho) → erro de validação.

---

## UC-09 — Visualizar evidência (leitura)

**Atores**: Operador, Gestor, Administrador

**Fluxo principal**
1. Front solicita detalhes da solicitação.
2. Backend valida acesso.
3. Backend retorna a solicitação com a lista de `publicUrl` persistentes das evidências.

**Exceções**
- Evidência não encontrada → `404`.
- Usuário sem acesso à solicitação → `403`.

---

## UC-10 — Recalcular `Modelo.temPendenciaAberta` (consistência)

**Atores**: Sistema

**Objetivo**: impedir inconsistência em campo derivado.

**Regra de verdade**
- `temPendenciaAberta` é `true` se existir solicitação do modelo em status não-terminal (`A_FAZER`, `EM_ANDAMENTO`, `EM_VALIDACAO`).

**Disparos**
- Ao abrir solicitação (UC-02).
- Ao encerrar solicitação (UC-07) via evento/listener.

**Exceções**
- Falha na atualização do Modelo → deve ser tratada para não deixar o estado mentir (ex.: retentativa ou job de correção).

---

## UC-11 — Cadastrar prestador externo (usuário perfil `EXTERNO`)

**Ator primário**: Administrador

**Objetivo**: representar o “externo” sem criar tipo de manutenção.

**Regras**
- Somente o `ADMINISTRADOR` pode criar usuários no sistema.
- Prestador externo é um registro passivo: não faz login.

**Fluxo principal**
1. Administrador cria usuário com `perfil=EXTERNO`.
2. Sistema salva usuário como ativo/inativo conforme decisão do Administrador.

---

## UC-12 — Atribuir a prestador externo e movimentar como procurador

**Ator primário**: Gestor

**Objetivo**: o Gestor controla o ciclo, coletando feedback do externo.

**Pré-condições**
- Prestador externo já está cadastrado (UC-11).

**Fluxo principal**
1. Gestor atribui a solicitação ao prestador externo (UC-03).
2. Gestor registra comentários do feedback conforme necessário.
3. Gestor movimenta o card (UC-04) até `EM_VALIDACAO`.
4. Gestor conclui/cancela (UC-07).

**Regras de auditoria**
- A movimentação e os comentários registram o autor real (Gestor/Administrador), mesmo quando a atribuição for do prestador externo.

---

## UC-13 — Administração (usuários, modelos)

**Atores e restrições obrigatórias**
- **Administrador**:
  - único com permissão para criar/editar/gerenciar Usuários (inclui `EXTERNO`);
  - herda permissões de Gestor para movimentar cards;
  - é invisível para atribuições: não pode ser responsável em `SolicitacaoAtribuicao`.
- **Gestor**:
  - responsável por cadastrar/editar/desativar Modelos.

Para visualizar os diagramas de casos de uso e sequência, ver [diagramas.md](../agent/diagramas.md).

---

## UC-14 — Atualizar foto capa do Modelo

**Ator primário**: Gestor (Administrador herda)

**Objetivo**: manter a foto de capa do Modelo atualizada por uma URL persistente (`fotoUrl`), reutilizando evidências quando possível.

**Opções suportadas**
1) Upload de nova foto
  - Backend envia arquivo ao bucket e atualiza `Modelo.fotoUrl`.
2) Usar evidência existente
  - Gestor seleciona uma `Evidencia` já vinculada ao histórico daquele Modelo e o backend atualiza `Modelo.fotoUrl = Evidencia.publicUrl`.

**Regras**
- Somente `GESTOR` (e `ADMINISTRADOR` por herança) pode executar.
- A evidência escolhida deve pertencer ao histórico do mesmo Modelo (por solicitação do `modeloId` e/ou por `EventoModelo`).
- Registrar auditoria/histórico da alteração (quem alterou e origem da foto).

**Exceções**
- Evidência não pertence ao Modelo → erro de regra.
- Bucket indisponível (no upload) → erro de integração.

---

## UC-15 — Exclusão (hard delete) de registros do sistema

**Ator primário**: Administrador

**Objetivo**: permitir exclusão permanente de registros inseridos incorretamente (painel administrativo).

**Fluxo principal**
1. Administrador acessa a visão administrativa do recurso (ex.: Solicitação, Modelo, Usuário).
2. Solicita a exclusão do registro.
3. Confirma a exclusão.
4. Backend valida `perfil=ADMINISTRADOR`.
5. Backend executa a exclusão em cascata quando aplicável.
6. Sistema atualiza a tela refletindo a deleção do item.

**Regras**
- Apenas `ADMINISTRADOR` pode realizar hard delete.
- Exclusão de Solicitação deve remover em cascata: atribuições, atividades e vínculos.
