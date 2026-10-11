# As-is — rgm-backend

Retrato do repositório em `develop` @ `0c858e6` (2026-10-05), produzido por `/bu:reverse`.
Nenhum arquivo do projeto foi alterado para gerar este documento.

## 1. Retrato

API REST do sistema RGM (Reparo/Gestão de Modelos): cadastro de modelos de fundição e
chamados de manutenção sobre eles, em quadro Kanban, com evidências fotográficas, métricas e
PDFs. Usuários internos de uma fábrica, com quatro perfis: OPERADOR, GESTOR, ADMINISTRADOR e
EXTERNO (este não faz login).

- **Stack:** Java 21, Spring Boot 3.5.13 (`app/pom.xml:8`, `app/pom.xml:30`), PostgreSQL com
  Flyway (9 migrações em `app/src/main/resources/db/migration/`, `V1` a `V9`), MinIO/S3 para
  arquivos, OpenPDF para relatórios.
- **Tamanho:** 189 arquivos Java de produção e 84 de teste; 24.306 linhas de Java
  (inventário `inventory.py`).
- **Entrypoint único:** `app/src/main/java/com/rgm/api/ApiApplication.java`.
- **Fluxo principal:** Modelo → Solicitação (A_FAZER → EM_ANDAMENTO → EM_VALIDACAO →
  CONCLUIDA/CANCELADA, com devolução de EM_VALIDACAO para EM_ANDAMENTO) → Evidência.
  Mudanças de status são publicadas por SSE para o frontend.
- **Versão em produção:** `v1.5.0` (tag em `main`); `develop` e `main` apontam para o mesmo
  commit.
- **Repos irmãos:** `rgm-frontend` (React/Vite) e `rgm-infra` (compose e deploy).

## 2. Domínio identificado

Tudo sob `app/src/main/java/com/rgm/api/core/domain/`.

| Elemento | Onde | Observação |
|---|---|---|
| Agregados | `model/aggregates/`: `Solicitacao`, `Modelo`, `Evidencia`, `FotoGaleriaModelo`, `EventoModelo`, `Maquina`, `Usuario` | imutáveis, factory `criar()` |
| Entidades | `model/entities/`: `AtividadeSolicitacao`, `SolicitacaoAtribuicao`, `SolicitacaoEvidencia`, `EventoModeloEvidencia` | vínculos e linha do tempo |
| Máquina de estados | `model/enums/StatusSolicitacao.java:22-35` (`canTransitionTo`) | única fonte das transições |
| Autorização por perfil | `model/enums/PerfilUsuario.java:16-51` (`podeTriar`, `podeEncerrar`, `podeDevolver`, `podeGerenciarModelos`, `podeExcluir` etc.) | regra central de permissão |
| Autorização por solicitação | `Solicitacao.validarAutorizacaoMover` em `model/aggregates/Solicitacao.java` | autor × responsável × perfil |
| Enums de negócio | `model/enums/`: `TipoEvidencia`, `TipoSolicitacao`, `TipoModelo`, `PrioridadeSolicitacao`, `TipoAtividadeSolicitacao`, `TipoEventoModelo`, `TipoFiltroData`, `OrdenacaoMetricaModelo` | |
| Erros de domínio | `exceptions/`: `DomainException`, `BusinessRuleException`, `NaoAutorizadoException`, `RecursoNaoEncontradoException`, `TransicaoStatusInvalidaException`, `ValidationException` | tipos próprios |
| Portas | `ports/repositories/` (10 repositórios, `PageResult`, `MetricaModeloRow`) e `ports/services/` (`StorageService`, `PasswordHasher`, `AccessTokenIssuer`, `DomainEventPublisher`) | |
| Evento de domínio | `events/SolicitacaoFinalizadaEvent.java` | |

Casos de uso em `core/application/usecases/<área>/`, um por classe com `execute()`:
solicitacao (15), modelo (8), admin (5), auth (3), evidencia (3), maquina (1).

Regra de acesso duplicada fora do domínio: `validarAcesso` existe com o mesmo corpo em
`core/application/usecases/evidencia/AnexarEvidenciaUseCase.java:231-248` e em
`VisualizarEvidenciaUseCase.java:59-75`. É a origem da issue #86.

## 3. Arquitetura as-is × alvo

O projeto já é Clean Architecture; a diferença para o padrão Byte Union é de nomes de pacote
e de onde vivem as interfaces, não de direção de dependência.

| Área hoje | Arquivos | Camada alvo | O que impede migrar agora |
|---|---|---|---|
| `core/domain/model`, `enums`, `exceptions`, `validation`, `events` | 30 | `core.domain.{entities,enums,errors}` | só renomeação de pacote (`exceptions` → `errors`, `aggregates` → `entities`) |
| `core/domain/ports/{repositories,services}` | 16 | `interfaces.adapters.*`, com prefixo `I` | renomear 14 interfaces e todos os usos; nenhum acoplamento técnico |
| `core/application/usecases` | 35 | `core.application` | 10 casos de uso importam `org.springframework.transaction.annotation.Transactional` (9 em `solicitacao/`, mais `admin/GerenciarMaquinasUseCase.java`); é a única dependência de framework em `core` |
| `adapter/in/web/<área>` + `adapter/in/web/dto` | 49 | `adapters.controllers` (+ DTOs) | pacote singular `adapter` × `adapters`; `SolicitacaoController.java` tem 526 linhas |
| `adapter/out/persistence` | 42 | `adapters.repositories` | renomeação |
| `adapter/out/{storage,security,report,event}` | 9 | `adapters.clients` (storage) e `infra.tools` (security, report, event) | `ModeloPdfService.java` tem 583 linhas |
| `adapter/config` | 7 | `infra.init` (`UseCaseConfig` é o IoC, 397 linhas) e `infra.api` (filtros, handler) | renomeação |

Arquivos acima do limite de 500 linhas: `Solicitacao.java` (617), `ModeloPdfService.java`
(583), `SolicitacaoController.java` (526).

Recomendação: não migrar pacotes agora. O ganho é só de nomenclatura, o diff tocaria quase
todos os arquivos, e há 8 issues abertas de produto. Registrar na constituição o mapa
"nome local → camada Byte Union" e aplicar as regras de camada sobre os nomes existentes.

## 4. Contrato de operação

| Alvo exigido | Existe hoje | Observação |
|---|---|---|
| `infra` | `docker-up` | sobe só `db`, `minio`, `minio-init` (`docker-compose.yml:4-39`) |
| `install` | `build` (`mvnw compile`) | |
| `init` | — | Flyway roda na subida da aplicação; bucket criado por `minio-init` |
| `fmt` | `format` (`spotless:apply`) | nome diferente |
| `lint` | `lint` (`spotless:check`) | sem checkstyle nem spotbugs no `pom.xml`, embora `openspec/config.yaml` cite checkstyle |
| `test` | `test`, `test-fast`, `test-all` | |
| `cover` | `coverage` (`mvnw verify`) | JaCoCo com mínimo de 95% de linha no bundle (`app/pom.xml:260`, `:281`) |
| `it` | — | integração misturada em `src/test`; `FlywayMigrationTest` usa Testcontainers |
| `bdd` | — | não há Cucumber nem `.feature` |
| `validate` | `validate` = `lint` + `mvnw clean verify` | não roda `fmt`, `it` nem `bdd` |
| `run`, `down`, `ps`, `logs` | `run`, `docker-down`, `docker-logs` | `ps` não existe |

Desvios do contrato:

- **Sem serviço `dev`:** os alvos rodam `./mvnw` no host e exigem JDK 21 instalado. Nesta VPS
  não há JDK de sistema; a medição deste relatório usou `mise exec java@temurin-21`.
- **Compose sem `name:`** e sem rede nomeada (`docker-compose.yml` começa em `services:`).
  O projeto herda o nome da pasta.
- **`ports` em backing services:** `db` e `minio` publicam portas no host
  (`docker-compose.yml:11`, `:28`).
- **Makefile, Dockerfile e compose ficam na raiz**, não em `app/`.
- **Comentário desatualizado:** o alvo `validate` anuncia "coverage 85%", mas o `pom.xml`
  exige 95%.

O que falta para `make validate` rodar headless: um serviço `dev` com JDK 21 no compose, ou
um `mise.toml` fixando `java = "temurin-21"`. A CI (`.github/workflows/ci.yml`) já roda
`spotless:check` e `mvnw verify` com JDK 21.

## 5. Testes

- **Onde:** `app/src/test/java/com/rgm/api/`, espelhando os pacotes de produção.
- **Quantidade:** 84 arquivos, razão teste/produção de 0,417 em arquivos.
- **Padrão:** JUnit 5 e Mockito. O prefixo de nome mais comum é `deve...` (português), não
  `should...When...`.
- **AAA:** 1 arquivo de 82 usa o comentário `// Arrange`.
- **`verifyNoMoreInteractions`:** não aparece em nenhum teste.
- **Integração:** 2 arquivos usam `@SpringBootTest` ou Testcontainers; os testes de
  controller usam `@WebMvcTest`. Não há WireMock nem BDD.
- **Cobertura medida** (`mvnw verify -Dtest='!FlywayMigrationTest'`, JDK Temurin 21, em
  2026-10-05): 546 testes, 0 falhas, 0 ignorados. Linha **95,60%** (3.410 de 3.567), ramo
  **79,23%** (740 de 934), 201 classes. `FlywayMigrationTest` ficou fora, como no alvo
  `make coverage`.
- **Classes abaixo de 90% de linha:** `HistoricoMetricasResponse` (0%),
  `EvidenciaController` (77%), `VisualizarEvidenciaUseCase` (79%), `AdminUserInitializer`
  (87%), `SolicitacaoEventPublisher` (88%), `ModeloPdfService` (88%),
  `SolicitacaoPdfService` (88%), `BcryptPasswordHasher` (75%). O mínimo do `pom.xml` é do
  bundle, não por arquivo; o padrão Byte Union exige 90% por arquivo modificado.
- **Excluídos da medição** (`app/pom.xml:270-272`): `PdfFooterEvent`, `SolicitacaoEvent` e
  `SolicitacaoSseController`. O último concentra o comportamento das issues #87 e #88.

## 6. Riscos

1. **SSE fora da cobertura.** `SolicitacaoSseController` está excluído do JaCoCo, e as
   issues #87 (eventos faltando) e #88 (heartbeat) mexem exatamente ali.
2. **Regra de acesso a evidências duplicada** em dois casos de uso (seção 2). Uma correção
   feita em um só lugar deixa o outro inconsistente; é o caso da #86.
3. **Permissão recriada no frontend.** O backend não expõe as ações permitidas (#90); a tela
   pode mostrar botões que terminam em 403.
4. **Paginação sem teto** em `SolicitacaoController`, `ModeloController` e `AdminController`
   (#89): `size` arbitrário gera consulta pesada.
5. **Upload em duas chamadas** (#91): o status avança mesmo se a foto falhar. O armazenamento
   não é transacional.
6. **Tokens:** refresh token em `localStorage` e access token na query string do SSE (#93).
7. **Segredo de desenvolvimento versionado:** `jwt.secret` fixo em
   `app/src/main/resources/application-dev.properties:6`. Produção lê `${JWT_SECRET}`
   (`application.properties:46`), mas `DB_PASSWORD` e `MINIO_SECRET_KEY` têm valores padrão
   (`application.properties:11`, `:40`).
8. **Dois padrões de especificação.** O repositório adotou OpenSpec em agosto de 2026
   (`openspec/specs/` com 10 capacidades, 2 mudanças abertas em `openspec/changes/`, skills
   em `.claude/skills/openspec-*`). O fluxo Byte Union usa `.specify/` e `specs/NNN-slug/`.
   Manter os dois ativos divide a fonte da verdade.
   **Resolvido em 2026-10-05:** as 2 mudanças foram arquivadas e sincronizadas, as skills e
   os comandos OpenSpec saíram de `.claude/` e `openspec/` ficou congelado (Princípio 12).
9. **Arquivos grandes** concentram regra: `Solicitacao.java` (617 linhas).

## 7. Plano de adoção em ondas

O projeto parte de uma base boa: camadas corretas, `make validate` existente e CI ativa.
As ondas abaixo são pequenas.

### Onda 1 — contrato de operação

- Fixar o JDK: `mise.toml` com `java = "temurin-21"` e/ou serviço `dev` no compose.
- Compose com `name: rgm-backend`, rede nomeada, `expose` em `db` e `minio`.
- Makefile com os nomes do contrato (`infra`, `install`, `fmt`, `cover`, `down`, `ps`,
  `logs`), mantendo os nomes atuais como atalhos.
- **Pronto quando:** `make validate` passa em máquina sem JDK instalado no sistema.
- **Risco de parar no meio:** baixo; os alvos antigos continuam funcionando.

### Onda 2 — especificação única e constituição

- `/bu:constitution` registrando o mapa de nomes da seção 3, o mínimo de cobertura (95%
  do projeto, acima dos 90% da organização) e a decisão sobre OpenSpec.
- Decidir: arquivar `openspec/changes/` abertas e passar a especificar em `specs/NNN-slug/`,
  mantendo `openspec/specs/` como referência histórica.
- **Pronto quando:** existe `.specify/memory/constitution.md` e nenhuma mudança OpenSpec
  fica aberta sem dono.
- **Risco de parar no meio:** duas fontes de especificação ativas.

### Onda 3 — testes no padrão

- Aplicar AAA, `shouldXWhenY` e `verifyNoMoreInteractions` **só nos testes novos ou
  alterados** por cada issue; não reescrever os 84 existentes.
- Tirar `SolicitacaoSseController` da lista de exclusão do JaCoCo ao tratar #87/#88.
- Separar integração em `it` (failsafe) quando a primeira issue exigir teste de contrato.
- **Pronto quando:** todo arquivo tocado por uma issue tem cobertura ≥ 95% e o teste segue
  o padrão.
- **Risco de parar no meio:** nenhum; a suíte antiga continua válida.

### Onda 4 — camadas e interfaces

- Remover `@Transactional` do `core` (10 casos de uso), movendo a fronteira transacional
  para `UseCaseConfig` ou para um decorador em `adapter`.
- Extrair a regra de acesso a evidências para o domínio (resolve a duplicação da #86).
- Quebrar `SolicitacaoController` e `Solicitacao` abaixo de 500 linhas.
- Renomear pacotes para o padrão Byte Union por último, um domínio por vez, se ainda
  fizer sentido.
- **Pronto quando:** `grep -r "org.springframework" core/` não retorna nada e nenhum
  arquivo passa de 500 linhas.
- **Risco de parar no meio:** renomeação parcial deixa dois padrões de pacote; por isso
  ela fica por último e é opcional.
