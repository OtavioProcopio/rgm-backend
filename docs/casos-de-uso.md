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
- **Atores**: Gestor, Administrador. Os quatro endpoints de indicadores (`GET /api/solicitacoes/metricas`, `/metricas/historico`, `/metricas/por-modelo` e `/metricas/por-modelo/pdf`) respondem só a esses perfis; Operador recebe 403 (antes da spec 010, respondiam 200 a qualquer autenticado)
- **Classe**: `ObterMetricasPorModeloUseCase`
- **Endpoints**: `GET /api/solicitacoes/metricas/por-modelo`, `GET /api/solicitacoes/metricas/por-modelo/pdf`
- **Regras**: Agregação via SQL nativo (CTEs + `LAG()`), nunca carrega solicitações em memória; retorna, por modelo com ao menos 1 solicitação CONCLUIDA, o tempo médio de resolução e o intervalo médio entre solicitações consecutivas (nulo se houver menos de 2); ordenação (`sort=TEMPO_RESOLUCAO|INTERVALO`, `dir=asc|desc`) validada contra whitelist antes de virar SQL; exportação em PDF separada do relatório de lista de modelos
- **Erros**: 400 (`sort`/`dir` inválidos; `size` fora de 1..100), 403 (operador)
- **Solicitação de criação (spec 011)**: o pedido do tipo `CRIACAO` conta no total de solicitações do modelo, mas **não entra** no tempo médio de resolução nem no intervalo entre solicitações (nem no ranking, nem no resumo): esses números medem manutenção do modelo. Modelo com só a criação concluída não aparece no ranking
- **Histórico (`/metricas/historico`)**: os dias e as semanas são calculados no fuso da fábrica (`rgm.fabrica.fuso`, padrão `America/Sao_Paulo`; semana de segunda a domingo), `dias` aceita de 1 a 365 (400 fora disso) e a série vem de consulta agregada, com o filtro de modelo na consulta. O formato da resposta não mudou; continua agrupando por data de **criação**. Para concluídas pela data de conclusão use o painel (UC-23)
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
- **Regras**: agregados no banco, nunca carregam a lista de modelos ou de solicitações. O resumo de modelos devolve `total`, `ativos`, `inativos`, `comPendenciaAberta` e `porMaquina` (da maior para a menor quantidade; empate em ordem alfabética); pendência e máquina contam modelos ativos e inativos. O resumo de um modelo devolve `total`, `emAberto`, `concluidas`, `canceladas`, `tempoMedioResolucaoSegundos` (média das concluídas; nulo sem concluída) e `intervaloMedioSegundos` (aberturas das solicitações de manutenção do modelo, sem a de criação; nulo com menos de 2), a mesma regra do UC-16; `total` continua contando todas, inclusive a de criação
- **Erros**: 404 (modelo inexistente no resumo de um modelo)

## UC-21 — Visibilidade do operador
- **Atores**: Operador
- **Classes**: `AcessoSolicitacao` (domínio), `ListarSolicitacoesUseCase`, `ObterSolicitacaoUseCase`, `ListarAtividadesUseCase`, `ResolverDestinatariosEventoUseCase`
- **Regras**: o operador tem acesso à solicitação que abriu ou da qual é responsável ativo; gestor e administrador têm acesso a todas. A mesma regra vale na listagem, no relatório em PDF, no detalhe, no histórico, nas evidências e nos eventos de tempo real. Na listagem, os filtros `abertaPorUsuarioId` e `responsavelId` enviados pelo operador restringem dentro desse conjunto, sem ampliá-lo. Nos eventos, quem é removido de responsável recebe o evento dessa mudança e mais nenhum
- **Erros**: 403 (operador consulta detalhe ou histórico de solicitação alheia)

## UC-22 — Senha e sessão
- **Atores**: qualquer usuário que faz login; Administrador na redefinição
- **Classes**: `PoliticaSenha` (domínio), `AutenticarAcessoUseCase`, `AlterarSenhaPropriaUseCase`, `GerenciarUsuariosUseCase`, `RefreshTokenUseCase`
- **Regras**: senha com no mínimo 8 caracteres ao criar usuário, trocar a própria senha e redefinir a de outro; perfil que faz login não é criado sem senha. Cada usuário tem uma versão de credencial (`usuarios.versao_credencial`, migração `V10`) que os tokens de acesso e de renovação carregam; trocar ou redefinir a senha soma 1, e token com versão diferente da atual é recusado na chamada comum, na abertura do tempo real e na renovação. Token emitido antes da `V10` vale como versão 0. As conexões de tempo real abertas do usuário são encerradas na troca. `PATCH /api/usuarios/me/senha` devolve `token` e `refreshToken` novos, para a sessão que trocou continuar. Mudar o perfil não invalida tokens: a autoridade de cada chamada vem do perfil atual do usuário, não do perfil gravado no token
- **Erros**: 400 (senha curta ou ausente), 401 (token de versão antiga)

## UC-23 — Painel de métricas por período
- **Atores**: Gestor, Administrador. Operador recebe 403 e anônimo 401
- **Classe**: `ObterPainelPorPeriodoUseCase`
- **Endpoint**: `GET /api/solicitacoes/metricas/painel?dias=7|30|90&modeloId=` (`dias` padrão 30; `modeloId` opcional)
- **Resposta**: `periodo` e `anterior`, cada um com `inicio`, `fim` (janela `[inicio, fim)`, `dias` dias locais inteiros), `criadas` (pela data de criação), `concluidas` (pela data de **conclusão**), `canceladas` (pela data de cancelamento), `tempoMedioResolucaoSegundos` (média das concluídas na janela, em segundos com uma casa; nulo sem concluída; 30 minutos = 1800,0) e `serie`. Cada ponto da série (um por dia; por semana de segunda a domingo quando `dias` > 30) traz `rotulo`, `criadas`, `concluidas`, `canceladas`, `abertasAoFimDoDia` e `atrasadasAoFimDoDia`. Só `periodo` traz também `abertasAgora` e `atrasadasAgora`. O `anterior` é a janela de mesmo tamanho imediatamente antes
- **Regras**: agregado no banco, nunca carrega solicitação em memória; dias no fuso da fábrica (`rgm.fabrica.fuso`). "Aberta ao fim do dia" = criada até o fim do dia e ainda não concluída nem cancelada nesse instante. "Atrasada ao fim do dia" = aberta nesse instante e com o prazo (criação + horas da prioridade) já vencido; **usa a prioridade de hoje** (aproximação aceita: se a prioridade mudou depois, o atraso de dias passados reflete a atual). Solicitação sem prioridade nunca é atrasada. Concluir e cancelar contam no dia em que aconteceram, não no dia da criação
- **Compatibilidade**: rota nova e aditiva; `/metricas` e `/metricas/historico` não mudam de formato (`slaMediaHoras` e `slaGlobalMediaHoras` seguem como tempo de resolução em horas inteiras, mantidos para o frontend v1.5.0)
- **Erros**: 400 (`dias` diferente de 7, 30 e 90), 401, 403

## Limites de texto

Declarados em `LimitesTexto` (domínio) e aplicados nos requests; texto acima do limite responde 400 com o campo e o limite.

| Campo | Limite |
|---|---|
| Título da solicitação | 255 |
| Descrição da solicitação; comentário; motivo de cancelamento e de devolução; comentário de encerramento | 2000 |
| Comentário do envio para validação | 1000 |
| Modelo: código / descrição / máquina / observações | 100 / 255 / 255 / 2000 |
| Modelo pretendido (solicitação de criação): código / máquina / observações | 50 / 100 / 2000 |
| Nome da máquina; nome e e-mail do usuário | 255 |

---

## Endpoints de Listagem (com filtros)

| Endpoint | Filtros | Paginação |
|----------|---------|-----------|
| `GET /api/solicitacoes` | `status`, `modeloId`, `tipo`, `prioridade`, `criadaEmInicio`/`criadaEmFim`, `abertaPorUsuarioId`, `responsavelId`, `maquina`, `atrasada`, `emAberto`, `q` (busca textual); ordenação `sort` (`criadaEm`, `prazoLimite`, `prioridade`, `concluidaEm`) e `dir` (`asc`, `desc`) | `page`, `size` |
| `GET /api/solicitacoes/relatorio` | mesmos filtros de `GET /api/solicitacoes` | — (Exportação em PDF) |
| `GET /api/admin/usuarios` | `perfil`, `ativo` | `page`, `size` |
| `GET /api/modelos` | `ativo`, `codigo` | `page`, `size` |

`emAberto=true` devolve só A_FAZER, EM_ANDAMENTO e EM_VALIDACAO e se soma aos outros filtros (com um `status` encerrado, a lista vem vazia); ausente ou `false` não filtra. Com `tipoData=CONCLUSAO`, `dataInicio`/`dataFim` comparam a data de encerramento: conclusão das concluídas e cancelamento das canceladas.

**Paginação**: `size` aceita de 1 a 100 (padrão 20) e `page` é a partir de 0; valor fora do intervalo responde 400 com mensagem que diz o intervalo. Vale para `GET /api/solicitacoes`, `GET /api/solicitacoes/metricas/por-modelo`, `GET /api/modelos` e `GET /api/admin/usuarios`.

## Convenções de erro

- **Entrada inválida**: corpo JSON malformado, data fora de ISO-8601 e enumeração inválida (status, tipo, prioridade, tipoData e perfil) respondem 400, com o nome do campo e o formato ou os valores aceitos; nunca 500.
- **Segurança**: as respostas 401 (sem credencial válida) e 403 (autenticado sem permissão) trazem corpo JSON no formato padrão de erro (`status`, `error`, `message`, `timestamp`). Operador autenticado sem permissão recebe 403, e não 401.

## Exposição operacional

Spec `specs/010-seguranca-e-robustez-da-api-permissoes`:

- Só `GET /actuator/health` é anônimo.
- `/actuator/prometheus` e o índice do actuator (`/actuator`) exigem HTTP Basic da conta de coleta definida por `MONITORAMENTO_COLETA_USUARIO` e `MONITORAMENTO_COLETA_SENHA`; sem a senha configurada, ninguém coleta.
- A interface Swagger UI não é servida em produção.

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

O `AdminUserInitializer` cria o primeiro administrador ao iniciar a aplicação, se o e-mail não existir. Em `dev` e `test` vale `admin@rgm.com` / `admin123`; em `prod` o e-mail e a senha vêm de `ADMIN_EMAIL` e `ADMIN_SENHA` (sem senha válida, nada é criado). Usa BCrypt via `PasswordHasher`.

## Refresh Token

- `POST /api/auth/refresh` — renova access + refresh token
- JwtFilter rejeita refresh tokens como Bearer (verifica `type=access`)
- Token de renovação emitido antes da última troca de senha é recusado (ver UC-22)

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

**Ordenação (`sort`, `dir`):** sem `sort` a ordem é a criação crescente (a mais antiga primeiro); `dir` sem `sort` ordena por criação. `prazoLimite` ordena pelo prazo da prioridade (crescente = a mais vencida primeiro); `prioridade` ordena por urgência (crescente = URGENTE primeiro). Solicitação sem prioridade (não triada) vai sempre para o fim, em qualquer sentido; o desempate é por criação e id, então as páginas não repetem nem perdem item. `concluidaEm` ordena pela data de encerramento (conclusão das concluídas, cancelamento das canceladas); as não encerradas vão para o fim. Valor fora da lista responde 400 com os valores aceitos.

**Busca (`q`):** de 2 a 100 caracteres (fora disso, 400); casa, sem diferenciar caixa nem acento, com título, descrição e código do modelo, e se soma aos demais filtros e à visibilidade do operador.

**Modelo na resposta:** a listagem e o detalhe trazem `modeloCodigo`, `modeloMaquina` e `modeloVersao` do cadastro do modelo (uma consulta por página); solicitação de criação ainda sem modelo mantém o código e a máquina informados na abertura.

**Responsáveis disponíveis:** `GET /api/solicitacoes/responsaveis-disponiveis?busca=&page=&size=` (gestor e administrador; operador recebe 403) lista usuários ativos de perfil atribuível (operador e gestor) por nome, sem diferenciar caixa nem acento.

**Distribuição em aberto:** `GET /api/solicitacoes/metricas` traz também `emAbertoPorTipo` e `emAbertoPorPrioridade` (`SEM_PRIORIDADE` para a não triada), só das solicitações em aberto.

**Regras da spec 012:** edição só do gestor e do administrador depois da triagem (o operador edita em A Fazer) e cada edição entra no histórico como comentário com o título e a descrição anteriores; troca de responsáveis só depois da triagem; cancelar por `cancelar` ou por `encerrar` com `concluir=false` grava o mesmo histórico; o nome do objeto da evidência é gerado pelo servidor e o conteúdo precisa corresponder ao tipo declarado (`image/jpeg`, `image/png`, `image/gif`, `image/webp`, `application/pdf`, `video/mp4`).

## Catálogo de tipos de modelo (spec 012)

Espelha o catálogo de máquinas. `GET /api/tipos-modelo` lista (qualquer usuário autenticado);
`POST /api/admin/tipos-modelo`, `PUT /api/admin/tipos-modelo/{id}` (renomear),
`PATCH /api/admin/tipos-modelo/{id}/ativar` e `/desativar` são do administrador. Ao criar ou editar
um modelo, `tipo` (opcional) precisa estar no catálogo e ativo; editar sem trocar o tipo continua
valendo mesmo que ele tenha sido desativado. Renomear um tipo (ou uma máquina) troca o nome nos
modelos que o usam.
