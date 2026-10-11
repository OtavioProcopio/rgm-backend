# Plano de implementação — Indicadores de gestão, relatórios em PDF fiéis à tela e limpeza

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.
>
> Código lido em `develop` (`ce37c52`). `MAIN` = `app/src/main/java/com/rgm/api`, `TEST` =
> `app/src/test/java/com/rgm/api`, `RES` = `app/src/main/resources`. Sistema interno: soluções
> simples, sem índice novo nem carga sintética.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Onde ficam os indicadores | Endpoint novo `GET /api/solicitacoes/metricas/gestao?dias=7\|30\|90`, caso de uso próprio, porta própria (`IndicadoresDeGestaoRepository`) | Inchar `SolicitacaoRepository` e o painel da 011 | `SolicitacaoRepository` já tem mais de 20 operações; o painel tem formato fechado e o frontend v1.5.0 o consome |
| Janela do período | A mesma do painel: os últimos N dias locais da fábrica, de hoje para trás, `[00:00 de hoje-(N-1), 00:00 de amanhã)` no fuso configurado | Janela móvel de 24 h × N | Os números do dashboard têm de bater com o painel |
| Cálculo | SQL agregado (`percentile_cont`, `FILTER`, `LEAD`) numa consulta nativa por bloco; o caso de uso só monta a resposta e nunca lista solicitações | Calcular em memória | RNF-02; mediana e p90 exatos |
| Prazo cumprido | Concluídas com `concluida_em` na janela e prioridade preenchida; no prazo se `concluida_em <= criada_em + horas da prioridade` (mesma expressão `PRAZO_LIMITE_EXPR` do filtro de atrasada) | Reimplementar a regra | Uma só definição de prazo no sistema |
| Tempo por etapa | Intervalos entre marcos de `atividades_solicitacao` (tipo de mudança de status) mais o marco de abertura; um intervalo conta se **terminou** na janela e foi encerrado pela transição certa (A Fazer→Em andamento, Em andamento→Em validação, Em validação→Concluída ou devolução); soma por solicitação e etapa, depois mediana e média | Usar só `criada_em`/`concluida_em` | Só as atividades têm o instante de cada transição; devolução soma sozinha |
| Tipos de solicitação nos indicadores | Entram todos nos de prazo, tempo de resolução, etapas e backlog; **só reparo** na reincidência; **criação fica fora** do bloco por máquina (não tem modelo até concluir) | Excluir criação de tudo | Criação tem prazo e etapas como qualquer pedido; a regra da 011 (tempo do modelo) vale para modelo e máquina |
| Valores sem dado | `null` (mediana, p90, taxa, tempo médio) e `amostras: 0`; nunca `0` | Zero | Spec RF-05, RF-09 |
| Reincidência | Limite 3 fixo, constante nomeada; lista por modelo com código, versão, máquina e quantidade | Parâmetro configurável | Decisão do usuário (fixo) |
| PDF acompanha a tela | `GET /api/solicitacoes/relatorio` passa a aceitar `q`, `sort` e `dir` e monta a consulta com o **mesmo** `buildInput` da listagem; o cabeçalho do PDF escreve os filtros por `DescricaoDosFiltros` (rótulo legível → valor), construído a partir dos mesmos parâmetros | PDF por lista de ids vinda da tela | O servidor é fonte única; nada para o cliente montar |
| Sem corte, com rede de segurança | Constante `LIMITE_DE_LINHAS_DO_RELATORIO = 5000`; consulta pede 5000, lê `totalElements`; se `total > 5000`, o PDF traz o aviso "exibindo 5000 de N" no cabeçalho | Paginar o PDF; recusar com 422 | Decisão do usuário (sem limite, 5000 só como proteção) |
| Fotos na ficha | Nova operação `baixar(url)` na porta de armazenamento; `ModeloPdfService` recebe o `StorageService`, baixa cada foto e embute (capa primeiro e em destaque); falha vira o marcador "foto indisponível" | Embutir por URL pública (HTTP interno) | A URL pública pode não ser alcançável de dentro do servidor; o cliente MinIO já está configurado |
| Montagem da ficha | Caso de uso novo `ObterFichaDoModeloUseCase` (substitui a montagem solta no controller), com `findBySolicitacaoIdIn` nas atividades | Manter no controller | Regra de aplicação fora do controller; número fixo de consultas (RNF-04) |
| Limpeza | Remover o que o levantamento achou sem chamador; sem migração (a tabela `evento_modelo_evidencias` permanece: nada a perder e `V1`–`V10` são imutáveis) | `DROP TABLE` | Apagar dado é irreversível e o benefício é zero |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Reuso por constante compartilhada | Expressões SQL de prazo e atraso em `SolicitacaoJpaRepository` | Duas definições de prazo | Interface JPA nova referencia as constantes |
| Value Object | `DescricaoDosFiltros` (lista imutável de rótulo e valor) | Cabeçalho do PDF montado em cada controller | Uma classe pequena |

**Considerados e recusados**

- *Specification* para os filtros: o `findByFilters` nativo concentra a regra desde a 011.
- *Strategy por tipo de relatório*: três relatórios com cabeçalho comum cabem num método
  utilitário do serviço de PDF.
- *Cache dos indicadores*: base pequena e uso esporádico.

## Arquivos a criar ou alterar

### Indicadores de gestão

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| ports | `MAIN/core/domain/ports/repositories/IndicadoresDeGestaoRepository.java` e records de linha (`LinhaPrazo`, `ResumoResolucao`, `FaixasDoBacklog`, `LinhaEtapa`, `LinhaDevolucao`, `LinhaMaquina`, `LinhaReincidente`, `LinhaCarga`) | criar | — (interfaces e records) |
| per | `MAIN/adapter/out/persistence/repository/IndicadoresDeGestaoJpaRepository.java` | criar | `TEST/adapter/out/persistence/repository/IndicadoresDeGestaoJpaRepositoryPostgresTest.java` |
| per | `MAIN/adapter/out/persistence/IndicadoresDeGestaoRepositoryAdapter.java` | criar | `TEST/adapter/out/persistence/IndicadoresDeGestaoRepositoryAdapterTest.java` |
| uc | `MAIN/core/application/usecases/solicitacao/ObterIndicadoresDeGestaoUseCase.java` | criar | `TEST/core/application/usecases/solicitacao/ObterIndicadoresDeGestaoUseCaseTest.java` |
| web | `MAIN/adapter/in/web/dto/response/IndicadoresDeGestaoResponse.java` | criar | `TEST/adapter/in/web/dto/response/IndicadoresDeGestaoResponseTest.java` |
| web | `MAIN/adapter/in/web/solicitacao/IndicadoresDeGestaoController.java` (`/api/solicitacoes/metricas/gestao`, restrição já coberta pela regra de `/metricas/**`) | criar | `TEST/adapter/in/web/solicitacao/IndicadoresDeGestaoControllerTest.java` |
| web | `MAIN/adapter/in/web/ParametrosDeConsulta.java` (`periodoDeGestao` reaproveita `PERIODOS_DO_PAINEL`) | alterar | `TEST/adapter/in/web/ParametrosDeConsultaTest.java` |
| config | `MAIN/adapter/config/UseCaseConfig.java` | alterar | `TEST/ApiApplicationTests.java` |

### PDFs

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| web | `MAIN/adapter/in/web/DescricaoDosFiltros.java` | criar | `TEST/adapter/in/web/DescricaoDosFiltrosTest.java` |
| web | `MAIN/adapter/in/web/solicitacao/SolicitacaoController.java` (relatório com `q`, `sort`, `dir`, filtros, aviso; ranking com filtros) | alterar | `TEST/adapter/in/web/solicitacao/SolicitacaoControllerTest.java` |
| web | `MAIN/adapter/in/web/modelo/ModeloController.java` (lista com filtros e aviso; ficha via caso de uso) | alterar | `TEST/adapter/in/web/modelo/ModeloControllerTest.java` |
| uc | `MAIN/core/application/usecases/modelo/ObterFichaDoModeloUseCase.java` | criar | `TEST/core/application/usecases/modelo/ObterFichaDoModeloUseCaseTest.java` |
| ports | `MAIN/core/domain/ports/repositories/AtividadeSolicitacaoRepository.java` (`findBySolicitacaoIdIn`) | alterar | — |
| per | `MAIN/adapter/out/persistence/AtividadeSolicitacaoRepositoryAdapter.java` e `.../repository/AtividadeSolicitacaoJpaRepository.java` | alterar | `TEST/adapter/out/persistence/AtividadeSolicitacaoRepositoryAdapterTest.java` |
| ports | `MAIN/core/domain/ports/services/StorageService.java` (`baixar`) | alterar | — |
| storage | `MAIN/adapter/out/storage/MinioStorageService.java` | alterar | `TEST/adapter/out/storage/MinioStorageServiceTest.java` |
| report | `MAIN/adapter/out/report/ModeloPdfService.java` (filtros, total, aviso, coluna tipo, fotos, atividades) | alterar | `TEST/adapter/out/report/ModeloPdfServiceTest.java` |
| report | `MAIN/adapter/out/report/SolicitacaoPdfService.java` (filtros, total, aviso) | alterar | `TEST/adapter/out/report/SolicitacaoPdfServiceTest.java` |
| report | `MAIN/adapter/out/report/PdfFooterEvent.java` | sem mudança | — |

### Limpeza (RF-23 a RF-27)

| Alvo | Ação | Motivo comprovado |
|---|---|---|
| `MAIN/core/domain/ports/repositories/EventoModeloEvidenciaRepository.java`, `MAIN/adapter/out/persistence/EventoModeloEvidenciaRepositoryAdapter.java`, `.../repository/EventoModeloEvidenciaJpaRepository.java`, `.../entity/EventoModeloEvidenciaJpaEntity.java`, `MAIN/core/domain/model/entities/EventoModeloEvidencia.java` e os testes `EventoModeloEvidenciaRepositoryAdapterTest`, `EventoModeloEvidenciaTest` | remover | Nenhum caso de uso chama a porta (nem `save`); a tabela fica |
| `findByStatus` (paginado e em lista) em `SolicitacaoRepository`, no adaptador e em `SolicitacaoJpaRepository`, e os testes dele | remover | Sem chamador em `main` |
| Sobrecargas "mantém compatibilidade" de `ModeloPdfService` e `SolicitacaoPdfService` (`gerarLista(modelos)`, `gerarFicha` de 4 argumentos, `gerar(solicitacoes)`) | remover | Só os testes as chamam |
| `app/Makefile` | remover | Duplica o `Makefile` da raiz (que o README e o CI usam) com cobertura de 90% desatualizada |
| `openspec/changes/archive/` | remover | Propostas antigas congeladas (commit `018c44d`) que nada referencia; a história está no git e em `specs/` |
| `.vscode/launch.json` | corrigir | Aponta para `com.rgm.backend.BackendApplication`, que não existe; passa a `com.rgm.api.ApiApplication` |
| `TEST/adapter/out/persistence/mapper/MapperTest.java` e `MapperRoundtripTest.java` | fundir em um só, mantendo os casos distintos | Mesmos mapeadores, mesmas viagens de ida e volta |
| `RateLimitFilterTest.evictExpiredEntries_...` | dar verificação (a cadeia é chamada 105 vezes e o limite continua valendo) | Teste sem nenhuma asserção |
| `TEST/core/domain/model/PerfilUsuarioTest.java`, `StatusSolicitacaoTest.java` | mover para `TEST/core/domain/model/enums/` | O código mora em `enums`; o espelho do pacote está errado |
| `docs/diagramas.md`, `docs/modelo_dados.md`, `docs/casos-de-uso.md`, `openspec/README.md` | atualizar | Referências ao que foi removido e aos recursos novos |

**Mantidos de propósito (conferidos):** `.devcontainer/` e `app/.env.example` formam um conjunto
próprio do ambiente em contêiner (host `db`, MinIO `root`); `*PostgresTest` ficam fora do
`validate` por desenho do `Makefile`.

## Contrato entre camadas

- **Entrada:** `ParametrosDeConsulta` valida período (400) e `q`; o PDF reusa `buildInput` da listagem.
- **Aplicação:** `ObterIndicadoresDeGestaoUseCase` só combina linhas agregadas da porta e o relógio;
  `ObterFichaDoModeloUseCase` devolve um registro com tudo que a ficha precisa (modelo, eventos,
  solicitações, atividades por solicitação, nomes, resumo, fotos).
- **Saída:** o serviço de PDF é o único que baixa foto; erro de download não propaga.
- **Compatibilidade:** só rota, campos e parâmetros novos; `GET .../relatorio` mantém os
  parâmetros antigos.

## Dependências externas

Nenhuma nova. OpenPDF já embute imagem; MinIO `getObject` do cliente existente. Testes de PDF
leem o texto com o próprio `PdfReader` do OpenPDF.

## Impacto no contrato de operação

Nenhuma variável nem alvo novo; um alvo a menos (`app/Makefile`). Sem migração.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Foto grande deixa o PDF pesado | baixa | Galeria de modelo é pequena; imagem escalada para a largura da página |
| Remoção de classe usada por reflexão | muito baixa | `ApiApplicationTests` sobe o contexto; `RotasExistentesTest` compara as rotas |
| Mediana por etapa diverge de cálculo manual | média | Teste com conjunto conhecido (2 h / 5 h / 1 h, e devolução) em Postgres real |
| `percentile_cont` com poucas amostras | baixa | Contagem de amostras vai na resposta |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Contrato de operação | Tudo via `make`; um alvo duplicado removido |
| Arquitetura limpa | Regras novas em `core` sem Spring; SQL e PDF nos adaptadores; montagem da ficha sai do controller para um caso de uso |
| Testes provam a entrega | Todo arquivo novo tem teste espelhado; SQL em `*PostgresTest`; PDF lido de volta e afirmado; cobertura ≥ 95% por arquivo; cenários rastreados em `tasks.md` |
| Simplicidade defensável | Sem cache, índice, configuração nem biblioteca nova; dois padrões leves e três recusados |
| Autoria | Commits sem trailer de autoria de IA |
| Idioma | Artefatos em português; identificadores técnicos em inglês e domínio em português |
| P7 — mapa de camadas | Nada novo fora da nomenclatura do legado |
| P8 — linguagem ubíqua | `IndicadoresDeGestao`, `Reincidente`, `CargaPorResponsavel` |
| P9 — compatibilidade | Aditivo; nenhuma migração; rotas existentes inalteradas (teste de rotas) |
