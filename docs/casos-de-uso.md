# Casos de Uso — RGM Backend

Referência de todos os casos de uso implementados no sistema.

## UC-01 — Logar no sistema
- **Atores**: Operador, Gestor, Administrador
- **Classe**: `LoginUseCase`
- **Endpoint**: `POST /api/auth/login`
- **Sessão de usuário desativado**: toda chamada autenticada consulta o usuário; se ele foi desativado ou removido, a chamada é tratada como não autenticada (401), sem esperar o token vencer. Vale também para a abertura da conexão de eventos
- **Regras**: EXTERNO não faz login; valida credenciais + status ativo. A resposta traz `id` (identificador do usuário), `token`, `refreshToken`, `nome` e `perfil`
- **Erros**: 401 (credenciais inválidas ou inativo)

## UC-02 — Abrir solicitação (A_FAZER)
- **Ator**: Operador (Gestor também pode)
- **Classe**: `AbrirSolicitacaoUseCase`
- **Endpoint**: `POST /api/solicitacoes`
- **Regras**: Modelo deve existir e estar ativo; EXTERNO não pode abrir; registra atividade ABERTURA; atualiza `temPendenciaAberta`. Tipo `CRIACAO` é um fluxo à parte — ver UC-19.
- **Erros**: 422 (modelo inativo), 403 (EXTERNO)

## UC-03 — Triar e atribuir (A_FAZER → EM_ANDAMENTO)
- **Ator**: Gestor, Administrador
- **Classe**: `TriarSolicitacaoUseCase`
- **Endpoint**: `PATCH /api/solicitacoes/{id}/triar`
- **Regras**: Prioridade obrigatória; 1+ responsáveis; ADMINISTRADOR não pode ser atribuído
- **Erros**: 422 (sem responsáveis), 409 (status inválido), 403 (sem permissão)

## UC-04 — Autorização central de movimentação
- **Atores**: Operador, Gestor, Administrador
- **Classe**: `Solicitacao.validarAutorizacaoMover()`
- **Regras**: GESTOR/ADMIN movem qualquer; OPERADOR só move atribuições ativas dele
- **Erros**: 403 (não autorizado), 409 (transição inválida)

## UC-05 — Enviar para validação (EM_ANDAMENTO → EM_VALIDACAO)
- **Ator**: Operador atribuído, Gestor, Administrador
- **Classe**: `EnviarParaValidacaoUseCase`
- **Endpoint**: `PATCH /api/solicitacoes/{id}/enviar-validacao`
- **Regras**: UC-04 aplicado; operador deve estar atribuído
- **Erros**: 403 (operador não atribuído), 409 (status inválido)

## UC-06 — Devolver para correção (EM_VALIDACAO → EM_ANDAMENTO)
- **Ator**: Gestor, Administrador
- **Classe**: `DevolverSolicitacaoUseCase`
- **Endpoint**: `PATCH /api/solicitacoes/{id}/devolver`
- **Regras**: Motivo/comentário **obrigatório**; pode reatribuir prioridade
- **Erros**: 422 (sem motivo), 403 (sem permissão), 409 (status inválido)

## UC-07 — Encerrar solicitação (EM_VALIDACAO → CONCLUIDA/CANCELADA)
- **Ator**: Gestor, Administrador
- **Classe**: `EncerrarSolicitacaoUseCase`
- **Endpoint**: `PATCH /api/solicitacoes/{id}/encerrar`
- **Regras**: Comentário final **obrigatório**; publica `SolicitacaoFinalizadaEvent` (`modeloId` pode ser `null` ao cancelar uma solicitação `CRIACAO` ainda sem modelo). Ao **concluir** uma solicitação `CRIACAO`, o Modelo é criado nesse momento — ver UC-19.
- **Erros**: 422 (sem comentário), 403 (sem permissão), 409 (status inválido)

## UC-08 — Anexar evidência (upload)
- **Atores**: Operador, Gestor, Administrador
- **Classe**: `AnexarEvidenciaUseCase`
- **Endpoint**: `POST /api/solicitacoes/{id}/evidencias`
- **Regras**: Valida MIME type (imagens, PDF ou vídeo MP4) e tamanho (max 10MB); armazena publicUrl persistente; upload fora da transação DB. **Acesso** (`AcessoEvidenciaSolicitacao`): GESTOR, ADMINISTRADOR e responsável atribuído anexam qualquer tipo; quem abriu a solicitação e não é responsável anexa apenas `ABERTURA` e `GERAL`; usuário inativo é recusado; não se anexa em solicitação encerrada
- **Erros**: 400 (tipo de evidência, tipo de arquivo ou tamanho inválido), 422 (solicitação encerrada), 403 (sem acesso, tipo não permitido a quem abriu ou usuário inativo), 404 (solicitação ou usuário não encontrado), 500 (MinIO indisponível)

## UC-09 — Visualizar evidências
- **Atores**: Operador, Gestor, Administrador
- **Classe**: `VisualizarEvidenciaUseCase`
- **Endpoint**: `GET /api/solicitacoes/{id}/evidencias`
- **Regras**: Valida acesso (`AcessoEvidenciaSolicitacao`): GESTOR/ADMIN veem todas; OPERADOR vê se for responsável atribuído ou se abriu a solicitação, inclusive depois de encerrada; usuário inativo é recusado
- **Erros**: 404 (solicitação ou usuário não encontrado), 403 (sem acesso ou usuário inativo)

## UC-10 — Recalcular temPendenciaAberta
- **Ator**: Sistema
- **Classe**: `RecalcularPendenciaUseCase` + `SolicitacaoFinalizadaListener`
- **Disparo**: UC-02 (abertura) e UC-07 (encerramento via evento)
- **Regra**: `true` se existir solicitação não-terminal do modelo

## UC-11 — Cadastrar prestador externo
- **Ator**: Administrador
- **Classe**: `CadastrarPrestadorExternoUseCase`
- **Endpoint**: `POST /api/admin/usuarios` (com perfil=EXTERNO)
- **Regras**: Somente ADMIN; sem login (sem email/senha)

## UC-12 — Atribuir a externo e movimentar como procurador
- **Ator**: Gestor
- **Fluxo**: UC-03 (atribuir externo) → UC-04/UC-05 (movimentar como gestor)
- **Regras**: Autor real registrado na auditoria (gestor, não externo)

## UC-13 — Administração (usuários, modelos)
- **Classes**: `GerenciarUsuariosUseCase`, `GerenciarModelosUseCase`
- **Endpoints**:
  - `POST /api/admin/usuarios` — criar usuário
  - `PUT /api/admin/usuarios/{id}` — editar nome/email
  - `PATCH /api/admin/usuarios/{id}/ativar` — ativar
  - `PATCH /api/admin/usuarios/{id}/desativar` — desativar
  - `POST /api/modelos` — criar modelo (Gestor)
  - `PUT /api/modelos/{id}` — editar modelo (Gestor)
- **Regras**: ADMIN gerencia usuários; GESTOR gerencia modelos

## UC-14 — Galeria de fotos do modelo
- **Ator**: Gestor, Administrador
- **Classes**: `AdicionarFotoGaleriaUseCase`, `ListarGaleriaModeloUseCase`, `EditarFotoGaleriaUseCase`, `RemoverFotoGaleriaUseCase`
- **Endpoints**:
  - `GET /api/modelos/{id}/galeria` — listar fotos da galeria
  - `POST /api/modelos/{id}/galeria` (multipart: `file`, `identificacao`) — adicionar foto
  - `PATCH /api/modelos/{id}/galeria/{fotoId}` — editar `identificacao` e/ou marcar como `principal` (capa)
  - `DELETE /api/modelos/{id}/galeria/{fotoId}` — remover foto
- **Regras**: Gestão manual da galeria (adicionar/editar/remover) é restrita a GESTOR/ADMINISTRADOR; cada foto da galeria tem uma `identificacao` livre (ex.: qual parte do ferramental ela retrata); no máximo uma foto por modelo é `principal` (capa), imposto por índice único parcial; a primeira foto adicionada a um modelo vira `principal` automaticamente; upload fora da transação DB; ao excluir um modelo (UC-15), os objetos da galeria são removidos do storage. **Gatilho automático**: uma evidência do tipo `SERVICO_REALIZADO` ou `CONCLUSAO`, anexada como imagem (JPEG/PNG/WEBP) a uma solicitação com modelo vinculado, é automaticamente copiada (arquivo duplicado, não compartilhado) para a galeria daquele modelo — mesmo se quem anexou não tiver permissão de gestão de galeria (ver `AnexarEvidenciaUseCase`/`AdicionarFotoGaleriaUseCase.uploadAutomatico`); falha nesse gatilho é logada e nunca derruba o anexo da evidência

## UC-15 — Exclusão (hard delete)
- **Ator**: Administrador
- **Classe**: `ExcluirRegistroUseCase`
- **Endpoints**: `DELETE /api/admin/registros` (genérico)
- **Regras**: Somente ADMIN; cascata em atribuições, atividades e vínculos

## UC-16 — Ranking de métricas de tempo por modelo
- **Atores**: Gestor, Administrador
- **Classe**: `ObterMetricasPorModeloUseCase`
- **Endpoints**: `GET /api/solicitacoes/metricas/por-modelo`, `GET /api/solicitacoes/metricas/por-modelo/pdf`
- **Regras**: Agregação via SQL nativo (CTEs + `LAG()`), nunca carrega solicitações em memória; retorna, por modelo com ao menos 1 solicitação CONCLUIDA, o tempo médio de resolução e o intervalo médio entre solicitações consecutivas (nulo se houver menos de 2); ordenação (`sort=TEMPO_RESOLUCAO|INTERVALO`, `dir=asc|desc`) validada contra whitelist antes de virar SQL; exportação em PDF separada do relatório de lista de modelos
- **Erros**: 400 (`sort`/`dir` inválidos)
- **Nota**: A ficha PDF individual do modelo (`GET /api/modelos/{id}/relatorio`) exibe as mesmas duas métricas, com a mesma regra do ranking, vindas do resumo das solicitações do modelo (UC-20). Até a v1.5.0 a ficha exigia 2+ solicitações CONCLUIDA e media o intervalo só entre as concluídas; a feature `specs/007-filtros-e-resumos-para-a-paginacao` unificou o critério.

## UC-17 — SLA de solicitações (SOL-007)
- **Classe**: `Solicitacao` (agregado) — `getPrazoLimite()`, `getTempoRestanteSegundos(agora)`, `isAtrasada(agora)`, `getTempoResolucaoSegundos()`
- **Regras**: cada `PrioridadeSolicitacao` tem um prazo de SLA fixo em horas (`URGENTE`=4h, `ALTA`=24h, `MEDIA`=72h, `BAIXA`=168h); `prazoLimite = criadaEm + slaHoras(prioridade)`, calculado a partir da abertura (mesmo anchor usado no "tempo médio de resolução" do UC-16); antes da triagem (sem prioridade) todos os campos de SLA retornam nulo/`false`; `atrasada` compara com `now()` em status não-terminal, com `concluidaEm` em CONCLUIDA, e é sempre `false` em CANCELADA (trabalho cancelado não conta contra o SLA); `tempoResolucaoSegundos` só é preenchido quando CONCLUIDA
- **Exposição**: os 4 campos (`prazoLimite`, `tempoRestanteSegundos`, `atrasada`, `tempoResolucaoSegundos`) são computados em `SolicitacaoResponse.from(...)` com `Instant.now()`, portanto aparecem em toda resposta de solicitação (`GET /api/solicitacoes`, `GET /api/solicitacoes/{id}`, e nas respostas de cada transição de status) sem exigir coluna nova no banco
- **Fora de escopo por ora**: não há alerta/notificação de vencimento — os campos ficam disponíveis para o cliente decidir como destacar (ver issue #81)

## UC-18 — Lock otimista em Solicitacao (issue #80)
- **Classe**: `Solicitacao` (agregado) — campo `version`, preservado (não recalculado) em toda transição; `SolicitacaoJpaEntity` mapeia via `@Version`
- **Regras**: sem `@Version`/lock pessimista, duas transições concorrentes no mesmo card (ex.: GESTOR e OPERADOR atribuído mexendo ao mesmo tempo) podiam gerar lost update silencioso. Com `@Version`, o `UPDATE ... WHERE id = ? AND version = ?` gerado pelo Hibernate falha (0 linhas afetadas) se a linha mudou entre o load e o save da mesma transação
- **Migração**: `V7__lock_otimista_solicitacoes.sql` adiciona a coluna `version BIGINT NOT NULL DEFAULT 0`
- **Erros**: `ObjectOptimisticLockingFailureException` (Spring/Hibernate) é mapeada pelo `GlobalExceptionHandler` para 409 Conflict — "Este registro foi alterado por outro usuario. Recarregue e tente novamente."
- **Compatibilidade**: o construtor público de 16 argumentos de `Solicitacao` continua existindo (version nulo, i.e. ainda não persistida); o mapper de persistência usa o construtor de 17 argumentos para preservar o version lido do banco

## UC-19 — Solicitação de criação de modelo (tipo CRIACAO)
- **Ator**: Gestor, Administrador (restrito — diferente dos demais tipos, abertos por qualquer usuário interno)
- **Classes**: `AbrirSolicitacaoUseCase` (branch CRIACAO), `EncerrarSolicitacaoUseCase` (branch CRIACAO), `Solicitacao.abrirCriacao()`/`concluirCriacao()`, `GerenciarModelosUseCase.criar()`
- **Endpoint**: mesmos endpoints de solicitação (`POST /api/solicitacoes` com `tipo=CRIACAO`, `PATCH /api/solicitacoes/{id}/encerrar` com `concluir=true`)
- **Fluxo**: uma solicitação `CRIACAO` nasce em A_FAZER **sem `modeloId`**, carregando em vez disso os dados do modelo pretendido (`modeloCodigo`, `modeloMaquina`, `modeloObservacoes` — mesma validação de máquina do cadastro direto). Percorre o Kanban normalmente (Triagem → Em Andamento → Em Validação). Ao ser **concluída**, o sistema cria o Modelo com esses dados (mesma regra de versionamento por código+máquina do cadastro direto) e só então vincula `modeloId` à solicitação, na mesma transação — se a criação do modelo falhar, a conclusão inteira é revertida
- **Regras**: tipo da solicitação é **imutável** após a abertura (`Solicitacao.editar()` rejeita qualquer tentativa de mudança, para qualquer tipo); uma `CRIACAO` cancelada antes de concluída nunca tem modelo vinculado (`modeloId` permanece `null` para sempre); o cadastro direto de modelo (`POST /api/modelos`) continua existindo sem mudanças — este é um caminho adicional
- **Evento CADASTRO**: ao nascer um Modelo — seja pelo cadastro direto, seja pela conclusão de uma `CRIACAO` — um evento `CADASTRO` é registrado automaticamente no prontuário do modelo (`EventoModelo.criarCadastro`), com autor o usuário responsável e, quando originado de uma solicitação, `solicitacaoRelacionadaId` preenchido
- **Schema**: `V8__solicitacao_tipo_criacao.sql` torna `solicitacoes.modelo_id` nullable, adiciona `modelo_codigo`/`modelo_maquina`/`modelo_observacoes` (nullable) e um `CHECK` garantindo que só `CRIACAO` pode ter `modelo_id` nulo, e só antes de concluída
- **Erros**: 403 (perfil sem permissão para abrir CRIACAO), 422 (dados do modelo inválidos, ex. máquina inativa)

## UC-20 — Resumos de modelos
- **Atores**: qualquer usuário autenticado
- **Classes**: `ObterResumoModelosUseCase`, `ObterResumoSolicitacoesModeloUseCase`
- **Endpoints**: `GET /api/modelos/resumo`, `GET /api/modelos/{id}/solicitacoes/resumo`
- **Regras**: agregados no banco, nunca carregam a lista de modelos ou de solicitações. O resumo de modelos devolve `total`, `ativos`, `inativos`, `comPendenciaAberta` e `porMaquina` (da maior para a menor quantidade; empate em ordem alfabética); pendência e máquina contam modelos ativos e inativos. O resumo de um modelo devolve `total`, `emAberto`, `concluidas`, `canceladas`, `tempoMedioResolucaoSegundos` (média das concluídas; nulo sem concluída) e `intervaloMedioSegundos` (aberturas de todas as solicitações do modelo; nulo com menos de 2), a mesma regra do UC-16
- **Erros**: 404 (modelo inexistente no resumo de um modelo)

---

## Endpoints de Listagem (com filtros)

| Endpoint | Filtros | Paginação |
|----------|---------|-----------|
| `GET /api/solicitacoes` | `status`, `modeloId`, `tipo`, `prioridade`, `criadaEmInicio`/`criadaEmFim`, `abertaPorUsuarioId`, `responsavelId`, `maquina`, `atrasada`, `emAberto` | `page`, `size` |
| `GET /api/solicitacoes/relatorio` | mesmos filtros de `GET /api/solicitacoes` | — (Exportação em PDF) |
| `GET /api/admin/usuarios` | `perfil`, `ativo` | `page`, `size` |
| `GET /api/modelos` | `ativo`, `codigo` | `page`, `size` |

`emAberto=true` devolve só A_FAZER, EM_ANDAMENTO e EM_VALIDACAO e se soma aos outros filtros (com um `status` encerrado, a lista vem vazia); ausente ou `false` não filtra. Com `tipoData=CONCLUSAO`, `dataInicio`/`dataFim` comparam a data de encerramento: conclusão das concluídas e cancelamento das canceladas.

## Endpoints de Consulta por ID

| Endpoint | Descrição |
|----------|-----------|
| `GET /api/solicitacoes/{id}` | Detalhes da solicitação |
| `GET /api/solicitacoes/{id}/atividades` | Histórico de atividades |
| `GET /api/solicitacoes/{id}/evidencias` | Lista de evidências |
| `GET /api/admin/usuarios/{id}` | Detalhes do usuário |
| `GET /api/modelos/{id}` | Detalhes do modelo |
| `GET /api/modelos/{id}/eventos` | Eventos do modelo |

## Admin Seed

O `AdminUserInitializer` cria automaticamente o usuário admin (`admin@rgm.com` / `admin123`) ao iniciar a aplicação em **qualquer ambiente**, se não existir. Usa BCrypt via `PasswordHasher` para garantir hash correto.

## Refresh Token

- `POST /api/auth/refresh` — renova access + refresh token
- JwtFilter rejeita refresh tokens como Bearer (verifica `type=access`)

## Ações permitidas na solicitação

- **Classe**: `AcoesPermitidasSolicitacao` (domínio), usada por `ObterSolicitacaoUseCase`
- **Endpoint**: `GET /api/solicitacoes/{id}` devolve `acoesPermitidas`, calculado para o usuário autenticado
- **Fora do detalhe**: em listagens, eventos SSE e respostas de ação o campo vem `null` (não calculado); `null` não significa "nenhuma ação"

| Ação | Permitida quando |
|---|---|
| `TRIAR` | GESTOR/ADMIN, status `A_FAZER` |
| `ENVIAR_VALIDACAO` | status `EM_ANDAMENTO`, para GESTOR/ADMIN ou responsável atribuído |
| `DEVOLVER` | GESTOR/ADMIN, status `EM_VALIDACAO` |
| `ENCERRAR` | GESTOR/ADMIN, status `EM_VALIDACAO` (concluir) |
| `CANCELAR` | GESTOR/ADMIN em qualquer status não encerrado; quem abriu, só em `A_FAZER` e sem responsável |
| `EDITAR` | status não encerrado, para GESTOR/ADMIN ou quem abriu |
| `COMENTAR` | GESTOR/ADMIN sempre; quem abriu ou responsável, enquanto não encerrada |
| `ALTERAR_RESPONSAVEIS` | GESTOR/ADMIN, status não encerrado |
| `ANEXAR_EVIDENCIA` | status não encerrado, para GESTOR/ADMIN, responsável ou quem abriu (este só `ABERTURA` e `GERAL`) |

- **Regras**: usuário inativo e perfil EXTERNO não têm ação. O cálculo cobre permissão e status; não cobre pré-condições de dados, como a evidência `SERVICO_REALIZADO` exigida para enviar à validação
- **Erros**: 404 (solicitação ou usuário não encontrado)

## Eventos em tempo real (SSE)

- **Endpoint**: `GET /api/solicitacoes/events?token=<access token>` (`text/event-stream`)
- **Classes**: `SolicitacaoSseController`, `SolicitacaoEventPublisher`
- **Ao conectar**: evento `connected` com dado `ok`
- **Evento `solicitacao`**: dado JSON `{ "tipo": "<tipo>", "solicitacao": <SolicitacaoResponse> }`

| `tipo` | Publicado quando |
|---|---|
| `aberta` | uma solicitação é aberta |
| `editada` | título, descrição ou tipo são editados |
| `responsaveis_alterados` | os responsáveis são alterados |
| `triada` | a solicitação é triada |
| `enviada_validacao` | é enviada para validação |
| `devolvida` | é devolvida para correção |
| `encerrada` | é encerrada pelo endpoint de encerramento (concluída ou não) |
| `cancelada` | é cancelada |

- **Heartbeat**: a cada 25 s o servidor envia o comentário `:ping`, que o cliente ignora e que
  mantém a conexão viva atrás de proxies com tempo limite de leitura
- **Tempo limite**: a conexão expira em 30 min; o cliente reconecta
- **`responsaveis_alterados`**: a solicitação do evento traz `responsavelIds` com a lista resultante
- **Evento `solicitacao_atividade`**: dado JSON `{ "tipo": "<tipo>", "solicitacaoId": "<id>" }`, para a linha do tempo do detalhe recarregar

| `tipo` | Publicado quando |
|---|---|
| `comentada` | um comentário é registrado |
| `evidencia_adicionada` | uma evidência é anexada |

- **Acesso**: usuário inativo ou inexistente não abre a conexão (401)
