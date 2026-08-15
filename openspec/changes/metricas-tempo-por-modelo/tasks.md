## 1. Persistencia

- [x] 1.1 Adicionar `findMetricasPorModelo(SortField sort, SortDirection dir, int page, int size)` a `SolicitacaoRepository` (porta de dominio)
- [x] 1.2 Implementar a query nativa com `LAG()` window function no adapter JPA (`SolicitacaoRepositoryAdapter`/`SolicitacaoJpaRepository`), agrupando por modelo e calculando tempo medio de resolucao e intervalo medio entre solicitacoes
- [x] 1.3 Criada migration `V6__indices_metricas_modelo.sql` com indices em `(modelo_id, status)`/`(modelo_id, criada_em)`. **Limitacao conhecida**: nao foi possivel rodar `EXPLAIN ANALYZE` neste sandbox (sem daemon Docker disponivel para subir um Postgres descartavel) — a sintaxe SQL segue o padrao ja usado em `getTempoMedioResolucaoSegundos()`/`findByFilters()`, mas precisa de verificacao manual em staging antes de considerar os indices definitivamente suficientes em volume real.

## 2. Caso de uso

- [x] 2.1 Criar `ObterMetricasPorModeloUseCase` em `core/application/usecases/solicitacao`, com `Input(sort, dir, page, size)` e `Output` paginado (lista de `{modeloId, codigo, tempoMedioResolucaoSegundos, intervaloMedioEntreSolicitacoesSegundos}`, total, pagina)
- [x] 2.2 Testes unitarios do caso de uso (mock do repository), cobrindo: ordenacao pelas duas metricas, paginacao, modelo com 1 solicitacao concluida (intervalo nulo)

## 3. API

- [x] 3.1 Adicionar `GET /api/solicitacoes/metricas/por-modelo` a `SolicitacaoController`, com `sort`/`dir`/`page`/`size` validados contra whitelist (nunca interpolar nome de coluna a partir do input)
- [x] 3.2 Criar `MetricaModeloResponse` (DTO de resposta paginada)
- [x] 3.3 Testes de integracao do endpoint (`@WebMvcTest` ou equivalente), incluindo parametro de ordenacao invalido (400, nao 500)
- [x] 3.4 Registrar o novo bean do caso de uso em `UseCaseConfig.java`

## 4. Relatorio PDF do ranking

- [x] 4.1 Adicionar metodo de geracao do PDF de ranking em `ModeloPdfService` (ou service dedicado), reaproveitando o estilo visual existente
- [x] 4.2 Adicionar endpoint de export (`GET /api/solicitacoes/metricas/por-modelo/pdf` ou similar) com os mesmos parametros de ordenacao
- [x] 4.3 Testes do service de PDF (gera bytes nao vazios, cabecalho correto)

## 5. Ficha do modelo com metricas

- [x] 5.1 Estender `ModeloPdfService.gerarFicha` para aceitar as duas metricas como parametros opcionais (nulos quando o modelo nao tem dados suficientes)
- [x] 5.2 Atualizar `ModeloController` (endpoint de ficha) para calcular e passar as metricas daquele modelo especifico ao service
- [x] 5.3 Testes cobrindo modelo com 2+ solicitacoes concluidas (metricas aparecem) e com menos de 2 (omitidas sem erro)

## 6. Qualidade e documentacao

- [x] 6.1 `make lint` (spotless + checkstyle) e `./mvnw clean verify` com cobertura JaCoCo >= 95% nos arquivos tocados
- [x] 6.2 Atualizar `docs/casos-de-uso.md` com o novo caso de uso
- [ ] 6.3 Rodar `/opsx:archive` ao final, sincronizando `openspec/specs/metricas-dashboard/spec.md` e `openspec/specs/modelos/spec.md` com as mudancas deste change
