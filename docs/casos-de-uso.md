# Casos de Uso — RGM Backend

Referência de todos os casos de uso implementados no sistema.

## UC-01 — Logar no sistema
- **Atores**: Operador, Gestor, Administrador
- **Classe**: `LoginUseCase`
- **Endpoint**: `POST /api/auth/login`
- **Regras**: EXTERNO não faz login; valida credenciais + status ativo
- **Erros**: 401 (credenciais inválidas ou inativo)

## UC-02 — Abrir solicitação (A_FAZER)
- **Ator**: Operador (Gestor também pode)
- **Classe**: `AbrirSolicitacaoUseCase`
- **Endpoint**: `POST /api/solicitacoes`
- **Regras**: Modelo deve existir e estar ativo; EXTERNO não pode abrir; registra atividade ABERTURA; atualiza `temPendenciaAberta`
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
- **Regras**: Comentário final **obrigatório**; publica `SolicitacaoFinalizadaEvent`
- **Erros**: 422 (sem comentário), 403 (sem permissão), 409 (status inválido)

## UC-08 — Anexar evidência (upload)
- **Atores**: Operador, Gestor, Administrador
- **Classe**: `AnexarEvidenciaUseCase`
- **Endpoint**: `POST /api/solicitacoes/{id}/evidencias`
- **Regras**: Valida MIME type (imagens, PDF ou vídeo MP4) e tamanho (max 10MB); armazena publicUrl persistente; upload fora da transação DB
- **Erros**: 422 (tipo/tamanho inválido), 500 (MinIO indisponível)

## UC-09 — Visualizar evidências
- **Atores**: Operador, Gestor, Administrador
- **Classe**: `VisualizarEvidenciaUseCase`
- **Endpoint**: `GET /api/solicitacoes/{id}/evidencias`
- **Regras**: Valida acesso (GESTOR/ADMIN veem todas; OPERADOR só se atribuído)
- **Erros**: 404 (solicitação não encontrada), 403 (sem acesso)

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
- **Regras**: Galeria é independente do histórico de evidências — fotos de evidência (anexadas a Solicitação/EventoModelo) nunca são promovidas automaticamente à galeria; cada foto da galeria tem uma `identificacao` livre (ex.: qual parte do ferramental ela retrata); no máximo uma foto por modelo é `principal` (capa), imposto por índice único parcial; a primeira foto adicionada a um modelo vira `principal` automaticamente; upload fora da transação DB; ao excluir um modelo (UC-15), os objetos da galeria são removidos do storage

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
- **Nota**: A ficha PDF individual do modelo (`GET /api/modelos/{id}/pdf`) também passou a exibir as mesmas duas métricas, calculadas em memória a partir das solicitações já carregadas do modelo (exige 2+ solicitações CONCLUIDA); o critério difere levemente do ranking (que considera intervalos entre TODAS as solicitações, não só as concluídas), decisão documentada no OpenSpec change `metricas-tempo-por-modelo`.

---

## Endpoints de Listagem (com filtros)

| Endpoint | Filtros | Paginação |
|----------|---------|-----------|
| `GET /api/solicitacoes` | `status`, `modeloId`, `tipo`, `prioridade`, `criadaEmInicio`/`criadaEmFim`, `abertaPorUsuarioId`, `responsavelId`, `maquina` | `page`, `size` |
| `GET /api/solicitacoes/relatorio` | mesmos filtros de `GET /api/solicitacoes` | — (Exportação em PDF) |
| `GET /api/admin/usuarios` | `perfil`, `ativo` | `page`, `size` |
| `GET /api/modelos` | `ativo`, `codigo` | `page`, `size` |

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
