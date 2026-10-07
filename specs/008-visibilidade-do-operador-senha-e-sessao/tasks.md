# Tarefas — Visibilidade do operador, senha e sessão seguras e limites de texto

> Ordem de dependência. `[P]` marca tarefa paralelizável (não toca arquivo de outra `[P]`
> da mesma fase). Teste vem antes da implementação que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`.

Checklists: liberados pelo usuário em 2026-10-07 ("termina a 8"), sem revisão item a item;
as caixas ficam desmarcadas.

Desvio herdado (feature 001): o projeto não tem `app/tests/bdd` nem alvo `make bdd`. Os
critérios de aceite da spec são provados pelos testes de caso de uso, de controller e de
repositório listados abaixo, não por cenários executáveis em Gherkin.

## Fase 1 — Domínio

- [x] T001 [P] Criar `TEST/core/domain/validation/AcessoSolicitacaoTest.java`: gestor e administrador veem; operador que abriu vê; operador responsável vê; operador sem relação não vê; inativo não vê; `validarLeitura` lança `NaoAutorizadoException`
- [x] T002 [P] Criar `MAIN/core/domain/validation/AcessoSolicitacao.java`; alterar `MAIN/core/domain/validation/AcessoEvidenciaSolicitacao.java` para delegar a ela (o teste `TEST/core/domain/validation/AcessoEvidenciaSolicitacaoTest.java` continua verde sem alteração)
- [x] T003 [P] Criar `TEST/core/domain/validation/PoliticaSenhaTest.java` (7 recusa com mensagem citando 8; 8 aceita; nula e em branco recusam) e `MAIN/core/domain/validation/PoliticaSenha.java`
- [x] T004 [P] Criar `MAIN/core/domain/validation/LimitesTexto.java` com as constantes da tabela de limites e a mensagem única
- [x] T005 [P] Testes em `TEST/core/domain/model/UsuarioTest.java` (usuário novo nasce na versão 0; `withSenha` soma 1; `alterarPerfil`, `editar` e `withAtivo` preservam a versão) e alterar `MAIN/core/domain/model/aggregates/Usuario.java`
- [x] T006 Criar `MAIN/core/domain/ports/services/CredencialToken.java`; alterar `MAIN/core/domain/ports/services/AccessTokenIssuer.java` (`validateAccessToken`; `validateRefreshToken` devolve `CredencialToken`) e `MAIN/core/domain/ports/repositories/SolicitacaoRepository.java` (`visivelParaUsuarioId` em `findByFilters`)

## Fase 2 — Aplicação

- [x] T007 [P] Testes em `TEST/core/application/usecases/solicitacao/ListarSolicitacoesUseCaseTest.java` (operador sem filtro usa a consulta de filtros com a visibilidade dele; operador não tem `responsavelId` forçado; filtros `abertaPor` e `responsavel` do operador passam como vieram; gestor e administrador sem visibilidade) e alterar `MAIN/core/application/usecases/solicitacao/ListarSolicitacoesUseCase.java`
- [x] T008 [P] Testes em `TEST/core/application/usecases/solicitacao/ObterSolicitacaoUseCaseTest.java` (operador sem acesso recebe `NaoAutorizadoException`; quem abriu, responsável e gestor recebem) e alterar `MAIN/core/application/usecases/solicitacao/ObterSolicitacaoUseCase.java`
- [x] T009 [P] Testes em `TEST/core/application/usecases/solicitacao/ListarAtividadesUseCaseTest.java` (mesmos quatro casos) e alterar `MAIN/core/application/usecases/solicitacao/ListarAtividadesUseCase.java` para receber o usuário
- [x] T010 [P] Criar `TEST/core/application/usecases/solicitacao/ResolverDestinatariosEventoUseCaseTest.java` (gestor recebe; operador que abriu recebe; responsável recebe; sem relação não recebe; removido informado como "tinha acesso antes" recebe; sem conectados não lê nada; solicitação inexistente devolve vazio) e `MAIN/core/application/usecases/solicitacao/ResolverDestinatariosEventoUseCase.java`
- [x] T011 [P] Testes em `TEST/core/application/usecases/solicitacao/GerenciarResponsaveisUseCaseTest.java` (saída traz os responsáveis removidos; vazia quando só acrescenta) e alterar `MAIN/core/application/usecases/solicitacao/GerenciarResponsaveisUseCase.java`
- [x] T012 [P] Criar `TEST/core/application/usecases/auth/AutenticarAcessoUseCaseTest.java` (token válido devolve o usuário; token inválido, usuário inexistente, inativo e versão diferente lançam `NaoAutorizadoException`) e `MAIN/core/application/usecases/auth/AutenticarAcessoUseCase.java`
- [x] T013 [P] Testes em `TEST/core/application/usecases/auth/RefreshTokenUseCaseTest.java` (versão antiga recusada; versão atual renova) e alterar `MAIN/core/application/usecases/auth/RefreshTokenUseCase.java`
- [x] T014 [P] Testes em `TEST/core/application/usecases/auth/AlterarSenhaPropriaUseCaseTest.java` (senha de 7 recusada sem tocar o repositório; troca devolve acesso e renovação emitidos para o usuário já salvo) e alterar `MAIN/core/application/usecases/auth/AlterarSenhaPropriaUseCase.java`
- [x] T015 [P] Testes em `TEST/core/application/usecases/admin/GerenciarUsuariosUseCaseTest.java` (criar com senha de 7 e sem senha recusa; redefinir com 7 recusa; redefinir soma a versão; alterar perfil não soma) e alterar `MAIN/core/application/usecases/admin/GerenciarUsuariosUseCase.java`

## Fase 3 — Adapters e infra

- [x] T016 Criar `app/src/main/resources/db/migration/V10__versao_credencial_usuario.sql`; alterar `MAIN/adapter/out/persistence/entity/UsuarioJpaEntity.java` e `MAIN/adapter/out/persistence/mapper/UsuarioMapper.java`; ajustar `TEST/adapter/out/persistence/mapper/MapperTest.java` e `MapperRoundtripTest.java` (a versão vai e volta)
- [x] T017 Testes em `TEST/adapter/out/persistence/repository/SolicitacaoJpaRepositoryPostgresTest.java` (visibilidade nula traz tudo; operador recebe as que abriu e as de que é responsável ativo, com total certo; responsável removido não conta; filtro por outro usuário devolve vazio; `abertaPor` do próprio restringe) e alterar `MAIN/adapter/out/persistence/repository/SolicitacaoJpaRepository.java`
- [x] T018 Testes em `TEST/adapter/out/persistence/SolicitacaoRepositoryAdapterTest.java` e alterar `MAIN/adapter/out/persistence/SolicitacaoRepositoryAdapter.java`
- [x] T019 Testes em `TEST/adapter/out/security/JwtAccessTokenIssuerTest.java` (os dois tokens carregam a versão; acesso válido é lido; renovação usada como acesso é recusada; token sem versão vale 0) e alterar `MAIN/adapter/out/security/JwtAccessTokenIssuer.java`
- [x] T020 Testes em `TEST/adapter/out/security/JwtAuthenticationFilterTest.java` (autoridade vem do perfil atual do usuário; credencial recusada segue sem autenticação) e alterar `MAIN/adapter/out/security/JwtAuthenticationFilter.java`
- [x] T021 Testes em `TEST/adapter/in/web/solicitacao/SolicitacaoEventPublisherTest.java` (evento vai só aos destinatários resolvidos; sem conexão não consulta; heartbeat vai a todos; encerrar conexões de um usuário fecha só as dele) e alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoEventPublisher.java`
- [x] T022 Testes em `TEST/adapter/in/web/solicitacao/SolicitacaoSseControllerTest.java` (token ausente e credencial recusada respondem 401; aceita registra a conexão com o usuário) e alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoSseController.java`; tirar a classe da exclusão do JaCoCo em `app/pom.xml`
- [x] T023 Testes em `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java` (histórico repassa o usuário; 403 no detalhe e no histórico; publicações informam a solicitação; troca de responsáveis informa os removidos; um caso acima do limite por campo de texto e título no limite aceito) e alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java` e os requests `AbrirSolicitacaoRequest`, `EditarSolicitacaoRequest`, `ComentarioRequest`, `CancelarSolicitacaoRequest`, `DevolverSolicitacaoRequest`, `EncerrarSolicitacaoRequest` em `MAIN/adapter/in/web/dto/request/`
- [x] T024 [P] Testes em `TEST/adapter/in/web/evidencia/EvidenciaControllerTest.java` e alterar `MAIN/adapter/in/web/evidencia/EvidenciaController.java` (publicação informa a solicitação)
- [x] T025 [P] Testes em `TEST/adapter/in/web/usuario/UsuarioControllerTest.java` (resposta da troca traz `token` e `refreshToken` e os campos de antes; conexões do usuário encerradas), criar `MAIN/adapter/in/web/dto/response/SenhaAlteradaResponse.java` e alterar `MAIN/adapter/in/web/usuario/UsuarioController.java`
- [x] T026 [P] Testes em `TEST/adapter/in/web/admin/AdminControllerTest.java` (redefinição encerra as conexões do alvo; nome e e-mail acima do limite e e-mail fora do formato respondem 400) e alterar `MAIN/adapter/in/web/admin/AdminController.java`, `CriarUsuarioRequest` e `EditarUsuarioRequest`
- [x] T027 [P] Testes em `TEST/adapter/in/web/modelo/ModeloControllerTest.java` e `TEST/adapter/in/web/maquina/MaquinaAdminControllerTest.java` (um caso acima do limite por campo) e alterar `CriarModeloRequest`, `EditarModeloRequest`, `CriarMaquinaRequest`, `EditarMaquinaRequest`
- [x] T028 [P] Testes em `TEST/adapter/config/GlobalExceptionHandlerTest.java` (valor longo demais vindo do banco responde 400 sem falar em duplicata; violação de unicidade continua 409) e alterar `MAIN/adapter/config/GlobalExceptionHandler.java`
- [x] T029 Alterar `MAIN/adapter/config/UseCaseConfig.java`: registrar `AutenticarAcessoUseCase` e `ResolverDestinatariosEventoUseCase`; `AlterarSenhaPropriaUseCase` recebe o emissor; ajustar `TEST/adapter/in/web/WebMvcTestConfig.java` se a fatia web precisar dos beans novos

## Fase 4 — Integração e BDD

- [x] T030 Atualizar `docs/casos-de-uso.md` e a tabela de `openspec/README.md`
- [x] T031 `make validate` verde e `make test-all` verde

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01 | T001, T002 |
| RF-02 | T007, T017, T018 |
| RF-03 | T007, T017 |
| RF-04 | T008, T023 |
| RF-05 | T009, T023 |
| RF-06 | T007, T017 |
| RF-07 | T001, T007, T008, T009 |
| RF-08 | T003, T014, T015 |
| RF-09 | T003, T015 |
| RF-10 | T026 |
| RF-11 | T005, T012, T013, T019, T020, T022 |
| RF-12 | T012, T019 |
| RF-13 | T012, T020 |
| RF-14 | T004, T023, T026, T027 |
| RF-15 | T023 |
| RF-16 | T028 |
| RF-17 | T023, T028 |
| RF-18 | T010, T021, T023, T024 |
| RF-19 | T014, T025 |
| RF-20 | T010, T011, T021, T023 |
| RF-21 | T021, T025, T026 |
| RF-22 | T005, T015 |
| RNF-01 | T007, T017 |
| RNF-02 | T012, T020 |
| RNF-03 | T025, T031 |
| RNF-04 | T022, T031 |
| RNF-05 | T003, T004 |
| RNF-06 | T020 |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.
