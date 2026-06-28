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

## UC-14 — Atualizar foto capa do modelo
- **Ator**: Gestor, Administrador
- **Classe**: `AtualizarFotoCapaUseCase`
- **Endpoints**: `POST /api/modelos/{id}/foto-capa` (upload), `PATCH /api/modelos/{id}/foto-capa` (usar evidência)
- **Regras**: Evidência deve pertencer ao mesmo modelo; upload fora da transação DB

## UC-15 — Exclusão (hard delete)
- **Ator**: Administrador
- **Classe**: `ExcluirRegistroUseCase`
- **Endpoints**: `DELETE /api/admin/registros` (genérico)
- **Regras**: Somente ADMIN; cascata em atribuições, atividades e vínculos

---

## Endpoints de Listagem (com filtros)

| Endpoint | Filtros | Paginação |
|----------|---------|-----------|
| `GET /api/solicitacoes` | `status`, `modeloId` | `page`, `size` |
| `GET /api/solicitacoes/exportar` | `status`, `modeloId` | — (Exportação de arquivo CSV) |
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
