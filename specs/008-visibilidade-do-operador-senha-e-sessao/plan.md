# Plano de implementação — Visibilidade do operador, senha e sessão seguras e limites de texto

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.

Caminhos relativos a `app/src/main/java/com/rgm/api/` (produção) e
`app/src/test/java/com/rgm/api/` (teste), conforme o Princípio 7.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Regra de acesso à solicitação | Uma regra de domínio, `AcessoSolicitacao`, usada pelo detalhe, pelo histórico e pelo tempo real; `AcessoEvidenciaSolicitacao` passa a delegar a ela a parte "abriu ou é responsável" | Repetir a condição em cada caso de uso | A regra é a mesma nos quatro lugares (RF-01); hoje ela só existe dentro da regra de evidências |
| Visibilidade na listagem | Parâmetro novo `visivelParaUsuarioId` na consulta de filtros: nulo não restringe; preenchido exige "abriu ou é responsável ativo". O caso de uso preenche com o próprio operador e deixa `abertaPorUsuarioId` e `responsavelId` passarem como vieram | Manter o `responsavelId` forçado e somar uma segunda consulta; filtrar em memória depois de paginar | Uma condição a mais na consulta existente: total e paginação saem certos do banco (RF-02), os filtros enviados só restringem (RF-03) e não há consulta por item (RNF-01) |
| Operador sem nenhum filtro | Sempre usa a consulta de filtros | Tratar no `findAll` | O caminho sem filtros não tem onde aplicar a visibilidade |
| Resposta para solicitação alheia | `NaoAutorizadoException`, que o tratador global já converte em 403 | Exceção nova | Decisão 2 da spec; a exceção e o mapeamento já existem |
| Relatório em PDF | Nenhuma mudança própria: usa o mesmo caso de uso da listagem | Regra separada no controller | RF-06 sai de graça e não pode divergir da listagem |
| Destinatários do tempo real | Cada conexão guarda o usuário dono. A cada evento, um caso de uso novo recebe a solicitação e os usuários conectados e devolve quem pode receber, lendo o perfil atual deles | Guardar o perfil na conexão; decidir no controller | O perfil pode mudar com a conexão aberta (RF-13, RNF-06); a regra fica no núcleo, testável sem SSE. Custo: até 3 leituras por evento, 0 quando não há ninguém conectado |
| Evento na perda de acesso (RF-20) | `GerenciarResponsaveisUseCase` passa a devolver também quem foi removido; o controller entrega esses usuários ao publicador como "tinham acesso antes" | O controller ler os responsáveis antes de chamar o caso de uso | É o único ponto do sistema que remove responsável, e ele já sabe quem saiu; evita uma leitura extra |
| Como invalidar credencial antiga | Número de versão da credencial no usuário, copiado para dentro do acesso e da renovação; a troca ou redefinição de senha soma 1. Credencial com versão diferente da atual é recusada. Credencial sem versão (emitida antes desta entrega) vale como versão 0 | Instante "credenciais alteradas em" comparado com a data de emissão, como a #98 sugere | A data de emissão do token tem precisão de segundo: a credencial nova devolvida pela própria troca (RF-19) nasceria no mesmo segundo da troca e seria recusada, ou a antiga do mesmo segundo seria aceita. O número não depende de relógio |
| Onde a credencial é validada | Um caso de uso novo, `AutenticarAcessoUseCase`: lê o token pela porta, busca o usuário, confere ativo e versão e devolve o usuário. O filtro de autenticação e a abertura do tempo real passam a chamá-lo | Repetir a checagem de versão no filtro e no controller de SSE | Hoje os dois leem o token cada um do seu jeito; a regra nova entraria em três lugares. Mantém 1 leitura do usuário por chamada (RNF-02) |
| Perfil de cada chamada | O filtro monta a autoridade com o perfil do usuário devolvido por `AutenticarAcessoUseCase` | Continuar lendo o perfil do token | RF-13 e RNF-06; o usuário já é lido a cada chamada desde a feature 006 |
| Mudança de perfil | Não mexe na versão da credencial | Somar 1 também na mudança de perfil | Decisão 5 da spec (RF-22) |
| Sessão de quem troca a senha | `AlterarSenhaPropriaUseCase` emite acesso e renovação novos depois de salvar; a resposta ganha `token` e `refreshToken` ao lado dos campos que já devolve | Endpoint separado para buscar credencial nova | RF-19 numa chamada só; resposta só ganha campos (RNF-03) |
| Conexões abertas na troca de senha (RF-21) | O publicador ganha "encerrar conexões do usuário"; os dois controllers que trocam senha o chamam depois do caso de uso | Porta nova no núcleo para o caso de uso encerrar | As conexões vivem na memória do adaptador web; uma porta só para isso seria abstração sem segundo uso |
| Mínimo da senha | `PoliticaSenha` no domínio, com o mínimo e a validação; chamada em criar usuário, trocar e redefinir. Senha ausente em perfil que faz login cai na mesma validação | Anotação de tamanho nos três requests | RF-09 depende do perfil e não cabe em anotação; uma validação só atende RF-08 e RF-09 e fica num lugar (RNF-05) |
| Limites de texto | Constantes em `LimitesTexto`, no domínio, referenciadas pelas anotações de tamanho dos requests | Validar no construtor dos agregados | O construtor também reconstrói o que vem do banco; validar ali quebraria a leitura dos textos antigos acima do limite, que a spec manda preservar |
| Mensagem do limite | Texto único "deve ter no máximo {max} caracteres"; o tratador já prefixa o nome do campo | Mensagem escrita campo a campo | RF-14 com uma fonte só |
| Texto longo que escape dos requests | O tratador de violação de integridade responde 400 "texto acima do limite" quando o banco acusa valor longo demais, e mantém 409 nos outros casos | Deixar como está | RF-17 vale para qualquer campo, inclusive os que não passam por request com limite; RF-16 fica intacto |
| E-mail | Anotação de formato e de tamanho nos requests de criar e editar usuário | Validar no agregado | RF-10; usuário externo não tem e-mail e a anotação aceita ausente |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Nenhum novo | — | — | — |

Considerados e recusados:

- **Observer** com evento de domínio "senha alterada" para encerrar as conexões de tempo
  real: há um único interessado e dois pontos de disparo; a chamada direta no controller é
  mais curta e mais fácil de seguir.
- **Specification** para compor a visibilidade com os filtros da listagem: a consulta de
  filtros já existe e ganha uma condição.
- **Strategy** por perfil para a regra de acesso: são dois ramos (vê todas; abriu ou é
  responsável) e cabem num método.

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| core/domain | `core/domain/validation/AcessoSolicitacao.java` | criar: `podeVer` e `validarLeitura` (RF-01) | `core/domain/validation/AcessoSolicitacaoTest.java` (criar) |
| core/domain | `core/domain/validation/AcessoEvidenciaSolicitacao.java` | alterar: delega "abriu ou é responsável" a `AcessoSolicitacao` | `core/domain/validation/AcessoEvidenciaSolicitacaoTest.java` (sem mudança de comportamento; roda como está) |
| core/domain | `core/domain/validation/PoliticaSenha.java` | criar: mínimo de 8 e validação (RF-08, RF-09) | `core/domain/validation/PoliticaSenhaTest.java` (criar) |
| core/domain | `core/domain/validation/LimitesTexto.java` | criar: constantes da tabela de limites e a mensagem (RNF-05) | — (só constantes) |
| core/domain | `core/domain/model/aggregates/Usuario.java` | alterar: `versaoCredencial`; `withSenha` soma 1; construtor antigo continua, com versão 0 | `core/domain/model/UsuarioTest.java` (alterar) |
| interfaces | `core/domain/ports/services/CredencialToken.java` | criar: usuário e versão lidos de um token | — (registro sem lógica) |
| interfaces | `core/domain/ports/services/AccessTokenIssuer.java` | alterar: `validateAccessToken`; `validateRefreshToken` devolve `CredencialToken` | — |
| interfaces | `core/domain/ports/repositories/SolicitacaoRepository.java` | alterar: `visivelParaUsuarioId` em `findByFilters` | — |
| core/application | `core/application/usecases/solicitacao/ListarSolicitacoesUseCase.java` | alterar: operador lista por visibilidade, sem forçar `responsavelId` (RF-02, RF-03) | `.../solicitacao/ListarSolicitacoesUseCaseTest.java` (alterar) |
| core/application | `core/application/usecases/solicitacao/ObterSolicitacaoUseCase.java` | alterar: valida leitura (RF-04) | `.../solicitacao/ObterSolicitacaoUseCaseTest.java` (alterar) |
| core/application | `core/application/usecases/solicitacao/ListarAtividadesUseCase.java` | alterar: recebe o usuário e valida leitura (RF-05) | `.../solicitacao/ListarAtividadesUseCaseTest.java` (alterar) |
| core/application | `core/application/usecases/solicitacao/ResolverDestinatariosEventoUseCase.java` | criar: quem, entre os conectados, recebe o evento de uma solicitação (RF-18, RF-20) | `.../solicitacao/ResolverDestinatariosEventoUseCaseTest.java` (criar) |
| core/application | `core/application/usecases/solicitacao/GerenciarResponsaveisUseCase.java` | alterar: devolve também os responsáveis removidos (RF-20) | `.../solicitacao/GerenciarResponsaveisUseCaseTest.java` (alterar) |
| core/application | `core/application/usecases/auth/AutenticarAcessoUseCase.java` | criar: token de acesso → usuário ativo com versão atual (RF-11, RF-13) | `.../auth/AutenticarAcessoUseCaseTest.java` (criar) |
| core/application | `core/application/usecases/auth/RefreshTokenUseCase.java` | alterar: recusa versão antiga (RF-11) | `.../auth/RefreshTokenUseCaseTest.java` (alterar) |
| core/application | `core/application/usecases/auth/AlterarSenhaPropriaUseCase.java` | alterar: `PoliticaSenha`; devolve credenciais novas (RF-08, RF-19) | `.../auth/AlterarSenhaPropriaUseCaseTest.java` (alterar) |
| core/application | `core/application/usecases/admin/GerenciarUsuariosUseCase.java` | alterar: `PoliticaSenha` em criar e redefinir (RF-08, RF-09) | `.../admin/GerenciarUsuariosUseCaseTest.java` (alterar) |
| adapters/repositories | `adapter/out/persistence/repository/SolicitacaoJpaRepository.java` | alterar: condição de visibilidade na consulta de filtros e na contagem | `.../repository/SolicitacaoJpaRepositoryPostgresTest.java` (alterar) |
| adapters/repositories | `adapter/out/persistence/SolicitacaoRepositoryAdapter.java` | alterar: repassa o parâmetro | `adapter/out/persistence/SolicitacaoRepositoryAdapterTest.java` (alterar) |
| adapters/repositories | `adapter/out/persistence/entity/UsuarioJpaEntity.java` | alterar: coluna `versaoCredencial` | coberto pelos testes do mapper |
| adapters/repositories | `adapter/out/persistence/mapper/UsuarioMapper.java` | alterar | `adapter/out/persistence/mapper/MapperTest.java`, `MapperRoundtripTest.java` (alterar) |
| adapters/repositories | `app/src/main/resources/db/migration/V10__versao_credencial_usuario.sql` | criar: `usuarios.versao_credencial INTEGER NOT NULL DEFAULT 0` | `adapter/out/persistence/FlywayMigrationTest.java` (roda como está) |
| infra/tools | `adapter/out/security/JwtAccessTokenIssuer.java` | alterar: grava a versão nos dois tokens; valida o de acesso | `adapter/out/security/JwtAccessTokenIssuerTest.java` (alterar) |
| infra/tools | `adapter/out/security/JwtAuthenticationFilter.java` | alterar: usa `AutenticarAcessoUseCase` e o perfil atual; deixa de ler o token por conta própria | `adapter/out/security/JwtAuthenticationFilterTest.java` (alterar) |
| adapters/controllers | `adapter/in/web/solicitacao/SolicitacaoEventPublisher.java` | alterar: conexão com dono; publicação por solicitação; encerrar conexões de um usuário (RF-18, RF-20, RF-21) | `adapter/in/web/solicitacao/SolicitacaoEventPublisherTest.java` (alterar) |
| adapters/controllers | `adapter/in/web/solicitacao/SolicitacaoSseController.java` | alterar: autentica por `AutenticarAcessoUseCase` e registra a conexão com o usuário | `adapter/in/web/solicitacao/SolicitacaoSseControllerTest.java` (alterar) |
| adapters/controllers | `adapter/in/web/solicitacao/SolicitacaoController.java` | alterar: histórico recebe o usuário; publicações informam a solicitação e, na troca de responsáveis, quem saiu | `adapter/in/web/solicitacao/SolicitacaoControllerTest.java` (alterar) |
| adapters/controllers | `adapter/in/web/evidencia/EvidenciaController.java` | alterar: publicação informa a solicitação | `adapter/in/web/evidencia/EvidenciaControllerTest.java` (alterar) |
| adapters/controllers | `adapter/in/web/usuario/UsuarioController.java` | alterar: resposta da troca com credenciais; encerra as conexões do usuário | `adapter/in/web/usuario/UsuarioControllerTest.java` (alterar) |
| adapters/controllers | `adapter/in/web/admin/AdminController.java` | alterar: encerra as conexões do usuário na redefinição | `adapter/in/web/admin/AdminControllerTest.java` (alterar) |
| adapters/controllers | `adapter/in/web/dto/response/SenhaAlteradaResponse.java` | criar: campos de `UsuarioResponse` mais `token` e `refreshToken` | coberto pelo teste do controller |
| adapters/controllers | `adapter/in/web/dto/request/` — `AbrirSolicitacaoRequest`, `EditarSolicitacaoRequest`, `ComentarioRequest`, `CancelarSolicitacaoRequest`, `DevolverSolicitacaoRequest`, `EncerrarSolicitacaoRequest`, `CriarModeloRequest`, `EditarModeloRequest`, `CriarMaquinaRequest`, `EditarMaquinaRequest`, `CriarUsuarioRequest`, `EditarUsuarioRequest` | alterar: anotação de tamanho com `LimitesTexto`; formato de e-mail nos dois de usuário (RF-10, RF-14, RF-15) | testes dos controllers correspondentes (alterar): um caso acima do limite por campo, um caso no limite para o título |
| infra/api | `adapter/config/GlobalExceptionHandler.java` | alterar: valor longo demais vindo do banco responde 400 (RF-17) | `adapter/config/GlobalExceptionHandlerTest.java` (alterar) |
| infra/init | `adapter/config/UseCaseConfig.java` | alterar: registra os dois casos de uso novos; `AlterarSenhaPropriaUseCase` recebe o emissor | — |
| operação | `app/pom.xml` | alterar: tira `SolicitacaoSseController` da lista de exclusão do JaCoCo | — |
| documentação | `docs/casos-de-uso.md`, `openspec/README.md` | alterar | — |

Rastreabilidade: RF-01 em `AcessoSolicitacao`; RF-02, RF-03 e RF-06 em
`ListarSolicitacoesUseCase` e `SolicitacaoJpaRepository`; RF-04 em `ObterSolicitacaoUseCase`;
RF-05 em `ListarAtividadesUseCase`; RF-07 nos mesmos três, pelo ramo de gestor e
administrador; RF-08 e RF-09 em `PoliticaSenha` e nos dois casos de uso de senha; RF-10 nos
requests de usuário; RF-11 e RF-12 em `Usuario`, `JwtAccessTokenIssuer`,
`AutenticarAcessoUseCase` e `RefreshTokenUseCase`; RF-13 em `JwtAuthenticationFilter`;
RF-14 e RF-15 em `LimitesTexto` e nos requests; RF-16 e RF-17 em `GlobalExceptionHandler`;
RF-18 e RF-20 em `ResolverDestinatariosEventoUseCase`, `SolicitacaoEventPublisher` e
`GerenciarResponsaveisUseCase`; RF-19 em `AlterarSenhaPropriaUseCase` e `UsuarioController`;
RF-21 em `SolicitacaoEventPublisher`, `UsuarioController` e `AdminController`; RF-22 em
`Usuario.alterarPerfil`, que não toca a versão.

## Contrato entre camadas

- `GET /api/solicitacoes` e `GET /api/solicitacoes/relatorio`: mesmos parâmetros. Para
  operador, o resultado é o conjunto "abriu ou é responsável ativo", restringido pelos
  filtros enviados.
- `GET /api/solicitacoes/{id}` e `GET /api/solicitacoes/{id}/atividades`: 403 para operador
  sem acesso; 404 continua valendo para identificador inexistente.
- `GET /api/solicitacoes/events?token=`: 401 para token inválido, de usuário inativo ou de
  versão antiga. A conexão fica associada ao usuário do token.
- Eventos `solicitacao` e `solicitacao_atividade`: mesmo nome e mesmo corpo. Muda só quem
  recebe: gestor e administrador, todos; operador, os das solicitações a que tem acesso,
  mais o evento da mudança que o removeu de responsável.
- `PATCH /api/usuarios/me/senha` responde os campos de hoje mais `token` e `refreshToken`.
  As conexões de tempo real do usuário são encerradas; a tela reconecta com o token novo.
- `PATCH /api/admin/usuarios/{id}/senha`: mesma resposta; encerra as conexões de tempo real
  do usuário alvo.
- `POST /api/auth/refresh`: 401 para renovação de versão antiga.
- Senha curta ou ausente (perfil que faz login) e texto acima do limite respondem 400, no
  formato de erro que a API já usa.
- Erros: os casos de uso lançam `NaoAutorizadoException` (403 pelo tratador global) e
  `ValidationException` (400). `AutenticarAcessoUseCase` lança `NaoAutorizadoException`; o
  filtro a trata deixando a chamada sem autenticação (401 pelo ponto de entrada de
  segurança) e o controller de SSE a converte em 401, como o `AuthController` já faz.

## Dependências externas

| Dependência | Versão | Justificativa | Simulada nos testes por |
|---|---|---|---|
| Nenhuma nova | — | Bean Validation, JJWT e Testcontainers já estão no projeto | — |

## Impacto no contrato de operação

Nenhum alvo novo no `Makefile`, nenhum serviço novo no compose, nenhuma variável de ambiente
nova. Uma migração nova (`V10`), aplicada pelo Flyway na subida.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Frontend atual contra este backend: quem troca a própria senha volta ao login, porque a tela ainda não guarda as credenciais novas | certa | Declarado na spec (fora de escopo); abrir issue no `rgm-frontend` para guardar `token` e `refreshToken` da resposta. Publicar o backend primeiro não quebra nada além disso |
| Credenciais emitidas antes da entrega não trazem versão | certa | Valem como versão 0, que é o valor inicial da coluna: ninguém é desconectado pela publicação; passam a ser recusadas na primeira troca de senha |
| Condição de visibilidade é SQL nativo de PostgreSQL e não roda no banco em memória | certa | Coberta em `SolicitacaoJpaRepositoryPostgresTest`, que roda em `make test-all` e na CI |
| Evento de tempo real passa a custar até 3 leituras | certa | Só quando há alguém conectado; volume da fábrica é baixo. Sem conexão, 0 leituras |
| Usuário inativo com conexão de tempo real aberta deixa de receber eventos | baixa | Consequência da regra de acesso, que exige usuário ativo, como a de evidências; coerente com a feature 006 |
| Trocar o construtor de `Usuario` quebrar os testes que o usam | média | O construtor atual é mantido e assume versão 0; só o mapper e o emissor usam o novo |
| Tirar o controller de SSE da exclusão do JaCoCo derrubar a cobertura | baixa | O controller perde a leitura de token e fica com dois caminhos, cobertos no teste que já existe |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Contrato de operação | Validação por `make validate`; nenhum alvo novo. Vale o desvio da feature 001: sem alvos `it` e `bdd` |
| Arquitetura limpa | Regras de acesso, de senha e de sessão no núcleo; o token só é lido atrás da porta `AccessTokenIssuer`; SQL só no repositório; nenhum import de framework novo em `core` |
| Testes provam a entrega | Teste antes da implementação; testes novos em AAA, um comportamento por teste, com `verifyNoMoreInteractions` |
| Simplicidade defensável | Uma condição a mais na consulta existente, um número de versão em vez de lista de tokens revogados, nenhum padrão novo |
| Autoria | Commits e PR só com o autor do `git config` |
| Idioma | Artefatos em português |
| Mapa de camadas (7) | Só pacotes existentes |
| Linguagem ubíqua (8) | `AcessoSolicitacao`, `PoliticaSenha`, `LimitesTexto`, `versaoCredencial`; testes novos em `shouldXWhenY` |
| Compatibilidade com produção (9) | Nenhum campo, endpoint ou tipo de evento removido ou renomeado (RNF-03); migração nova `V10`, sem tocar `V1` a `V9`; ordem de publicação: backend primeiro |
| Cobertura não regride (10) | Mínimo do JaCoCo intocado; a lista de exclusões diminui em uma classe |
| Padrão de teste no que for tocado (11) | Testes novos e alterados no padrão |
| Uma fonte de especificação (12) | Spec nesta pasta; linha nova na tabela de `openspec/README.md` |
