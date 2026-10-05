## 1. Levantamento de impacto

- [x] 1.1 Mapear todos os pontos do código que leem `Solicitacao.getModeloId()` assumindo não-nulo (métricas por modelo, filtro por máquina, PDF/relatórios, mapper JPA, DTOs de response) e listar em comentário de PR ou anotação temporária, para tratar em 4.x — **achados críticos**: (a) `SolicitacaoJpaRepository.findByFilters` usava `JOIN modelos` (INNER) — uma CRIACAO sem modelo nunca apareceria em NENHUMA listagem/Kanban, mesmo sem filtro de máquina; corrigido para `LEFT JOIN`. (b) `SolicitacaoFinalizadaEvent` fazia `requireNonNull(modeloId, ...)` — cancelar uma CRIACAO sem modelo lançava `NullPointerException` (500); corrigido para aceitar `null`, e `SolicitacaoFinalizadaListener` passou a pular o recálculo de pendência quando `modeloId` é nulo. Ambos encontrados via smoke test real, não só testes unitários.
- [x] 1.2 Confirmar em `ModeloRepositoryAdapter`/`SolicitacaoRepositoryAdapter` como `maquina` é validada contra o catálogo hoje, para reaproveitar exatamente a mesma validação na abertura de CRIACAO — reaproveitada via `MaquinaRepository.existsByNomeAndAtivoTrue`, mesma usada em `GerenciarModelosUseCase.validarMaquina`

## 2. Domínio

- [x] 2.1 Adicionar `CRIACAO` a `TipoSolicitacao` e `CADASTRO` a `TipoEventoModelo`, com teste cobrindo os novos valores
- [x] 2.2 Alterar `Solicitacao`: `modeloId` passa a ser obrigatório apenas quando `tipo != CRIACAO`; adicionar campos `modeloCodigo`, `modeloMaquina`, `modeloObservacoes` (nullable, obrigatórios só quando `tipo == CRIACAO`); atualizar `validateInvariants()` para refletir a regra; cobrir com `SolicitacaoTest` (casos válidos e inválidos dos dois lados)
- [x] 2.3 Bloquear alteração de `tipo` em `Solicitacao.editar(...)`, com teste cobrindo a rejeição

## 3. Persistência

- [x] 3.1 Criar migration Flyway (`V8__solicitacao_tipo_criacao.sql`): `ALTER COLUMN modelo_id DROP NOT NULL`, novas colunas nullable (`modelo_codigo`, `modelo_maquina`, `modelo_observacoes`), `CHECK` de consistência tipo/modelo_id/status — validada contra o Postgres real do `make up` (Flyway aplicou com sucesso, schema v8)
- [x] 3.2 Atualizar `SolicitacaoJpaEntity`, mapper e `SolicitacaoRepositoryAdapter` para os novos campos e para `modelo_id` nullable; cobrir com `MapperTest`/`MapperRoundtripTest` e `SolicitacaoRepositoryAdapterTest`

## 4. Casos de uso

- [x] 4.1 `AbrirSolicitacaoUseCase`: novo branch para `tipo = CRIACAO` — validar perfil (GESTOR/ADMINISTRADOR), validar dados do modelo (reaproveitando validação de máquina), criar a solicitação sem `modeloId`; teste cobrindo sucesso, OPERADOR rejeitado, dados inválidos rejeitados
- [x] 4.2 `EncerrarSolicitacaoUseCase`: injetar `GerenciarModelosUseCase`; ao concluir uma CRIACAO, chamar `criar(...)` com os dados carregados, obter o `modeloId` resultante, persistir a solicitação já com `modeloId` e `CONCLUIDA`; teste cobrindo sucesso e falha de criação do modelo (conclusão inteira revertida)
- [x] 4.3 `GerenciarModelosUseCase.criar()` e o novo branch de `EncerrarSolicitacaoUseCase` chamam a mesma criação de `EventoModelo` tipo CADASTRO (`EventoModelo.criarCadastro`); teste cobrindo os dois gatilhos, com e sem `solicitacaoRelacionadaId`
- [x] 4.4 `EditarSolicitacaoUseCase`: `tipo` permanece no `Input`, mas o domínio (`Solicitacao.editar`) rejeita qualquer valor diferente do atual; teste cobrindo que uma tentativa de mudar o tipo é rejeitada

## 5. API

- [x] 5.1 Atualizar `AbrirSolicitacaoRequest`/`SolicitacaoResponse` em `SolicitacaoController` para aceitar/expor os campos condicionais de CRIACAO e `modeloId` opcional

## 6. Regressão e qualidade

- [x] 6.1 Rodar a suíte completa (exceto `FlywayMigrationTest`, que exige Docker-in-Docker indisponível neste ambiente — validado em compensação rodando a migration contra o Postgres real do `make up`) — 545 testes, 0 falhas
- [x] 6.2 Confirmar cobertura JaCoCo >= 95% nos arquivos alterados — `mvnw verify`: "All coverage checks have been met"
- [x] 6.3 Validar manualmente via `make up` + API real: abrir CRIACAO como GESTOR (sucesso), como OPERADOR (403), aparecer na listagem sem modelo (confirma fix do LEFT JOIN), triar → enviar validação (sem exigir evidência SERVICO_REALIZADO) → concluir → Modelo criado com os dados corretos, `modeloId` vinculado, evento CADASTRO no histórico com `solicitacaoRelacionadaId` preenchido; cadastro direto de modelo também gera CADASTRO (sem `solicitacaoRelacionadaId`); cancelar uma CRIACAO sem modelo funciona (regressão do bug do item 1.1); tentativa de editar `tipo` via API rejeitada com 422

## 7. Documentação e próximos passos

- [x] 7.1 Atualizar `docs/casos-de-uso.md` (novo UC-19, notas em UC-02/UC-07/UC-14/UC-18) e `docs/diagramas.md` (enums `TipoSolicitacao`/`TipoEventoModelo`, campos novos em `Solicitacao`)
- [x] 7.2 Implementado diretamente no repo `rgm-frontend` (fora de uma change formal do OpenSpec, a pedido do usuário para fechar todas as pontas soltas): tipo `CRIACAO` no union/enum, `modeloId` nullable, campos `modeloCodigo`/`modeloMaquina`/`modeloObservacoes`, formulário de abertura com campos condicionais restrito a GESTOR/ADMINISTRADOR (`canAbrirSolicitacaoCriacao`), card do Kanban sem quebrar quando não há modelo, tipo imutável no formulário de edição, evidência não mais obrigatória em `EnviarValidacaoModal` para CRIACAO. Um bug de crash real (`KanbanCard` quebrava com `tipo=CRIACAO`) foi encontrado e corrigido nesse processo — ver auditoria e correções no repo `rgm-frontend`.
