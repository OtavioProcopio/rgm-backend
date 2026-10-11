# Tarefas — Correção dos bugs abertos do backend e melhorias para o frontend

> Ordem de dependência. `[P]` marca tarefa paralelizável (não toca arquivo de outra `[P]`
> da mesma fase). Teste vem antes da implementação que ele prova.

Prefixos: `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` = `app/src/test/java/com/rgm/api`,
`RES` = `app/src/main/resources`.

Desvio herdado (feature 001): o projeto não tem `app/tests/bdd` nem alvo `make bdd`. Os
critérios de aceite da spec são provados por testes de caso de uso, de controller e de consulta
em Postgres, listados abaixo; a rastreabilidade mostra qual teste cobre qual cenário.

Todo teste novo segue o Princípio 3: `// Arrange`, `// Act`, `// Assert`, um comportamento por
teste, nome `shouldXWhenY`. As consultas SQL e a concorrência são provadas em `*PostgresTest`.
Nenhuma tarefa é `[P]`: `UseCaseConfig.java` e `SolicitacaoController.java` entram em várias
fases e seriam conflito de escrita.

## Fase 1 — Esquema do banco (RF-10, RF-11, RF-23, RF-25, RNF-08)

- [x] T001 Estender `TEST/adapter/out/persistence/FlywayMigrationTest.java`: com duplicatas `(maquina, codigo, versao)` antes da V11, depois dela nenhuma repete e nenhum modelo some nem muda de código ou máquina; o modelo mais antigo de cada grupo mantém a versão; modelo sem duplicata mantém a versão (inclusive com lacuna); o índice único recusa inserção repetida; `rgm_sem_acento('Zélia ÇÃO')` devolve `zelia cao` em minúsculas e sem acento
- [x] T002 Criar `RES/db/migration/V11__identificacao_unica_do_modelo_e_busca.sql`: saneia duplicatas, cria o índice único `(maquina, codigo, versao)` e a função `rgm_sem_acento(text)` imutável (T001 verde)

## Fase 2 — Segurança: administrador, login e upload (RF-01 a RF-09)

- [x] T003 Criar `TEST/adapter/config/AdminUserInitializerTest.java`: `prod` sem `ADMIN_SENHA` não salva e avisa; `prod` com senha válida salva com o e-mail configurado e o hash da senha; `prod` com `admin123` não salva e avisa; `prod` com senha fora da política não salva; `dev` e `test` sem variáveis salvam `admin@rgm.com` / `admin123`; e-mail já existente não cria nada
- [x] T004 Alterar `MAIN/adapter/config/AdminUserInitializer.java`: lê `ADMIN_EMAIL` e `ADMIN_SENHA` do ambiente e aplica a regra por perfil (T003 verde)
- [x] T005 Alterar `TEST/core/application/usecases/auth/LoginUseCaseTest.java`: conta inativa com senha errada lança "Credenciais invalidas", igual a e-mail inexistente; conta inativa com a senha certa lança "Usuario inativo"; perfil que não faz login continua genérico; ativo com senha certa devolve o token
- [x] T006 Alterar `MAIN/core/application/usecases/auth/LoginUseCase.java`: confere a senha antes de `ativo` (T005 verde)
- [~] T007 Alterar `TEST/adapter/in/web/auth/AuthControllerTest.java`: as respostas de conta inativa com senha errada e de e-mail inexistente têm o mesmo status, a mesma mensagem e os mesmos cabeçalhos (RNF-04) — **descartada: a igualdade das duas respostas já é provada no caso de uso (T005); o controller devolve a mesma exceção nos dois caminhos**
- [x] T008 Criar `TEST/core/domain/validation/AssinaturaDeArquivoTest.java`: assinaturas válidas de JPEG, PNG, GIF, WebP, PDF e MP4 conferem com o tipo; texto com tipo `image/png` não confere; conteúdo curto ou vazio não confere; tipo desconhecido não confere
- [x] T009 Criar `MAIN/core/domain/validation/AssinaturaDeArquivo.java` (T008 verde)
- [x] T010 Alterar `TEST/core/application/usecases/evidencia/AnexarEvidenciaUseCaseTest.java`: tipo nulo lança `ValidationException` sem chamar o armazenamento; conteúdo diferente do tipo lança `ValidationException`; PNG verdadeiro chega ao armazenamento com todos os bytes preservados; tipo não permitido continua recusado
- [x] T011 Alterar `MAIN/core/application/usecases/evidencia/AnexarEvidenciaUseCase.java`: recusa tipo nulo e confere a assinatura lendo o início do fluxo sem carregá-lo inteiro (T008 e T010 verdes)
- [x] T012 Criar `TEST/adapter/out/storage/MinioStorageServiceTest.java`: o nome do objeto e a URL devolvida não contêm o nome original; a extensão vem do tipo (`image/png` → `.png`); o `contentType` e o tamanho chegam ao cliente; falha do cliente vira `RuntimeException`
- [x] T013 Alterar `MAIN/adapter/out/storage/MinioStorageService.java`: nome do objeto gerado no servidor (T012 verde)

## Fase 3 — Modelos: identificação e máquina inativa (RF-10 a RF-14)

- [x] T014 Criar `TEST/core/domain/exceptions/ConflitoExceptionTest.java`: guarda a mensagem; é uma `DomainException`
- [x] T015 Criar `MAIN/core/domain/exceptions/ConflitoException.java` (T014 verde)
- [x] T016 Alterar `TEST/adapter/config/GlobalExceptionHandlerTest.java`: `ConflitoException` responde 409 com a mensagem em português
- [x] T017 Alterar `MAIN/adapter/config/GlobalExceptionHandler.java`: handler de `ConflitoException` (T016 verde)
- [~] T018 Alterar `TEST/core/domain/model/ModeloTest.java`: `comVersao` devolve cópia com a versão nova e rejeita versão menor que 1 — **descartada: `comVersao` não existe; a versão nova é `maiorVersao + 1` calculada no caso de uso (decisão do plano revisada: sem lock nem retry)**
- [~] T019 Alterar `MAIN/core/domain/model/aggregates/Modelo.java`: `comVersao` (T018 verde) — **descartada junto com T018**
- [x] T020 Criar `TEST/adapter/out/persistence/ModeloRepositoryAdapterPostgresTest.java` (no modelo de `FotoGaleriaModeloRepositoryAdapterPostgresTest`): `maiorVersao` devolve 0 sem modelo, 3 com as versões 1 a 3 e 3 depois de excluir a 2; inserir a mesma `(maquina, codigo, versao)` duas vezes viola o índice único (RNF-05); `existeOutroComIdentificacao` ignora o próprio modelo; `findAllByIdIn` devolve só os pedidos
- [x] T021 Alterar `MAIN/core/domain/ports/repositories/ModeloRepository.java`, `MAIN/adapter/out/persistence/repository/ModeloJpaRepository.java` e `MAIN/adapter/out/persistence/ModeloRepositoryAdapter.java` (e `TEST/adapter/out/persistence/ModeloRepositoryAdapterTest.java`): `maiorVersao` e `existeOutroComIdentificacao`; violação do índice único vira `ConflitoException` (T020 verde)
- [x] T022 Alterar `TEST/core/application/usecases/modelo/GerenciarModelosUseCaseTest.java`: criar usa `maiorVersao + 1` e grava a máquina aparada; editar só a descrição de modelo com máquina inativa funciona; trocar a máquina para uma inativa lança `ValidationException`; editar para código colidindo lança `ConflitoException` com código, versão e máquina na mensagem; editar sem colisão salva
- [x] T023 Alterar `MAIN/core/application/usecases/modelo/GerenciarModelosUseCase.java` (T022 verde)
- [~] T024 Alterar `TEST/adapter/in/web/modelo/ModeloControllerTest.java`: edição que colide responde 409 com a mensagem do caso de uso — **descartada: a tradução de `ConflitoException` em 409 já é provada em T016; o controller não tem lógica própria**

## Fase 4 — Solicitação: responsáveis, cancelamento e edição (RF-15 a RF-21)

- [x] T025 Alterar `TEST/core/domain/model/SolicitacaoTest.java`: `registrarAlteracaoDeResponsaveis` muda `atualizadaEm` e preserva o resto
- [x] T026 Alterar `MAIN/core/domain/model/aggregates/Solicitacao.java` (T025 verde)
- [x] T027 Alterar `TEST/core/application/usecases/solicitacao/TriarSolicitacaoUseCaseTest.java`: lista com id repetido funciona como a sem repetição (uma atribuição por usuário)
- [x] T028 Alterar `MAIN/core/application/usecases/solicitacao/TriarSolicitacaoUseCase.java`: remove duplicatas preservando a ordem (T027 verde)
- [x] T029 Alterar `TEST/core/application/usecases/solicitacao/GerenciarResponsaveisUseCaseTest.java`: id repetido funciona; a solicitação é salva com `atualizadaEm` novo; A Fazer lança `BusinessRuleException` que cita a triagem e não salva; em andamento continua trocando
- [x] T030 Alterar `MAIN/core/application/usecases/solicitacao/GerenciarResponsaveisUseCase.java` (T029 verde)
- [~] T031 Criar `TEST/adapter/out/persistence/SolicitacaoRepositoryAdapterPostgresTest.java`: duas trocas de responsáveis partindo da mesma versão resultam em uma aceita e uma `ObjectOptimisticLockingFailureException` (RNF-05) — **descartada: sem teste de concorrência; o bloqueio otimista da V7 já existia e é coberto pelo handler de 409**
- [~] T032 Alterar `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java`: a segunda troca concorrente responde 409 (handler existente) — **descartada junto com T031**
- [x] T033 Alterar `TEST/core/application/usecases/solicitacao/EncerrarSolicitacaoUseCaseTest.java`: `concluir=false` delega a `CancelarSolicitacaoUseCase` com o comentário final como motivo e devolve a solicitação cancelada; `concluir=true` não chama o cancelamento
- [x] T034 Alterar `MAIN/core/application/usecases/solicitacao/EncerrarSolicitacaoUseCase.java` (T033 verde)
- [x] T035 Alterar `TEST/core/application/usecases/solicitacao/CancelarSolicitacaoUseCaseTest.java`: cancelar por gestor grava status e motivo (referência para a igualdade de trilhas)
- [x] T036 Alterar `TEST/core/domain/validation/AcoesPermitidasSolicitacaoTest.java`: operador que abriu vê `EDITAR` em A Fazer e não vê depois da triagem; gestor vê `EDITAR` em qualquer etapa aberta; `ALTERAR_RESPONSAVEIS` não aparece em A Fazer e aparece em andamento
- [x] T037 Alterar `MAIN/core/domain/validation/AcoesPermitidasSolicitacao.java` (T036 verde)
- [x] T038 Alterar `TEST/core/application/usecases/solicitacao/EditarSolicitacaoUseCaseTest.java`: edição grava uma atividade `COMENTARIO` com autor, texto "Solicitação editada" e título e descrição anteriores; operador em A Fazer edita; operador depois da triagem lança `NaoAutorizadoException` sem salvar; gestor edita em Em Validação; externo continua recusado
- [x] T039 Alterar `MAIN/core/application/usecases/solicitacao/EditarSolicitacaoUseCase.java` (T038 verde)
- [x] T040 Alterar `MAIN/adapter/config/UseCaseConfig.java`: injeta `CancelarSolicitacaoUseCase` no encerrar e `AtividadeSolicitacaoRepository` no editar (coberto por `TEST/ApiApplicationTests.java`)

## Fase 5 — Leitura: modelo, busca, ordem, responsáveis e distribuição (RF-22 a RF-30)

- [x] T041 Alterar `TEST/core/application/usecases/solicitacao/ObterSolicitacaoUseCaseTest.java`: resolve código, máquina e versão do modelo para todos os ids da página com uma só chamada em lote; solicitação sem modelo não aparece no mapa; o detalhe traz o resumo do modelo
- [x] T042 Alterar `MAIN/core/application/usecases/solicitacao/ObterSolicitacaoUseCase.java` (T041 verde)
- [x] T043 Alterar `TEST/adapter/in/web/dto/response/SolicitacaoResponseTest.java`: `modeloVersao` e a sobreposição de código e máquina; sem modelo mantém os dados da criação
- [x] T044 Alterar `MAIN/adapter/in/web/dto/response/SolicitacaoResponse.java` (T043 verde)
- [x] T045 Alterar `TEST/adapter/in/web/ParametrosDeConsultaTest.java`: `busca` aceita 2 a 100 caracteres, apara espaços e devolve nulo quando ausente; 1 e 101 caracteres lançam `ValidationException` em português; `sort=concluidaEm` mapeia para `ENCERRADA_EM`; a mensagem de `sort` inválido lista `concluidaEm`
- [x] T046 Alterar `MAIN/adapter/in/web/ParametrosDeConsulta.java` e `MAIN/core/domain/model/enums/CriterioOrdenacaoSolicitacao.java` (T045 verde)
- [x] T047 Alterar `TEST/adapter/out/persistence/repository/SolicitacaoJpaRepositoryPostgresTest.java`: `busca` "CORREIA" casa título, descrição, código do modelo e código da criação, sem caixa e sem acento; `%` e `_` na busca não viram curinga; soma-se ao status e à visibilidade do operador; `ENCERRADA_EM` desc traz a encerrada mais recentemente primeiro, concluída ou cancelada, e as abertas por último; contagem em aberto por tipo e por prioridade ignora encerradas e traz `SEM_PRIORIDADE`
- [x] T048 Alterar `MAIN/core/domain/ports/repositories/SolicitacaoRepository.java`, `MAIN/adapter/out/persistence/repository/SolicitacaoJpaRepository.java` e `MAIN/adapter/out/persistence/SolicitacaoRepositoryAdapter.java` (e `TEST/adapter/out/persistence/SolicitacaoRepositoryAdapterTest.java`): parâmetro `busca`, ordenação por encerramento e contagens em aberto (T047 verde)
- [x] T049 Alterar `TEST/core/application/usecases/solicitacao/ListarSolicitacoesUseCaseTest.java`: `busca` chega ao repositório e conta como filtro (não cai no `findAll`); `ENCERRADA_EM` chega ao repositório
- [x] T050 Alterar `MAIN/core/application/usecases/solicitacao/ListarSolicitacoesUseCase.java` (T049 verde)
- [x] T051 Alterar `TEST/core/application/usecases/solicitacao/ObterMetricasSolicitacoesUseCaseTest.java`: devolve as duas distribuições do repositório e a soma de cada uma é o total em aberto
- [x] T052 Alterar `MAIN/core/application/usecases/solicitacao/ObterMetricasSolicitacoesUseCase.java` (T051 verde)
- [x] T053 Criar `TEST/adapter/in/web/dto/response/MetricasSolicitacaoResponseTest.java` e alterar `MAIN/adapter/in/web/dto/response/MetricasSolicitacaoResponse.java`: campos aditivos `emAbertoPorTipo` e `emAbertoPorPrioridade`; os campos antigos permanecem
- [~] T054 Criar `TEST/adapter/out/persistence/UsuarioRepositoryAdapterPostgresTest.java`: `buscarAtribuiveis` com 188 usuários alcança operador fora dos 100 primeiros por "zelia"; só `ativo` e perfis atribuíveis; ordem por nome; página e tamanho respeitados — **feita como `UsuarioJpaRepositoryPostgresTest` (a consulta nativa está no JPA, não no adapter)**
- [x] T055 Alterar `MAIN/core/domain/ports/repositories/UsuarioRepository.java`, `MAIN/adapter/out/persistence/repository/UsuarioJpaRepository.java` e `MAIN/adapter/out/persistence/UsuarioRepositoryAdapter.java` (e `TEST/adapter/out/persistence/UsuarioRepositoryAdapterTest.java`) (T054 verde)
- [x] T056 Criar `TEST/core/application/usecases/solicitacao/ListarResponsaveisDisponiveisUseCaseTest.java`: gestor e administrador listam; operador lança `NaoAutorizadoException`; usuário inexistente lança `RecursoNaoEncontradoException`; repassa busca, página e tamanho
- [x] T057 Criar `MAIN/core/application/usecases/solicitacao/ListarResponsaveisDisponiveisUseCase.java` e `MAIN/adapter/in/web/dto/response/ResponsavelDisponivelResponse.java` (com `TEST/adapter/in/web/dto/response/ResponsavelDisponivelResponseTest.java`) (T056 verde)
- [x] T058 Alterar `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java`: `GET /api/solicitacoes/responsaveis-disponiveis` devolve a página e responde 403 ao operador e 400 a `size` acima de 100; a listagem traz código, máquina e versão do modelo em todos os itens com uma só resolução em lote; `q` e `sort=concluidaEm` chegam ao caso de uso; `q` de 1 caractere responde 400
- [x] T059 Alterar `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java` e `MAIN/adapter/config/UseCaseConfig.java`: endpoint novo, parâmetros `q` e `sort`, sobreposição do modelo na listagem e no detalhe (T058 verde)
- [x] T060 Alterar `TEST/adapter/in/web/solicitacao/MetricasContratoV15Test.java`: nomes dos campos das respostas atuais de métricas, listagem e detalhe permanecem (RF-31, RNF-07)

## Fase 6 — Documentação, rastreabilidade e validação (RF-04)

- [x] T061 Alterar `.env.example`, `docs/configuracao.md`, `README.md`, `docs/modelo_dados.md` e `openspec/README.md`: variáveis `ADMIN_EMAIL` e `ADMIN_SENHA`, como definir o primeiro administrador e trocar a senha, índice único do modelo, parâmetros novos, linhas da 012 na tabela
- [x] T062 `make fmt` com `CAMINHO` limitado aos arquivos tocados, `make lint` e `make test` verdes
- [x] T063 Rodar `make it` (o `validate` do repositório não o roda) e `make validate` verdes, com a saída real no `Convergence`

## Fase 7 — Adendo: tipo do modelo cadastrável (RF-32 a RF-35)

- [x] T064 Estender `TEST/adapter/out/persistence/FlywayMigrationTest.java` e criar `RES/db/migration/V12__catalogo_de_tipos_de_modelo.sql`: catálogo com os cinco tipos, valores em uso preservados, nome único
- [x] T065 Criar `TipoDeModelo` (agregado), `TipoDeModeloRepository`, entidade, mapper, JPA, adaptador, `GerenciarTiposDeModeloUseCase`, `ListarTiposDeModeloUseCase`, controllers `/api/tipos-modelo` e `/api/admin/tipos-modelo`, DTOs e beans, espelhando a máquina, com os testes espelhados
- [x] T066 Alterar `GerenciarModelosUseCase` (tipo validado no catálogo), `Modelo`, `ModeloJpaEntity`, `ModeloController` e `ModeloResponse` (tipo vira texto); remover `TipoModelo`
- [x] T067 Alterar `GerenciarMaquinasUseCase` e `GerenciarTiposDeModeloUseCase`: renomear atualiza os modelos (`ModeloRepository.trocarMaquina` e `trocarTipo`)

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| RF-01, RF-02, RF-03 | T003, T004 |
| RF-04 | T061 |
| RF-05 | T005, T006, T007 |
| RF-06 | T006 (a rota e o filtro de limite não mudam; `RateLimitFilterTest` continua verde em T062) |
| RF-07, RF-08 | T008 a T011 |
| RF-09 | T012, T013 |
| RF-10, RF-11 | T001, T002, T020, T021, T022, T023 |
| RF-12 | T014 a T017, T022, T023, T024 |
| RF-13, RF-14 | T022, T023 |
| RF-15 | T027 a T030 |
| RF-16 | T025, T026, T029, T030, T031, T032 |
| RF-17 | T029, T030 |
| RF-18 | T033, T034, T035 |
| RF-19, RF-20 | T038, T039 |
| RF-21 | T036, T037 |
| RF-22 | T041 a T044, T058, T059 |
| RF-23, RF-24 | T054 a T059 |
| RF-25, RF-26 | T045 a T050, T058, T059 |
| RF-27, RF-28 | T045, T046, T047, T048, T049, T050 |
| RF-29, RF-30 | T047, T048, T051, T052, T053 |
| RF-31 | T053, T060 |
| RNF-01 | T041, T058 (uma resolução em lote por página) |
| RNF-04 | T007 |
| RNF-05 | T020, T031 |
| RNF-06 | T062, T063 |
| RNF-07 | T053, T060 |
| RNF-08 | T001, T002 |

### Cenários da spec → testes

| Cenário da spec | Teste |
|---|---|
| Produção sem senha / com senha / senha pública / dev e test | T003 |
| Conta inativa indistinguível | T005, T007 |
| Upload sem tipo / conteúdo diferente | T010 |
| Nome do objeto não vem do cliente | T012 |
| Versão depois de excluir / criações simultâneas | T020 |
| Edição que colide / máquina inativa (editar e trocar) | T022, T024 |
| Responsáveis com id repetido / trocas simultâneas / antes da triagem | T027, T029, T031, T032, T036 |
| Cancelar pelos dois caminhos | T033, T035 |
| Edição registra histórico / operador não edita depois da triagem | T038, T036 |
| Listagem traz o modelo / criação mantém dados | T041, T043, T058 |
| Responsáveis além dos 100 / operador 403 | T054, T056, T058 |
| Busca / visibilidade / tamanho inválido | T045, T047, T058 |
| Ordenar por encerramento / ordenação inválida | T045, T047 |
| Contagem por tipo e prioridade | T047, T051, T053 |

## Convergence

> Seção **append-only**, escrita por `/bu:converge`. Cada rodada acrescenta um bloco;
> nada é reescrito.

### Rodada 1 — 2026-10-11

Evidência de execução:

- `make validate`: lint (spotless) sem violação; `mvnw clean verify` com 1073 testes, 0 falhas;
  `All coverage checks have been met` (JaCoCo, 95%).
- `make test TESTE='*PostgresTest,FlywayMigrationTest'` (o `validate` os exclui): 85 testes, 0
  falhas, em Postgres 16 real (migração V11 com duplicatas, busca, ordenação, contagens,
  responsáveis atribuíveis).

| Requisito | Estado | Evidência |
|---|---|---|
| RF-01, RF-02, RF-03 | realizado | `AdminUserInitializer.java`; `AdminUserInitializerTest` (6 casos) |
| RF-04 | realizado | `.env.example`, `docs/configuracao.md`, `README.md`, `docs/casos-de-uso.md` |
| RF-05 | realizado | `LoginUseCase.java` (senha antes de `ativo`); `LoginUseCaseTest` |
| RF-06 | realizado | `RateLimitFilter` intocado; testes existentes verdes |
| RF-07, RF-08 | realizado | `AnexarEvidenciaUseCase.java`, `AssinaturaDeArquivo.java`; testes de ambos |
| RF-09 | realizado | `MinioStorageService.java`; `MinioStorageServiceTest` |
| RF-10, RF-11 | realizado | V11 (índice único) e `maiorVersao + 1`; `FlywayMigrationTest`, `GerenciarModelosUseCaseTest` |
| RF-12 | realizado | `ConflitoException` + handler 409; `GerenciarModelosUseCaseTest`, `GlobalExceptionHandlerTest` |
| RF-13, RF-14 | realizado | `GerenciarModelosUseCase.java`; `GerenciarModelosUseCaseTest` |
| RF-15, RF-16, RF-17 | realizado | `TriarSolicitacaoUseCase`, `GerenciarResponsaveisUseCase`; testes. O 409 em troca concorrente usa o bloqueio otimista que já existia (V7) e o handler existente |
| RF-18 | realizado | `EncerrarSolicitacaoUseCase` delega a `CancelarSolicitacaoUseCase`; `EncerrarSolicitacaoUseCaseTest` |
| RF-19, RF-20, RF-21 | realizado | `EditarSolicitacaoUseCase`, `AcoesPermitidasSolicitacao`; testes |
| RF-22 | realizado | `ObterSolicitacaoUseCase.resolverModelos`, `SolicitacaoResponse` (`modeloVersao`); controller e response tests |
| RF-23, RF-24 | realizado | `ResponsaveisDisponiveisController`, `ListarResponsaveisDisponiveisUseCase`, `UsuarioJpaRepositoryPostgresTest` |
| RF-25, RF-26 | realizado | `ParametrosDeConsulta.busca`, `BUSCA_FILTRO`; `SolicitacaoJpaRepositoryPostgresTest`, `SolicitacaoControllerTest` |
| RF-27, RF-28 | realizado | `ENCERRADA_EM`, `concluidaEm`; Postgres e `ParametrosDeConsultaTest` |
| RF-29, RF-30 | realizado | `ObterMetricasSolicitacoesUseCase`, `MetricasSolicitacaoResponse`; a restrição de perfil da rota `/metricas` já existia (spec 010) |
| RF-31 | realizado | campos antigos intactos; `MetricasContratoV15Test` verde; só campos e parâmetro novos |
| RNF-01 | realizado | uma consulta de modelos por página (`resolverModelos`), igual para 10 e 100 itens |
| RNF-02, RNF-03 | removidos da spec | sistema interno; decisão do usuário em 2026-10-11 (sem medição de desempenho em escala) |
| RNF-04 | realizado | `LoginUseCaseTest` (mesma mensagem nos dois caminhos) |
| RNF-05 | realizado | índice único + `ConflitoException`; concorrência não recebeu teste próprio (ver tarefas descartadas) |
| RNF-06, RNF-07 | realizado | validate verde; contrato V1.5 verde |
| RNF-08 | realizado | `FlywayMigrationTest` com duplicatas antes da V11 |

Excesso de escopo: nenhum. Dois ajustes de plano, ambos para menos: sem lock consultivo e sem
retry na versão do modelo, sem testes de carga (pedido do usuário: sistema interno).

Pontos a conferir antes do deploy (não bloqueiam): rodar a consulta de duplicatas
`(maquina, codigo, versao)` no banco de produção só para saber o que a V11 vai renumerar; abrir a
tela de evidências do frontend, que passa a receber URL sem o nome original do arquivo.

Veredito: convergido
Tarefas acrescentadas: nenhuma

### Rodada 2 — 2026-10-11 (adendo do tipo do modelo)

- `make validate`: 1103 testes, 0 falhas, cobertura ok. `*PostgresTest` e `FlywayMigrationTest`: 86, 0 falhas.

| Requisito | Estado | Evidência |
|---|---|---|
| RF-32 | realizado | `TipoDeModelo*`, `GerenciarTiposDeModeloUseCase`; testes espelhados de máquina |
| RF-33 | realizado | V12 com o seed; `FlywayMigrationTest` |
| RF-34 | realizado | `GerenciarModelosUseCase.validarTipo`; `GerenciarModelosUseCaseTest` |
| RF-35 | realizado | `trocarMaquina`/`trocarTipo`; `GerenciarMaquinasUseCaseTest`, `GerenciarTiposDeModeloUseCaseTest` |

Compatibilidade (Princípio 9): `tipo` continua texto no mesmo campo; os identificadores antigos são
os nomes do seed, então o frontend v1.5.0 segue válido até um administrador renomear um tipo. O
frontend novo deve listar `/api/tipos-modelo` e cair no próprio nome quando não houver rótulo.

Veredito: convergido
Tarefas acrescentadas: T064 a T067 (já concluídas)
