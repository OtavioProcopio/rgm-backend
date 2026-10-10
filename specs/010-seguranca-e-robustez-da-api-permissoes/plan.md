# Plano de implementação — Segurança e robustez da API: permissões, exposição e limites

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.
> Spec: `specs/010-seguranca-e-robustez-da-api-permissoes/spec.md`. Base: `develop`.

## O que a leitura do código mostrou

| Fato | Onde | Consequência para o plano |
|---|---|---|
| O 401 sem corpo em #113 vem de `response.sendError(...)`: o container faz um despacho de erro para `/error`, que cai em `anyRequest().authenticated()`, e como o filtro JWT não roda no despacho de erro, a resposta final é 401 sem corpo. Isso explica também por que `/actuator/metrics` e `/v3/api-docs` (rotas inexistentes) respondem 401 e não 404 | `adapter/config/SecurityConfig.java` (`exceptionHandling`) | Os dois tratadores passam a escrever o JSON direto, sem `sendError` |
| `springdoc.swagger-ui.enabled=false` e `springdoc.api-docs.enabled=false` **já estão** no perfil `prod`, e o compose de produção do `rgm-infra` define `SPRING_PROFILES_ACTIVE: prod` | `application-prod.properties`, `rgm-infra/docker-compose.prod.yml:95` | O RF-10 já é verdade; falta só o teste que o fixa |
| `PageRequest.of(page, size, ...)` lança `IllegalArgumentException` dentro de adaptadores `@Repository`, e o Spring a traduz em exceção de acesso a dados, que o `handleRuntime` transforma em 500 | `SolicitacaoRepositoryAdapter`, `ModeloRepositoryAdapter`, `UsuarioRepositoryAdapter` | A validação tem que acontecer **antes** do caso de uso, na borda web |
| `Instant.parse(...)` e `Enum.valueOf(...)` são chamados direto em `buildInput` e em `AdminController` | `SolicitacaoController.buildInput`, `AdminController.listarUsuarios` | Um ponto único de leitura de parâmetro na borda web, com o nome do campo na mensagem |
| `HttpMessageNotReadableException` não tem tratador próprio e cai em `handleRuntime` | `GlobalExceptionHandler` | Tratador novo, 400 |
| O filtro JWT só autentica quando há `Authorization: Bearer`, e dá a autoridade `ROLE_<perfil>` | `JwtAuthenticationFilter` | `hasAnyRole("ADMINISTRADOR","GESTOR")` funciona como nas rotas de `/api/admin` |
| O Prometheus de desenvolvimento coleta de `host.docker.internal:8080/actuator/prometheus` sem credencial; só existe no perfil `monitoring` do compose de desenvolvimento | `rgm-infra/monitoring/prometheus.yml` | O usuário não usa o `rgm-infra` (Esclarecimento 6): **nada é alterado lá**. A credencial de coleta fica documentada no backend para quem passar a coletar |

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Quem pode ler indicadores (RF-01) | Uma regra no `SecurityConfig`, no mesmo ponto das rotas de `/api/admin`: `GET /api/solicitacoes/metricas` e `/api/solicitacoes/metricas/**` exigem `ADMINISTRADOR` ou `GESTOR` | Anotar cada método com `@PreAuthorize`; checar o perfil dentro do caso de uso | Um lugar só, igual ao padrão existente, testável com a configuração real de segurança; o caso de uso continua sem conhecer perfil |
| Corpo de 401 e 403 (RF-02) | `AuthenticationEntryPoint` e `AccessDeniedHandler` próprios, que gravam o `ErrorResponse` em JSON diretamente na resposta | Permitir `/error` em `permitAll`; manter `sendError` e tratar no advice | Permitir `/error` mudaria a resposta de rotas inexistentes (hoje 401) e isso não está na spec; o advice não enxerga o que o filtro de segurança recusa |
| Exposição do actuator (RF-03) | Segunda `SecurityFilterChain` com `securityMatcher("/actuator/**")` e `@Order(1)`: `health` liberado, o resto exige HTTP Basic de uma conta de coleta, com `UserDetailsService` próprio construído das propriedades de coleta. Sem senha configurada, a conta não existe e nada abre | `hasRole("ADMINISTRADOR")` na cadeia única (a conta humana usa JWT que expira; o coletor teria de renovar); desligar o endpoint por padrão (o usuário escolheu a credencial) | Isola o actuator da cadeia JWT e do limite de tentativas, e o coletor autentica de forma estável |
| Onde validar `page` e `size` (RF-05, RF-07) | Um verificador na borda web (`ParametrosDeConsulta.pagina`), chamado pelos quatro controllers, que lança `ValidationException` (400) com o intervalo na mensagem | `@Validated` com `@Min`/`@Max` (proxy nos controllers e mensagem sem o nome do intervalo em português); validar em `PageRequest` no adaptador (tarde, vira 500) | Mensagem em português controlada, sem proxy novo, e um só lugar para o teto de 100 |
| Datas e enumerações (RF-05, RF-08) | Mesmo verificador: `ParametrosDeConsulta.data(campo, valor)` e `ParametrosDeConsulta.enumeracao(campo, valor, tipo)`, que dizem o campo e o formato ou os valores aceitos. Rede de proteção: o tratador de `IllegalArgumentException` nunca devolve a mensagem "No enum constant ..." | Tratador genérico de `DateTimeParseException` (não sabe o campo); só a rede de proteção (não diz os valores aceitos) | O RF-05 pede campo e formato na mensagem; a rede cobre também corpo de requisição com enum inválido |
| Corpo malformado (RF-05) | Tratador de `HttpMessageNotReadableException` no `GlobalExceptionHandler`, 400, mensagem em português | Validar o corpo na mão em cada controller | É o ponto único de erro de requisição |
| Swagger (RF-10) | Nenhuma mudança de código: o perfil `prod` já desliga. Acrescentar teste que lê `application-prod.properties` e afirma as duas chaves | Remover as rotas `/swagger-ui/**` do `SecurityConfig` | Em desenvolvimento ele continua útil; o risco real era prod, e prod já está fechado |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Cadeia de responsabilidade (já oferecida pelo Spring Security: `SecurityFilterChain` múltiplas, ordenadas) | `ActuatorSecurityConfig` ao lado de `SecurityConfig` | Regras de autenticação diferentes para `/actuator/**` e `/api/**` sem condicionais em uma cadeia só | Duas cadeias para ler em vez de uma |
| Objeto de parâmetro de leitura (utilitário estático puro) | `ParametrosDeConsulta` | Quatro controllers repetiriam parse, mensagem e teto de página | Uma classe nova na borda web; sem estado |

**Considerados e recusados:**

| Padrão | Motivo |
|---|---|
| Strategy para "validar parâmetro por tipo" | Três funções estáticas bastam; Strategy seria antecipação (YAGNI) |
| Decorator/Filter próprio para limitar `size` em toda requisição | Esconde a regra do controller que a usa e acopla a todos os parâmetros chamados `size`, inclusive os que não são de página |
| Chain of Responsibility explícito para os tratadores de erro | O `@RestControllerAdvice` já resolve pelo tipo da exceção |

## Arquivos a criar ou alterar

Caminhos sob `app/src/main/java/com/rgm/api/` (produção) e `app/src/test/java/com/rgm/api/`
(teste), conforme o mapa de camadas do Princípio 7: `adapter.in.web` é controllers e
`adapter.config` é `infra/init` e `infra/api`. Nenhuma pasta nova.

| Camada | Arquivo | Ação | Teste espelhado | Requisitos |
|---|---|---|---|---|
| infra/api | `adapter/config/SecurityConfig.java` (regra de indicadores; tira `/actuator/**` da cadeia; handlers JSON) | alterar | `adapter/config/SecurityConfigTest.java` (novo, `@WebMvcTest` com a configuração real) | RF-01, RF-02, RF-03 |
| infra/api | `adapter/config/ActuatorSecurityConfig.java` (cadeia do actuator, conta de coleta) | criar | `adapter/config/ActuatorSecurityConfigTest.java` | RF-03 |
| infra/api | `adapter/config/JsonAuthenticationEntryPoint.java` | criar | `adapter/config/JsonAuthenticationEntryPointTest.java` | RF-02 |
| infra/api | `adapter/config/JsonAccessDeniedHandler.java` | criar | `adapter/config/JsonAccessDeniedHandlerTest.java` | RF-02 |
| infra/api | `adapter/config/GlobalExceptionHandler.java` (tratador de corpo malformado; rede para "No enum constant") | alterar | `adapter/config/GlobalExceptionHandlerTest.java` (existe ou é criado) | RF-05, RF-08 |
| controllers | `adapter/in/web/ParametrosDeConsulta.java` (`pagina`, `data`, `enumeracao`) | criar | `adapter/in/web/ParametrosDeConsultaTest.java` | RF-05, RF-07, RF-08 |
| controllers | `adapter/in/web/solicitacao/SolicitacaoController.java` (usa o verificador em `listar`, `obterMetricasPorModelo` e `buildInput`) | alterar | `adapter/in/web/solicitacao/SolicitacaoControllerTest.java` (existente, mais casos) e `SolicitacaoControllerSegurancaTest.java` (novo, indicadores por perfil) | RF-01, RF-05, RF-07, RF-08 |
| controllers | `adapter/in/web/modelo/ModeloController.java` (`listar`) | alterar | `adapter/in/web/modelo/ModeloControllerTest.java` (existente, mais casos) | RF-07 |
| controllers | `adapter/in/web/admin/AdminController.java` (`listarUsuarios`: página e `perfil`) | alterar | `adapter/in/web/admin/AdminControllerTest.java` (existente, mais casos) e `AdminControllerSegurancaTest.java` (novo, 403 do operador) | RF-02, RF-07, RF-08 |
| resources | `app/src/main/resources/application.properties` (propriedades da conta de coleta, vazias por padrão) | alterar | coberto por `ActuatorSecurityConfigTest` | RF-03 |
| resources | `app/src/main/resources/application-prod.properties` | sem mudança (já fecha o Swagger) | `ConfiguracaoDeProducaoTest.java` (novo, lê o arquivo) | RF-10 |
| teste de contrato | `app/src/test/java/com/rgm/api/adapter/in/web/RotasExistentesTest.java` (afirma que as rotas listadas no `develop` de partida seguem mapeadas; não afirma códigos de status, que os testes de cada controller já protegem) | criar | — (é o teste) | RF-09 |

Fora de `app/` (artefatos do repositório, exigidos pela constituição e pela spec):

| Arquivo | Ação | Motivo |
|---|---|---|
| `docs/casos-de-uso.md` (UC-16: ator restrito, 403; nota sobre limite de página e erros 400) | alterar | Documentação do contrato |
| `openspec/README.md` (linha da 010 em `autenticacao` e `solicitacoes-kanban`) | alterar | Princípio 12 |
| `.env.example` (variáveis da conta de coleta, sem valor, com comentário de uso) | alterar | Contrato de operação e documentação para quem coletar |

## Contrato entre camadas

- **Entrada:** o filtro de segurança decide 401 e 403 e já grava o `ErrorResponse`. Passando
  dele, o controller usa `ParametrosDeConsulta` para ler página, datas e enumerações e só então
  chama o caso de uso com tipos válidos. O caso de uso e o domínio não mudam.
- **Erro:** `ValidationException` (core/domain/errors) continua sendo o sinal de pedido
  inválido; o `GlobalExceptionHandler` o traduz em 400. Nenhuma exceção de acesso a dados
  deve mais ser causada por entrada do cliente.
- **Dependência:** `adapter.in.web` importa `core.domain.exceptions` (para dentro, permitido);
  `core` não importa nada de `adapter`.

## Dependências externas

| Dependência | Versão | Justificativa | Simulada nos testes por |
|---|---|---|---|
| Spring Security (já no projeto) | a do Boot 3.5 | `SecurityFilterChain` múltipla, HTTP Basic, `UserDetailsService` em memória | Configuração real em `@WebMvcTest`, com controllers de teste mapeados em `/actuator/prometheus` e `/actuator/health` |
| `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (Spring Security Crypto, já presente; o codificador padrão é bcrypt) | a do Boot 3.5 | Guardar a senha de coleta com hash na memória. Emenda de 2026-10-10: o plano dizia `BCryptPasswordEncoder`; o delegante é o que a cadeia usa por padrão e resulta em bcrypt, com o prefixo `{bcrypt}` | Real |

Nenhuma dependência nova no `pom.xml`.

## Dependências entre repositórios e ordem de publicação

| Repositório | Mudança | Por quê | Ordem |
|---|---|---|---|
| `rgm-frontend` | `PerfilPage` não chama `useMetricas` para o operador e a seção "Visão Geral do Sistema" só aparece para gestor e administrador | RF-06: sem isso a restrição do RF-01 daria 403 na tela do operador | **1º** |
| `rgm-infra` | **Nenhuma** (Esclarecimento 6: o repositório não é usado de fato) | RF-04 foi retirado | — |
| `rgm-backend` | Esta spec | RF-01 a RF-03 e RF-05 a RF-10 | **2º** |

## Impacto no contrato de operação

- **Variáveis de ambiente novas** (backend): `MONITORAMENTO_COLETA_USUARIO` e
  `MONITORAMENTO_COLETA_SENHA`, mapeadas para `monitoramento.coleta.usuario` e
  `monitoramento.coleta.senha`. Vazias por padrão; com a senha vazia ninguém coleta.
- **Makefile:** nenhum alvo novo, mas o alvo `test` ganha a variável opcional `TESTE=<Classe>`
  (emenda de 2026-10-10: sem ela não há como rodar um teste só por `make`, e o Princípio 1
  proíbe chamar `mvnw` direto). Sem a variável, o comportamento é o de antes. O alvo `validate`
  roda `lint`, `test`, `coverage` e `build`, e todo comando passa por ele.
- **Compose e `rgm-infra`:** nenhuma mudança. Produção não executa o coletor e não precisa das
  variáveis. Quem passar a coletar define as duas variáveis no backend e usa HTTP Basic com
  elas no coletor, conforme o comentário do `.env.example`.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Publicar o backend antes do frontend dá 403 na tela de perfil do operador | média | A ordem da tabela acima é obrigatória; a PR do backend cita a do frontend e só é mesclada depois dela |
| Alguma tela do operador chama outro endpoint de métricas que eu não vi | baixa | `grep` em `rgm-frontend` mostrou só `PerfilPage` para `/metricas`; o e2e do operador roda contra o backend novo antes de publicar |
| Regras de `/actuator/**` na nova cadeia deixam o health check do orquestrador sem resposta | baixa | Teste com a configuração real afirma `health` 200 sem credencial; o healthcheck do compose de produção usa o mesmo caminho |
| `size` acima de 100 usado por alguma integração fora do frontend | baixa | O frontend não pede mais de 100; relatórios internos usam caso de uso direto, não a rota; registrar na nota de publicação |
| Mudar `buildInput` quebra algum filtro que hoje passa | média | Os testes existentes de `listar` rodam antes e depois; os cenários de aceite de data e enumeração entram como testes novos |
| A conta de coleta do actuator não tem limite de tentativas (o `RateLimitFilter` só atua em `/api/auth/login`); com `MONITORAMENTO_COLETA_*` definidas, `/actuator/prometheus` aceita tentativas ilimitadas de senha | baixa (a senha vem vazia por padrão; produção não coleta) | Aceito nesta spec (fora de escopo: limitar tentativas é requisito novo). Quem passar a coletar deve usar senha longa e aleatória e manter o endpoint atrás da rede interna. Registrado como dívida na issue de acompanhamento |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| 1 Contrato de operação | Nenhum alvo novo; tudo por `make`; as variáveis novas são declaradas e opcionais |
| 2 Arquitetura limpa | Regras de borda ficam em `adapter.in.web` e `adapter.config`; domínio e casos de uso não mudam; dependência aponta para dentro |
| 3 Testes provam a entrega | Cada arquivo de produção tem teste espelhado; testes novos em AAA, um comportamento por teste, `shouldXWhenY`; cenários de aceite viram teste com a configuração real de segurança |
| 4 Simplicidade defensável | Três funções estáticas em vez de Strategy; duas cadeias do Spring em vez de condicionais; padrões recusados registrados |
| 5 Autoria | Mensagens de commit e PR sem crédito a ferramenta de IA |
| 6 Idioma | Spec, plano, tarefas, mensagens de erro e commits em português; cenários em DADO/QUANDO/ENTÃO |
| 7 Mapa de camadas do legado | Nenhum pacote novo; `core` não importa `org.springframework`; classes novas em `adapter.config` e `adapter.in.web` |
| 8 Linguagem ubíqua em português | `ParametrosDeConsulta`, `pagina`, `data`, `enumeracao`; termos técnicos seguem em inglês; testes `shouldXWhenY` |
| 9 Compatibilidade com a produção | RF-09 vira teste; nenhuma rota, campo ou evento é removido; nenhuma migração Flyway; ordem de publicação declarada |
| 10 Cobertura não regride | Arquivos novos ficam fora de qualquer exclusão do JaCoCo; cada arquivo modificado com 95% ou mais de linha; limite do `pom.xml` intacto |
| 11 Padrão de teste | Só teste novo ou alterado segue o padrão; os existentes não são reescritos |
| 12 Uma fonte de especificação | Linha acrescentada em `openspec/README.md`; `openspec/specs/` e `openspec/changes/` não são tocados |

## Observação fora de escopo

Rotas inexistentes respondem **401** para quem não tem credencial, e para quem a tem, pelo
mesmo motivo do despacho de erro (o filtro JWT não roda lá). Esta entrega não muda isso (não
está na spec); vale uma issue própria.
