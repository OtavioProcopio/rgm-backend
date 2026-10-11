# Plano de implementação — Correção dos bugs abertos do backend e melhorias para o frontend

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.
>
> Código lido em `develop` (`61be374`). Pacotes sob `app/src/main/java/com/rgm/api` e testes
> sob `app/src/test/java/com/rgm/api`, espelhados (Princípio 7). Abreviações: `uc` =
> `core/application/usecases`, `web` = `adapter/in/web`, `per` = `adapter/out/persistence`.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Senha inicial do admin (RF-01–03) | `AdminUserInitializer` lê `ADMIN_EMAIL` e `ADMIN_SENHA`; em `prod` sem senha, ou com `admin123`, ou fora da `PoliticaSenha`, não cria nada e registra aviso; fora de `prod` o padrão continua `admin@rgm.com` / `admin123` | Remover o inicializador em `prod`; gerar senha aleatória e logar | Remover deixa a operação sem como nascer o primeiro admin; logar senha vaza em log. Variável de ambiente é o caminho que a issue pede |
| Login (RF-05) | Conferir a senha **antes** de olhar `ativo`; só quem acerta a senha de conta inativa vê "Usuario inativo" | Responder sempre genérico | Padrão adotado na spec; quem tem a senha precisa saber por que não entra |
| Upload (RF-07–09) | Tipo nulo recusado; leitura dos primeiros 12 bytes do fluxo (com `PushbackInputStream`, sem carregar o arquivo) conferida contra a assinatura do tipo declarado; nome do objeto gerado no `MinioStorageService` (UUID + extensão do tipo validado) | Detectar tipo por biblioteca (Tika); gravar o arquivo em memória | Seis tipos fixos têm assinatura curta e conhecida; biblioteca nova não se justifica (Princípio 4) |
| Versão do modelo (RF-10–11) | Migração V11 saneia duplicatas e cria índice único `(maquina, codigo, versao)`; a nova versão é `MAX(versao)+1`; se duas criações simultâneas colidirem, o índice recusa a segunda e ela responde 409 com mensagem clara | Lock consultivo por grupo; retry automático | Sistema interno com poucos usuários: a colisão é rara e o índice já garante a integridade; lock e retry são complexidade sem uso |
| Saneamento das duplicatas (RNF-08) | Em cada grupo duplicado, o mais antigo (`criado_em`, `id`) mantém a versão; os outros recebem `MAX(versao do grupo) + posição` | Renumerar todos os modelos | Renumerar tudo mudaria a identificação de modelos que não têm problema |
| Colisão na edição (RF-12) | Nova `ConflitoException` de domínio, mapeada para 409 no `GlobalExceptionHandler`, com a combinação na mensagem; `existsByMaquinaAndCodigoAndVersaoAndIdNot` antes de salvar, e o índice único como rede de segurança | Só confiar no índice (mensagem genérica de hoje) | A spec exige mensagem que diz qual combinação colidiu |
| Máquina inativa (RF-14) | `editar` carrega o modelo primeiro e valida a máquina só se a (aparada) for diferente da atual | Remover a validação | A troca para máquina inativa continua proibida |
| Responsáveis (RF-15–17) | `distinct()` na lista, preservando a ordem; `Solicitacao.registrarAlteracaoDeResponsaveis(agora)` atualiza `atualizadaEm` e o `save` incrementa o `version`; A Fazer recusa com orientação para a triagem | Bloqueio pessimista | O bloqueio otimista já existe (V7) e o 409 já tem handler |
| Cancelamento (RF-18) | `EncerrarSolicitacaoUseCase` com `concluir=false` delega a `CancelarSolicitacaoUseCase`, que já grava status e motivo | Duplicar o comentário no encerrar | Uma rotina só; o contrato dos dois endpoints não muda |
| Edição (RF-19–21) | Atividade do tipo **COMENTARIO** com texto fixo "Solicitação editada" + valores anteriores; limite de etapa do operador no caso de uso e em `AcoesPermitidasSolicitacao` | Novo tipo de atividade `EDICAO` | O frontend v1.5.0 mapeia os tipos de atividade em tabelas fechadas (`SolicitacaoTimeline`, `rotulos`); um tipo novo quebraria a linha do tempo em produção (Princípio 9) |
| Modelo na listagem (RF-22) | Resolução em lote, no mesmo padrão de `listarResponsaveisBatch`: `ModeloRepository.findAllByIdIn(ids)` uma vez por página e sobreposição em `SolicitacaoResponse` (novo campo `modeloVersao`); sem sobrescrever os dados próprios da CRIACAO quando não há modelo | `@Formula` na entidade; `JOIN` e colunas extras no `SELECT *` | Mantém o domínio e a consulta de filtros intactos; 1 consulta por página, igual para 10 ou 100 itens |
| Acento e caixa (RF-23, RF-25) | Função SQL `rgm_sem_acento(text)` (migração V11, `IMMUTABLE`, `translate` sobre tabela fixa de letras latinas) usada em consulta e termo | Extensão `unaccent`; `ILIKE` puro | `unaccent` exige privilégio no banco de produção; `ILIKE` não ignora acento. Função própria não depende de extensão |
| Responsáveis disponíveis (RF-23–24) | `GET /api/solicitacoes/responsaveis-disponiveis?busca&page&size`, caso de uso novo, consulta `UsuarioRepository.buscarAtribuiveis` filtrando `ativo` e `PerfilUsuario.isAtribuivel` (OPERADOR e GESTOR), ordenada por nome; `podeTriar` ou 403 | Reusar `/api/admin/usuarios` | A issue pede não alterar a administração; o caminho literal não colide com `/{id}` |
| Busca `q` (RF-25–26) | Novo parâmetro `busca` no `findByFilters` (título, descrição, `modelos.codigo` e `modelo_codigo` da CRIACAO), com `%` e `_` escapados; `ParametrosDeConsulta.busca` valida 2 a 100 caracteres | Índice `pg_trgm` | Sistema interno com poucas centenas de solicitações; a consulta já tem o `LEFT JOIN modelos` |
| Ordem por encerramento (RF-27–28) | Valor `ENCERRADA_EM` no enum e `concluidaEm` no mapa de `ParametrosDeConsulta` (a mensagem lista as chaves do mapa, então se atualiza sozinha); `ORDER BY` com `COALESCE(concluida_em, cancelada_em)` `NULLS LAST` | — | Reaproveita `ENCERRADA_EM_EXPR`, que o filtro por conclusão já usa |
| Distribuição em aberto (RF-29–30) | Campos aditivos `emAbertoPorTipo` e `emAbertoPorPrioridade` (grupo `SEM_PRIORIDADE`) em `GET /api/solicitacoes/metricas`, agregados no banco; restrição de perfil que a rota já tem | Painel por período | `solicitacoesPorStatus` já mora ali e o dashboard compara as três distribuições lado a lado |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Reuso por delegação | `EncerrarSolicitacaoUseCase` → `CancelarSolicitacaoUseCase` | Dois caminhos gravando trilhas diferentes | Construtor ganha uma dependência |
| Value Object | `AssinaturaDeArquivo` (domínio) | Regra de "bytes que um tipo precisa ter" fora do caso de uso | Uma classe pequena |

**Considerados e recusados**

- *Strategy por tipo de arquivo*: seis assinaturas cabem numa tabela; hierarquia seria antecipação.
- *Specification para os filtros da listagem*: o `findByFilters` nativo já concentra a regra e
  a spec 011 o manteve assim; trocar por Specification é refatoração sem requisito.
- *Record `FiltrosDeSolicitacao`*: reduziria os 18 parâmetros, mas mexe em todos os testes do
  repositório sem requisito que peça. Fica para quando a listagem ganhar o próximo filtro.

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| adapter.config | `adapter/config/AdminUserInitializer.java` | alterar (RF-01–03) | `adapter/config/AdminUserInitializerTest.java` |
| uc | `uc/auth/LoginUseCase.java` | alterar (RF-05) | `uc/auth/LoginUseCaseTest.java` |
| domain/validation | `core/domain/validation/AssinaturaDeArquivo.java` | criar (RF-08) | `core/domain/validation/AssinaturaDeArquivoTest.java` |
| uc | `uc/evidencia/AnexarEvidenciaUseCase.java` | alterar (RF-07–08) | `uc/evidencia/AnexarEvidenciaUseCaseTest.java` |
| adapter.out.storage | `adapter/out/storage/MinioStorageService.java` | alterar (RF-09) | `adapter/out/storage/MinioStorageServiceTest.java` |
| ports | `core/domain/ports/repositories/ModeloRepository.java` | alterar: `maiorVersao`, `existeOutroComIdentificacao`, `findAllByIdIn` | — (interface) |
| per | `per/ModeloRepositoryAdapter.java` e `per/repository/ModeloJpaRepository.java` | alterar | `per/ModeloRepositoryAdapterTest.java`, `per/ModeloRepositoryAdapterPostgresTest.java` (índice único) |
| uc | `uc/modelo/GerenciarModelosUseCase.java` | alterar (RF-10–14) | `uc/modelo/GerenciarModelosUseCaseTest.java` |
| domain/exceptions | `core/domain/exceptions/ConflitoException.java` | criar | `core/domain/exceptions/ConflitoExceptionTest.java` |
| adapter.config | `adapter/config/GlobalExceptionHandler.java` | alterar: 409 de `ConflitoException` | `adapter/config/GlobalExceptionHandlerTest.java` |
| resources | `resources/db/migration/V11__identificacao_unica_do_modelo_e_busca.sql` | criar: saneamento, índice único, função `rgm_sem_acento` | `per/FlywayMigrationTest.java` (estende) |
| uc | `uc/solicitacao/TriarSolicitacaoUseCase.java` | alterar (RF-15) | `uc/solicitacao/TriarSolicitacaoUseCaseTest.java` |
| domain/model | `core/domain/model/aggregates/Solicitacao.java` | alterar: `registrarAlteracaoDeResponsaveis` | `core/domain/model/SolicitacaoTest.java` |
| uc | `uc/solicitacao/GerenciarResponsaveisUseCase.java` | alterar (RF-15–17) | `uc/solicitacao/GerenciarResponsaveisUseCaseTest.java` |
| uc | `uc/solicitacao/EncerrarSolicitacaoUseCase.java` | alterar (RF-18) | `uc/solicitacao/EncerrarSolicitacaoUseCaseTest.java` |
| uc | `uc/solicitacao/EditarSolicitacaoUseCase.java` | alterar (RF-19–20) | `uc/solicitacao/EditarSolicitacaoUseCaseTest.java` |
| domain/validation | `core/domain/validation/AcoesPermitidasSolicitacao.java` | alterar (RF-21) | `core/domain/validation/AcoesPermitidasSolicitacaoTest.java` |
| adapter.config | `adapter/config/UseCaseConfig.java` | alterar: fios novos | coberto por `ApiApplicationTests` |
| uc | `uc/solicitacao/ObterSolicitacaoUseCase.java` | alterar: resumo do modelo no detalhe e em lote (RF-22) | `uc/solicitacao/ObterSolicitacaoUseCaseTest.java` |
| web | `web/dto/response/SolicitacaoResponse.java` | alterar: `modeloVersao` e sobreposição | `web/dto/response/SolicitacaoResponseTest.java` |
| web | `web/solicitacao/SolicitacaoController.java` | alterar: listagem, detalhe, `busca`, `sort`, responsáveis disponíveis | `web/solicitacao/SolicitacaoControllerTest.java` |
| uc | `uc/solicitacao/ListarResponsaveisDisponiveisUseCase.java` | criar (RF-23–24) | `uc/solicitacao/ListarResponsaveisDisponiveisUseCaseTest.java` |
| ports | `core/domain/ports/repositories/UsuarioRepository.java` | alterar: `buscarAtribuiveis` | — (interface) |
| per | `per/UsuarioRepositoryAdapter.java` e `per/repository/UsuarioJpaRepository.java` | alterar | `per/UsuarioRepositoryAdapterTest.java` |
| web | `web/dto/response/ResponsavelDisponivelResponse.java` | criar | `web/dto/response/ResponsavelDisponivelResponseTest.java` |
| web | `web/ParametrosDeConsulta.java` | alterar: `busca`, `concluidaEm` | `web/ParametrosDeConsultaTest.java` |
| domain/model | `core/domain/model/enums/CriterioOrdenacaoSolicitacao.java` | alterar: `ENCERRADA_EM` | `uc/solicitacao/ListarSolicitacoesUseCaseTest.java` |
| uc | `uc/solicitacao/ListarSolicitacoesUseCase.java` | alterar: `busca` | `uc/solicitacao/ListarSolicitacoesUseCaseTest.java` |
| ports | `core/domain/ports/repositories/SolicitacaoRepository.java` | alterar: `busca`, `countEmAbertoPorTipo`, `countEmAbertoPorPrioridade` | — (interface) |
| per | `per/SolicitacaoRepositoryAdapter.java` e `per/repository/SolicitacaoJpaRepository.java` | alterar | `per/SolicitacaoRepositoryAdapterTest.java`, `per/repository/SolicitacaoJpaRepositoryTest.java` |
| uc | `uc/solicitacao/ObterMetricasSolicitacoesUseCase.java` | alterar (RF-29) | `uc/solicitacao/ObterMetricasSolicitacoesUseCaseTest.java` |
| web | `web/dto/response/MetricasSolicitacaoResponse.java` | alterar: campos aditivos | `web/dto/response/MetricasSolicitacaoResponseTest.java` |
| docs | `.env.example`, `docs/configuracao.md`, `README.md`, `docs/modelo_dados.md`, `openspec/README.md` | alterar (RF-04, Princípio 12) | — |

## Contrato entre camadas

- **Entrada web → caso de uso:** `ParametrosDeConsulta` valida `busca`, `sort` e `size` e
  responde 400 em português antes de chegar ao caso de uso, como já faz com `sort`/`dir`.
- **Caso de uso → repositório:** portas em `core/domain/ports`; nenhum import de Spring em
  `core` novo (a regra de A Fazer, o limite de etapa do operador e a conferência de assinatura
  ficam em `core`; a tradução da violação do índice único em `ConflitoException` fica no adapter).
- **Erros:** `ConflitoException` → 409; `ValidationException` → 400; `NaoAutorizadoException` →
  403; `BusinessRuleException` → 422, como hoje. O 409 por bloqueio otimista continua no
  handler existente.
- **Compatibilidade:** campos novos (`modeloVersao`, `emAbertoPorTipo`, `emAbertoPorPrioridade`)
  e um parâmetro novo (`q` ou `busca`, nome público `q`); nenhum removido.

## Dependências externas

Nenhuma nova. Postgres: função SQL própria, sem extensão.

| Dependência | Versão | Justificativa | Simulada nos testes por |
|---|---|---|---|
| MinIO | atual | Upload com nome gerado | Mock de `StorageService` nos testes de caso de uso; `MinioClient` mockado no teste do serviço |
| PostgreSQL | atual | Função e índice | Testcontainers já usado em `*PostgresTest` |

## Impacto no contrato de operação

- **Variáveis novas:** `ADMIN_EMAIL` (opcional, padrão `admin@rgm.com`) e `ADMIN_SENHA`
  (obrigatória em `prod` para o admin nascer). Entram no `.env.example` e em
  `docs/configuracao.md`, com o passo de troca de senha.
- **Migração:** `V11` (sequencial; `V1`–`V10` intactas, Princípio 9). Em produção, rodar a
  consulta de duplicatas antes do deploy e conferir o resultado depois (passo no PR).
- **Makefile e compose:** nenhum alvo ou serviço novo.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Frontend dependia do nome do arquivo na URL da evidência | baixa | O download usa `nomeArquivo` da evidência; conferir a tela de evidências no e2e e documentar no PR |
| Operador perde a edição depois da triagem e o frontend ainda mostra o botão | média | `acoesPermitidas` já governa o botão (`EDITAR`); v1.5.0 do front esconde pelo que a API devolve |
| Frontend usa "alterar responsáveis" em A Fazer | baixa | Conferir no e2e do front (a triagem é o caminho de A Fazer); se aparecer, o plano volta para a spec |
| Migração de duplicatas em produção altera versão | baixa | Só duplicatas mudam; teste de migração com dados repetidos (RNF-08); consulta prévia no PR |
| Tempo de login difere entre conta inexistente e inativa (canal de tempo) | baixa | Fora do critério mensurável (RNF-04 compara respostas); o limite de tentativas continua valendo |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Contrato de operação | Tudo passa por `make`; nenhum alvo novo. `make validate` do repositório (`lint` + `mvnw clean verify`, que não roda `fmt` nem `it`; rodo `fmt` e `it` à parte) é o portão |
| Arquitetura limpa | Regras novas em `core` sem import de Spring; transação, SQL e MinIO nos adapters; interfaces em `core/domain/ports` |
| Testes provam a entrega | Cada arquivo de produção tem teste espelhado; integração com Postgres real para migração, índice único e busca; um teste nomeado por cenário da spec, com a rastreabilidade em `tasks.md` (o projeto não tem BDD nem `make bdd`, desvio herdado da feature 001 e já aceito na 011); cobertura ≥ 95% por arquivo tocado |
| Simplicidade defensável | Padrões só onde há problema presente; três padrões recusados com motivo; nenhuma biblioteca nova |
| Autoria | Commits sem trailer de autoria de IA (o repositório bloqueia) |
| Idioma | Artefatos em português; identificadores em inglês técnico e domínio em português (`Solicitacao`, `ResponsavelDisponivel`) |
| P7 — mapa de camadas | Nenhum pacote novo fora da nomenclatura do legado |
| P8 — linguagem ubíqua | Nomes novos em português: `ConflitoException`, `ResponsavelDisponivelResponse`, `emAbertoPorTipo` |
| P9 — compatibilidade | Só campos e parâmetro aditivos; sem tipo novo de atividade nem de evento SSE; migração nova `V11`, `V1`–`V10` intactas |
